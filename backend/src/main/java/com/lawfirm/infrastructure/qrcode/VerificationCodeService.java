package com.lawfirm.infrastructure.qrcode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 验证码生成服务 用于生成和验证各种业务对象的防伪验证码
 *
 * <p>使用场景： - 函件防伪验证 - 合同真伪验证 - 项目信息验证 - 其他需要防伪验证的业务场景
 *
 * <p>验证码生成算法：HMAC-SHA256 格式：HMAC-SHA256(businessType + businessId + businessNo + timestamp, secret)
 *
 * @author junyuzhan
 */
@Slf4j
@Service
public class VerificationCodeService {

  /** 验证码密钥（从配置读取，生产环境应使用强密钥） */
  @Value("${lawfirm.verification.secret:lawfirm-verification-secret-key-2024-change-in-production}")
  private String verificationSecret;

  /** 业务类型：函件 */
  public static final String BUSINESS_TYPE_LETTER = "LETTER";

  /** 业务类型：合同 */
  public static final String BUSINESS_TYPE_CONTRACT = "CONTRACT";

  /** 业务类型：项目 */
  public static final String BUSINESS_TYPE_MATTER = "MATTER";

  /**
   * 生成验证码
   *
   * @param businessType 业务类型（如：LETTER, CONTRACT, MATTER）
   * @param businessId 业务ID
   * @param businessNo 业务编号（如：申请编号、合同编号等）
   * @param timestamp 时间戳（用于防重放，通常使用创建时间或打印时间）
   * @return Base64编码的验证码
   */
  public String generateCode(
      final String businessType,
      final Long businessId,
      final String businessNo,
      final LocalDateTime timestamp) {
    try {
      // 构建待签名字符串：业务类型|业务ID|业务编号|时间戳
      String dataToSign =
          String.format("%s|%d|%s|%s", businessType, businessId, businessNo, timestamp.toString());

      // 使用HMAC-SHA256生成签名
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(verificationSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKeySpec);

      byte[] signature = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));

      // Base64 URL编码返回（去掉填充，便于URL传输）
      return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    } catch (Exception e) {
      log.error(
          "生成验证码失败: businessType={}, businessId={}, businessNo={}, error={}",
          businessType,
          businessId,
          businessNo,
          e.getMessage(),
          e);
      throw new RuntimeException("生成验证码失败", e);
    }
  }

  /**
   * 验证验证码
   *
   * @param businessType 业务类型
   * @param businessId 业务ID
   * @param businessNo 业务编号
   * @param timestamp 时间戳
   * @param verificationCode 待验证的验证码
   * @return 是否有效
   */
  public boolean verifyCode(
      final String businessType,
      final Long businessId,
      final String businessNo,
      final LocalDateTime timestamp,
      final String verificationCode) {
    try {
      // 重新生成验证码进行比对
      String expectedCode = generateCode(businessType, businessId, businessNo, timestamp);
      return expectedCode.equals(verificationCode);
    } catch (Exception e) {
      log.error(
          "验证验证码失败: businessType={}, businessId={}, businessNo={}, error={}",
          businessType,
          businessId,
          businessNo,
          e.getMessage(),
          e);
      return false;
    }
  }

  /**
   * 生成验证URL
   *
   * @param publicVerifyUrl 公开验证网站URL（从配置读取）
   * @param businessType 业务类型
   * @param businessNo 业务编号
   * @param verificationCode 验证码
   * @return 完整的验证URL
   */
  public String generateVerificationUrl(
      final String publicVerifyUrl,
      final String businessType,
      final String businessNo,
      final String verificationCode) {
    return String.format(
        "%s/verify/%s?no=%s&code=%s",
        publicVerifyUrl, businessType.toLowerCase(), businessNo, verificationCode);
  }
}
