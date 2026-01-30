package com.lawfirm.application.system.service;

import com.lawfirm.common.exception.BusinessException;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.base.Captcha;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** 验证码服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

  /** RedisTemplate. */
  private final RedisTemplate<String, Object> redisTemplate;

  /** CAPTCHA_PREFIX. */
  private static final String CAPTCHA_PREFIX = "captcha:";

  /** CAPTCHA_EXPIRE_MINUTES. */
  private static final int CAPTCHA_EXPIRE_MINUTES = 5; // 验证码5分钟过期

  /** CAPTCHA_WIDTH. */
  private static final int CAPTCHA_WIDTH = 120;

  /** CAPTCHA_HEIGHT. */
  private static final int CAPTCHA_HEIGHT = 40;

  /** 验证码字体大小 */
  private static final int CAPTCHA_FONT_SIZE = 25;

  /**
   * 生成验证码
   *
   * @return 验证码ID和Base64图片
   */
  public CaptchaResult generateCaptcha() {
    try {
      // 生成算术验证码（加减法）
      ArithmeticCaptcha captcha = new ArithmeticCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT);
      captcha.setLen(2); // 2位运算

      // 使用系统默认字体，避免在无头环境中字体不可用的问题
      try {
        // 尝试使用SansSerif字体（系统默认）
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, CAPTCHA_FONT_SIZE);
        captcha.setFont(font);
      } catch (Exception e) {
        // 如果设置字体失败，使用默认字体
        log.warn("设置验证码字体失败，使用默认字体", e);
      }

      // 生成验证码ID
      String captchaId = UUID.randomUUID().toString().replace("-", "");

      // 获取验证码答案（计算结果）
      String answer = captcha.text();

      // 存储到Redis，5分钟过期
      String cacheKey = CAPTCHA_PREFIX + captchaId;
      redisTemplate
          .opsForValue()
          .set(cacheKey, answer.toLowerCase(), CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

      // 转换为Base64图片
      String base64Image = captchaToBase64(captcha);

      log.debug("生成验证码: captchaId={}, answer={}", captchaId, answer);

      return CaptchaResult.builder()
          .captchaId(captchaId)
          .captchaUrl("data:image/png;base64," + base64Image)
          .build();
    } catch (Exception e) {
      log.error("生成验证码失败", e);
      throw new BusinessException("验证码生成失败，请稍后重试");
    }
  }

  /**
   * 验证验证码
   *
   * @param captchaId 验证码ID
   * @param captchaCode 用户输入的验证码
   * @return 是否验证通过
   */
  public boolean verifyCaptcha(final String captchaId, final String captchaCode) {
    if (captchaId == null || captchaCode == null) {
      return false;
    }

    String cacheKey = CAPTCHA_PREFIX + captchaId;
    Object storedAnswer = redisTemplate.opsForValue().get(cacheKey);

    if (storedAnswer == null) {
      log.warn("验证码不存在或已过期: captchaId={}", captchaId);
      return false;
    }

    // 验证码使用后立即删除（防止重复使用）
    redisTemplate.delete(cacheKey);

    // 比较验证码（不区分大小写）
    boolean verified = storedAnswer.toString().equalsIgnoreCase(captchaCode.trim());

    if (verified) {
      log.debug("验证码验证成功: captchaId={}", captchaId);
    } else {
      log.warn(
          "验证码验证失败: captchaId={}, input={}, expected={}", captchaId, captchaCode, storedAnswer);
    }

    return verified;
  }

  /**
   * 将验证码图片转换为Base64
   *
   * @param captcha 验证码对象
   * @return Base64字符串
   */
  private String captchaToBase64(final Captcha captcha) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      captcha.out(outputStream);
      byte[] imageBytes = outputStream.toByteArray();
      return Base64.getEncoder().encodeToString(imageBytes);
    } catch (IOException e) {
      log.error("验证码图片转换失败", e);
      throw new BusinessException("验证码生成失败");
    }
  }

  /** 验证码结果. */
  @lombok.Data
  @lombok.Builder
  public static class CaptchaResult {
    /** 验证码ID. */
    private String captchaId;

    /** Base64图片URL. */
    private String captchaUrl;
  }
}
