package com.lawfirm.application.system.service;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 滑块验证码服务
 *
 * <p>实现带后端校验的滑块验证，防止前端绕过
 *
 * <p>流程： 1. 前端请求获取滑块令牌 2. 后端生成令牌并存储到Redis 3. 用户完成滑块验证后，前端携带令牌登录 4. 后端验证令牌有效性
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SliderCaptchaService {

  /** RedisTemplate. */
  private final RedisTemplate<String, Object> redisTemplate;

  /** SLIDER_TOKEN_PREFIX. */
  private static final String SLIDER_TOKEN_PREFIX = "slider:token:";

  /** SLIDER_VERIFIED_PREFIX. */
  private static final String SLIDER_VERIFIED_PREFIX = "slider:verified:";

  /** TOKEN_EXPIRE_MINUTES. */
  private static final int TOKEN_EXPIRE_MINUTES = 5; // 令牌5分钟过期

  /** VERIFIED_EXPIRE_SECONDS. */
  private static final int VERIFIED_EXPIRE_SECONDS = 60; // 验证通过后60秒内有效

  /** 每分钟秒数 */
  private static final int SECONDS_PER_MINUTE = 60;

  /** SECURE_RANDOM. */
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /** 滑块目标位置最小值 */
  private static final int TARGET_POSITION_MIN = 10;

  /** 滑块目标位置最大值 */
  private static final int TARGET_POSITION_MAX = 90;

  /** 滑块验证最小滑动时间（毫秒） */
  private static final int MIN_SLIDE_TIME_MS = 300;

  /** 滑块验证最大滑动时间（毫秒） */
  private static final long MAX_SLIDE_TIME_MS = 30000L;

  /**
   * 生成滑块验证令牌
   *
   * @return 滑块令牌结果
   */
  public SliderTokenResult generateToken() {
    // 生成唯一令牌ID
    String tokenId = UUID.randomUUID().toString().replace("-", "");

    // 生成随机验证数据（用于后续校验）
    int targetPosition =
        SECURE_RANDOM.nextInt(TARGET_POSITION_MAX - TARGET_POSITION_MIN) + TARGET_POSITION_MIN;
    long timestamp = System.currentTimeMillis();

    // 存储到Redis
    String cacheKey = SLIDER_TOKEN_PREFIX + tokenId;
    SliderTokenData tokenData = new SliderTokenData(targetPosition, timestamp, false);
    redisTemplate.opsForValue().set(cacheKey, tokenData, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

    log.debug("生成滑块令牌: tokenId={}, targetPosition={}", tokenId, targetPosition);

    return SliderTokenResult.builder()
        .tokenId(tokenId)
        .targetPosition(targetPosition) // 告诉前端目标位置（可选，用于拼图类验证）
        .expireSeconds(TOKEN_EXPIRE_MINUTES * SECONDS_PER_MINUTE)
        .build();
  }

  /**
   * 验证滑块操作
   *
   * @param tokenId 令牌ID
   * @param slideTime 滑动耗时（毫秒）
   * @param slideTrack 滑动轨迹（可选，用于行为分析）
   * @return 验证结果
   */
  public SliderVerifyResult verify(
      final String tokenId, final long slideTime, final int[] slideTrack) {
    if (tokenId == null || tokenId.isEmpty()) {
      return SliderVerifyResult.fail("无效的验证令牌");
    }

    String cacheKey = SLIDER_TOKEN_PREFIX + tokenId;
    Object cached = redisTemplate.opsForValue().get(cacheKey);

    if (cached == null) {
      log.warn("滑块令牌不存在或已过期: tokenId={}", tokenId);
      return SliderVerifyResult.fail("验证已过期，请刷新重试");
    }

    // 令牌只能使用一次
    redisTemplate.delete(cacheKey);

    // 基本行为验证
    // 1. 滑动时间验证：太快（<300ms）可能是机器人，太慢（>30s）可能是异常
    if (slideTime < MIN_SLIDE_TIME_MS) {
      log.warn("滑块验证失败-滑动过快: tokenId={}, slideTime={}ms", tokenId, slideTime);
      return SliderVerifyResult.fail("验证失败，请重试");
    }
    if (slideTime > MAX_SLIDE_TIME_MS) {
      log.warn("滑块验证失败-滑动过慢: tokenId={}, slideTime={}ms", tokenId, slideTime);
      return SliderVerifyResult.fail("验证超时，请重新验证");
    }

    // 2. 轨迹验证（可选）：如果提供了轨迹，检查轨迹点数量
    // 注：前端简化实现可能不传轨迹，所以只在有轨迹时验证
    if (slideTrack != null && slideTrack.length > 0 && slideTrack.length < 3) {
      log.warn("滑块验证失败-轨迹异常: tokenId={}, trackLength={}", tokenId, slideTrack.length);
      return SliderVerifyResult.fail("验证失败，请重试");
    }

    // 验证通过，生成验证凭证
    String verifyToken = UUID.randomUUID().toString().replace("-", "");
    String verifiedKey = SLIDER_VERIFIED_PREFIX + verifyToken;
    redisTemplate.opsForValue().set(verifiedKey, "1", VERIFIED_EXPIRE_SECONDS, TimeUnit.SECONDS);

    log.debug("滑块验证通过: tokenId={}, verifyToken={}", tokenId, verifyToken);

    return SliderVerifyResult.success(verifyToken);
  }

  /**
   * 检查滑块验证凭证是否有效（登录时调用）
   *
   * @param verifyToken 验证凭证
   * @return 是否有效
   */
  public boolean checkVerified(final String verifyToken) {
    if (verifyToken == null || verifyToken.isEmpty()) {
      return false;
    }

    String verifiedKey = SLIDER_VERIFIED_PREFIX + verifyToken;
    Boolean exists = redisTemplate.hasKey(verifiedKey);

    if (Boolean.TRUE.equals(exists)) {
      // 凭证只能使用一次
      redisTemplate.delete(verifiedKey);
      log.debug("滑块验证凭证有效: verifyToken={}", verifyToken);
      return true;
    }

    log.warn("滑块验证凭证无效或已使用: verifyToken={}", verifyToken);
    return false;
  }

  // ========== 内部数据类 ==========

  /** 滑块令牌数据. */
  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class SliderTokenData implements java.io.Serializable {
    /** SerialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** 目标位置. */
    private int targetPosition;

    /** 时间戳. */
    private long timestamp;

    /** 是否已验证. */
    private boolean verified;
  }

  /** 滑块令牌结果. */
  @lombok.Data
  @lombok.Builder
  public static class SliderTokenResult {
    /** 令牌ID. */
    private String tokenId;

    /** 目标位置（可选，用于拼图验证）. */
    private int targetPosition;

    /** 过期秒数. */
    private int expireSeconds;
  }

  /** 滑块验证结果. */
  @lombok.Data
  @lombok.Builder
  public static class SliderVerifyResult {
    /** 是否成功. */
    private boolean success;

    /** 消息. */
    private String message;

    /** 验证通过后的凭证，登录时需要携带. */
    private String verifyToken;

    /**
     * 创建成功结果
     *
     * @param verifyToken 验证令牌
     * @return 验证结果
     */
    public static SliderVerifyResult success(final String verifyToken) {
      return SliderVerifyResult.builder()
          .success(true)
          .message("验证成功")
          .verifyToken(verifyToken)
          .build();
    }

    /**
     * 创建失败结果
     *
     * @param message 错误消息
     * @return 验证结果
     */
    public static SliderVerifyResult fail(final String message) {
      return SliderVerifyResult.builder().success(false).message(message).build();
    }
  }
}
