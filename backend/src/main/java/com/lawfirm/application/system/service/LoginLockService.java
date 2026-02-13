package com.lawfirm.application.system.service;

import com.lawfirm.infrastructure.notification.AlertService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 登录锁定服务
 *
 * <p>实现登录失败后的账户锁定机制，防止暴力破解
 *
 * <p>策略： - 连续失败 3 次：需要图形验证码 - 连续失败 5 次：锁定账户 15 分钟 - 连续失败 10 次：锁定账户 1 小时 - 连续失败 20 次：锁定账户 24 小时
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLockService {

  /** RedisTemplate. */
  private final RedisTemplate<String, Object> redisTemplate;

  /** Alert Service. */
  private final AlertService alertService;

  /** ConfigApp Service. */
  private final SysConfigAppService configAppService;

  // Redis Key 前缀
  /** FAIL_COUNT_PREFIX. */
  private static final String FAIL_COUNT_PREFIX = "login:fail:count:";

  /** LOCK_PREFIX. */
  private static final String LOCK_PREFIX = "login:lock:";

  /** 每分钟秒数 */
  private static final int SECONDS_PER_MINUTE = 60;

  // 失败次数阈值
  /** CAPTCHA_REQUIRED_THRESHOLD. */
  private static final int CAPTCHA_REQUIRED_THRESHOLD = 3; // 3次失败后需要验证码

  /** LOCK_THRESHOLD_1. */
  private static final int LOCK_THRESHOLD_1 = 5; // 5次失败锁定15分钟

  /** LOCK_THRESHOLD_2. */
  private static final int LOCK_THRESHOLD_2 = 10; // 10次失败锁定1小时

  /** LOCK_THRESHOLD_3. */
  private static final int LOCK_THRESHOLD_3 = 20; // 20次失败锁定24小时

  // 锁定时间（分钟）
  /** LOCK_DURATION_1. */
  private static final int LOCK_DURATION_1 = 15; // 15分钟

  /** LOCK_DURATION_2. */
  private static final int LOCK_DURATION_2 = 60; // 1小时

  /** LOCK_DURATION_3. */
  private static final int LOCK_DURATION_3 = 1440; // 24小时

  // 失败计数过期时间（小时）
  /** FAIL_COUNT_EXPIRE_HOURS. */
  private static final int FAIL_COUNT_EXPIRE_HOURS = 24;

  /**
   * 检查账户是否被锁定
   *
   * @param username 用户名
   * @return 锁定状态
   */
  public LockStatus checkLockStatus(final String username) {
    String lockKey = LOCK_PREFIX + username;
    Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);

    if (ttl != null && ttl > 0) {
      int remainingMinutes = (int) Math.ceil(ttl / SECONDS_PER_MINUTE);
      log.warn("账户已锁定: username={}, remainingMinutes={}", username, remainingMinutes);
      return LockStatus.locked(remainingMinutes);
    }

    return LockStatus.unlocked();
  }

  /**
   * 检查是否需要图形验证码
   *
   * @param username 用户名
   * @return 是否需要验证码
   */
  public boolean isCaptchaRequired(final String username) {
    String failCountKey = FAIL_COUNT_PREFIX + username;
    Object count = redisTemplate.opsForValue().get(failCountKey);

    if (count == null) {
      return false;
    }

    int failCount = Integer.parseInt(count.toString());
    return failCount >= CAPTCHA_REQUIRED_THRESHOLD;
  }

  /**
   * 获取当前失败次数
   *
   * @param username 用户名
   * @return 失败次数
   */
  public int getFailCount(final String username) {
    String failCountKey = FAIL_COUNT_PREFIX + username;
    Object count = redisTemplate.opsForValue().get(failCountKey);
    return count == null ? 0 : Integer.parseInt(count.toString());
  }

  /**
   * 记录登录失败
   *
   * @param username 用户名
   * @return 处理结果
   */
  public FailResult recordFailure(final String username) {
    return recordFailure(username, null);
  }

  /**
   * 记录登录失败（带IP信息）
   *
   * @param username 用户名
   * @param ip 来源IP
   * @return 处理结果
   */
  public FailResult recordFailure(final String username, final String ip) {
    String failCountKey = FAIL_COUNT_PREFIX + username;

    // 增加失败计数
    Long count = redisTemplate.opsForValue().increment(failCountKey, 1);
    if (count == null) {
      count = 1L;
    }

    // 设置过期时间
    if (count == 1) {
      redisTemplate.expire(failCountKey, FAIL_COUNT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    int failCount = count.intValue();
    log.warn("登录失败记录: username={}, ip={}, failCount={}", username, ip, failCount);

    // 发送登录失败告警（失败5次以上发送邮件）
    if (failCount >= LOCK_THRESHOLD_1 && isAlertEnabled("notification.alert.login.failure")) {
      try {
        alertService.sendLoginFailureAlert(username, ip != null ? ip : "未知", failCount);
      } catch (Exception e) {
        log.error("发送登录失败告警邮件失败", e);
      }
    }

    // 检查是否需要锁定
    if (failCount >= LOCK_THRESHOLD_3) {
      lockAccount(username, LOCK_DURATION_3, ip, "连续登录失败" + failCount + "次");
      return FailResult.locked(LOCK_DURATION_3, failCount);
    } else if (failCount >= LOCK_THRESHOLD_2) {
      lockAccount(username, LOCK_DURATION_2, ip, "连续登录失败" + failCount + "次");
      return FailResult.locked(LOCK_DURATION_2, failCount);
    } else if (failCount >= LOCK_THRESHOLD_1) {
      lockAccount(username, LOCK_DURATION_1, ip, "连续登录失败" + failCount + "次");
      return FailResult.locked(LOCK_DURATION_1, failCount);
    } else if (failCount >= CAPTCHA_REQUIRED_THRESHOLD) {
      return FailResult.captchaRequired(failCount);
    }

    return FailResult.normal(failCount, CAPTCHA_REQUIRED_THRESHOLD - failCount);
  }

  /**
   * 检查告警是否启用
   *
   * @param configKey 配置键
   * @return 是否启用
   */
  private boolean isAlertEnabled(final String configKey) {
    String enabled = configAppService.getConfigValue(configKey);
    return "true".equalsIgnoreCase(enabled);
  }

  /**
   * 锁定账户
   *
   * @param username 用户名
   * @param durationMinutes 锁定时长（分钟）
   * @param ip IP地址
   * @param reason 锁定原因
   */
  private void lockAccount(
      final String username, final int durationMinutes, final String ip, final String reason) {
    String lockKey = LOCK_PREFIX + username;
    redisTemplate.opsForValue().set(lockKey, "1", durationMinutes, TimeUnit.MINUTES);
    log.warn(
        "账户已锁定: username={}, durationMinutes={}, reason={}", username, durationMinutes, reason);

    // 发送账户锁定告警邮件
    if (isAlertEnabled("notification.alert.account.locked")) {
      try {
        alertService.sendAccountLockedAlert(
            username, ip != null ? ip : "未知", reason + "，锁定" + durationMinutes + "分钟");
      } catch (Exception e) {
        log.error("发送账户锁定告警邮件失败", e);
      }
    }
  }

  /**
   * 登录成功，清除失败记录
   *
   * @param username 用户名
   */
  public void clearFailure(final String username) {
    String failCountKey = FAIL_COUNT_PREFIX + username;
    String lockKey = LOCK_PREFIX + username;

    redisTemplate.delete(failCountKey);
    redisTemplate.delete(lockKey);

    log.debug("清除登录失败记录: username={}", username);
  }

  /**
   * 手动解锁账户（管理员操作）
   *
   * @param username 用户名
   */
  public void unlockAccount(final String username) {
    String failCountKey = FAIL_COUNT_PREFIX + username;
    String lockKey = LOCK_PREFIX + username;

    redisTemplate.delete(failCountKey);
    redisTemplate.delete(lockKey);

    log.info("手动解锁账户: username={}", username);
  }

  // ========== 结果类 ==========

  /** 锁定状态. */
  @lombok.Data
  @lombok.Builder
  public static class LockStatus {
    /** 是否锁定. */
    private boolean locked;

    /** 剩余分钟数. */
    private int remainingMinutes;

    /** 消息. */
    private String message;

    /**
     * 创建锁定状态
     *
     * @param remainingMinutes 剩余分钟数
     * @return 锁定状态
     */
    public static LockStatus locked(final int remainingMinutes) {
      return LockStatus.builder()
          .locked(true)
          .remainingMinutes(remainingMinutes)
          .message(String.format("账户已锁定，请 %d 分钟后重试", remainingMinutes))
          .build();
    }

    /**
     * 创建未锁定状态
     *
     * @return 锁定状态
     */
    public static LockStatus unlocked() {
      return LockStatus.builder().locked(false).remainingMinutes(0).build();
    }
  }

  /** 失败结果. */
  @lombok.Data
  @lombok.Builder
  public static class FailResult {
    /** 是否锁定. */
    private boolean locked;

    /** 是否需要验证码. */
    private boolean captchaRequired;

    /** 锁定持续时间（分钟）. */
    private int lockDurationMinutes;

    /** 失败次数. */
    private int failCount;

    /** 剩余尝试次数. */
    private int remainingAttempts;

    /** 消息. */
    private String message;

    /**
     * 创建锁定结果
     *
     * @param durationMinutes 锁定时长（分钟）
     * @param failCount 失败次数
     * @return 失败结果
     */
    public static FailResult locked(final int durationMinutes, final int failCount) {
      return FailResult.builder()
          .locked(true)
          .captchaRequired(true)
          .lockDurationMinutes(durationMinutes)
          .failCount(failCount)
          .remainingAttempts(0)
          .message(String.format("登录失败次数过多，账户已锁定 %d 分钟", durationMinutes))
          .build();
    }

    /**
     * 创建需要验证码的结果
     *
     * @param failCount 失败次数
     * @return 失败结果
     */
    public static FailResult captchaRequired(final int failCount) {
      return FailResult.builder()
          .locked(false)
          .captchaRequired(true)
          .failCount(failCount)
          .remainingAttempts(LOCK_THRESHOLD_1 - failCount)
          .message("登录失败次数较多，请完成图形验证码")
          .build();
    }

    /**
     * 创建正常结果
     *
     * @param failCount 失败次数
     * @param remainingBeforeCaptcha 距离需要验证码的剩余次数
     * @return 失败结果
     */
    public static FailResult normal(final int failCount, final int remainingBeforeCaptcha) {
      return FailResult.builder()
          .locked(false)
          .captchaRequired(false)
          .failCount(failCount)
          .remainingAttempts(remainingBeforeCaptcha)
          .message(String.format("用户名或密码错误，还可尝试 %d 次", remainingBeforeCaptcha))
          .build();
    }
  }
}
