package com.lawfirm.application.admin.service;

import com.lawfirm.application.clientservice.dto.LetterVerificationPushDTO;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.domain.admin.entity.LetterApplication;
import com.lawfirm.domain.admin.repository.LetterApplicationRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.qrcode.QrCodeService;
import com.lawfirm.infrastructure.qrcode.VerificationCodeService;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/** 函件验证服务 基于基础设施层的二维码和验证码服务实现函件防伪验证. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LetterVerificationService {

  /** LetterApplication Repository. */
  private final LetterApplicationRepository letterApplicationRepository;

  /** VerificationCode Service. */
  private final VerificationCodeService verificationCodeService;

  /** QrCode Service. */
  private final QrCodeService qrCodeService;

  /** SysConfigApp Service. */
  private final SysConfigAppService sysConfigAppService;

  /** Integration Mapper. */
  private final ExternalIntegrationMapper integrationMapper;

  /** Matter Mapper. */
  private final MatterMapper matterMapper;

  /** 数据库配置键名：函件验证公开URL（客服系统验证页面地址）. */
  private static final String CONFIG_KEY_VERIFY_URL = "letter.verify.public.url";

  /** 客户服务系统的集成类型标识. */
  private static final String CLIENT_SERVICE_TYPE = "CLIENT_SERVICE";

  /** 律所名称配置键（与系统其他模块统一使用 firm.name）. */
  private static final String CONFIG_KEY_FIRM_NAME = "firm.name";

  /** 客服系统验证页面URL（兜底默认值）. */
  @Value("${lawfirm.public.verify.url:https://service.lawfirm.com}")
  private String defaultPublicVerifyUrl;

  /** 默认律所名称. */
  @Value("${lawfirm.firm.name:律师事务所}")
  private String defaultFirmName;

  /**
   * 获取公开验证URL（优先从数据库配置读取，否则使用配置文件中的值）
   *
   * @return 公开验证URL
   */
  private String getPublicVerifyUrl() {
    try {
      String configUrl = sysConfigAppService.getConfigValue(CONFIG_KEY_VERIFY_URL);
      if (StringUtils.hasText(configUrl)) {
        return configUrl;
      }
    } catch (Exception e) {
      log.warn("从数据库获取公开验证URL失败，使用默认值: {}", defaultPublicVerifyUrl, e);
    }
    return defaultPublicVerifyUrl;
  }

  /**
   * 生成函件验证码
   *
   * @param application 函件申请
   * @return 验证码（Base64编码）
   */
  @Transactional
  public String generateVerificationCode(final LetterApplication application) {
    // 如果已经打印过，使用打印时间；否则使用当前时间
    LocalDateTime timestamp =
        application.getPrintedAt() != null ? application.getPrintedAt() : LocalDateTime.now();

    return verificationCodeService.generateCode(
        VerificationCodeService.BUSINESS_TYPE_LETTER,
        application.getId(),
        application.getApplicationNo(),
        timestamp);
  }

  /**
   * 验证函件验证码
   *
   * @param applicationNo 申请编号
   * @param verificationCode 验证码
   * @return 是否有效
   */
  public boolean verifyCode(final String applicationNo, final String verificationCode) {
    LetterApplication application =
        letterApplicationRepository.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                    LetterApplication>()
                .eq(LetterApplication::getApplicationNo, applicationNo)
                .eq(LetterApplication::getDeleted, false),
            false // 不抛出异常，返回null
            );

    if (application == null) {
      log.warn("函件不存在: applicationNo={}", applicationNo);
      return false;
    }

    LocalDateTime timestamp =
        application.getPrintedAt() != null
            ? application.getPrintedAt()
            : application.getCreatedAt();

    return verificationCodeService.verifyCode(
        VerificationCodeService.BUSINESS_TYPE_LETTER,
        application.getId(),
        applicationNo,
        timestamp,
        verificationCode);
  }

  /**
   * 生成验证二维码URL
   *
   * @param application 函件申请
   * @return 验证URL
   */
  public String generateVerificationUrl(final LetterApplication application) {
    String verificationCode = generateVerificationCode(application);
    return verificationCodeService.generateVerificationUrl(
        getPublicVerifyUrl(),
        VerificationCodeService.BUSINESS_TYPE_LETTER,
        application.getApplicationNo(),
        verificationCode);
  }

  /**
   * 生成验证二维码图片（Base64编码）
   *
   * @param application 函件申请
   * @param size 二维码尺寸（像素），默认200
   * @return Base64编码的二维码图片
   */
  public String generateQrCodeBase64(final LetterApplication application, final Integer size) {
    String verificationUrl = generateVerificationUrl(application);
    return qrCodeService.generateQrCodeBase64(verificationUrl, size);
  }

  /**
   * 生成验证二维码图片（字节数组）
   *
   * @param application 函件申请
   * @param size 二维码尺寸（像素），默认200
   * @return PNG格式的字节数组
   */
  public byte[] generateQrCodeBytes(final LetterApplication application, final Integer size) {
    String verificationUrl = generateVerificationUrl(application);
    return qrCodeService.generateQrCodeBytes(verificationUrl, size);
  }

  /**
   * 推送验证数据到客服系统并生成二维码 数据隔离方案：验证数据完全存储在客服系统，律所系统不对外暴露验证接口
   *
   * @param application 函件申请
   * @param size 二维码尺寸（像素），默认200
   * @return 二维码Base64或null（客服系统未配置时使用本地URL）
   */
  @Transactional
  public String generateQrCodeWithPush(final LetterApplication application, final Integer size) {
    // 1. 生成验证码
    String verificationCode = generateVerificationCode(application);

    // 2. 获取客服系统配置
    ExternalIntegration integration = getClientServiceIntegration();

    String verificationUrl;

    if (integration != null && Boolean.TRUE.equals(integration.getEnabled())) {
      // 3. 推送验证数据到客服系统
      try {
        verificationUrl =
            pushVerificationToClientService(integration, application, verificationCode);
        log.info(
            "函件验证数据已推送到客服系统: applicationNo={}, url={}",
            application.getApplicationNo(),
            verificationUrl);
      } catch (Exception e) {
        log.error("推送验证数据到客服系统失败，使用本地URL: applicationNo={}", application.getApplicationNo(), e);
        // 推送失败，使用本地URL作为兜底（但本地接口已删除，扫码将无法验证）
        verificationUrl =
            verificationCodeService.generateVerificationUrl(
                getPublicVerifyUrl(),
                VerificationCodeService.BUSINESS_TYPE_LETTER,
                application.getApplicationNo(),
                verificationCode);
      }
    } else {
      // 客服系统未配置，使用本地URL
      log.warn("客服系统未配置，函件二维码将指向本地URL（需配置客服系统后才能正常验证）");
      verificationUrl =
          verificationCodeService.generateVerificationUrl(
              getPublicVerifyUrl(),
              VerificationCodeService.BUSINESS_TYPE_LETTER,
              application.getApplicationNo(),
              verificationCode);
    }

    // 4. 生成二维码
    return qrCodeService.generateQrCodeBase64(verificationUrl, size);
  }

  /**
   * 推送验证数据到客服系统
   *
   * @param integration 客服系统集成配置
   * @param application 函件申请
   * @param verificationCode 验证码
   * @return 客服系统返回的验证URL
   */
  private String pushVerificationToClientService(
      final ExternalIntegration integration,
      final LetterApplication application,
      final String verificationCode) {
    RestTemplate restTemplate = new RestTemplate();

    // 构建请求头
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (StringUtils.hasText(integration.getApiKey())) {
      headers.set("Authorization", "Bearer " + integration.getApiKey());
    }

    // 获取项目名称（脱敏处理，只取部分信息）
    String matterName = null;
    if (application.getMatterId() != null) {
      Matter matter = matterMapper.selectById(application.getMatterId());
      if (matter != null) {
        // 脱敏处理：只显示项目类型，不显示具体案情
        matterName = matter.getMatterType() != null ? matter.getMatterType() + "案件" : "关联项目";
      }
    }

    // 构建推送数据
    LetterVerificationPushDTO pushData =
        LetterVerificationPushDTO.builder()
            .letterId(application.getId())
            .applicationNo(application.getApplicationNo())
            .verificationCode(verificationCode)
            .letterType(application.getLetterType())
            .letterTypeName(getLetterTypeName(application.getLetterType()))
            .targetUnit(application.getTargetUnit())
            .lawyerNames(application.getLawyerNames())
            .firmName(getFirmName())
            .matterName(matterName)
            .approvedAt(application.getApprovedAt())
            .printedAt(
                application.getPrintedAt() != null
                    ? application.getPrintedAt()
                    : LocalDateTime.now())
            .validUntil(LocalDateTime.now().plusYears(1)) // 默认1年有效期
            .remark("来自律所管理系统的函件验证数据")
            .build();

    HttpEntity<LetterVerificationPushDTO> entity = new HttpEntity<>(pushData, headers);

    // 发送请求到客服系统的函件验证数据接收接口
    String apiUrl = integration.getApiUrl() + "/letter/verification/receive";
    @SuppressWarnings("rawtypes")
    ResponseEntity<Map> response =
        restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> body = (Map<String, Object>) response.getBody();
      // 客服系统返回验证页面URL
      String url = (String) body.get("verifyUrl");
      if (StringUtils.hasText(url)) {
        return url;
      }
      // 如果没返回URL，则构造默认URL
      return integration.getApiUrl() + "/verify/letter?no=" + application.getApplicationNo();
    } else {
      throw new RuntimeException("客服系统返回错误: " + response.getStatusCode());
    }
  }

  /**
   * 获取客户服务系统集成配置
   *
   * @return 客户服务系统集成配置
   */
  private ExternalIntegration getClientServiceIntegration() {
    return integrationMapper.selectByType(CLIENT_SERVICE_TYPE);
  }

  /**
   * 获取律所名称
   *
   * @return 律所名称
   */
  private String getFirmName() {
    try {
      String firmName = sysConfigAppService.getConfigValue(CONFIG_KEY_FIRM_NAME);
      if (StringUtils.hasText(firmName)) {
        return firmName;
      }
    } catch (Exception e) {
      log.debug("从数据库获取律所名称失败，使用默认值");
    }
    return defaultFirmName;
  }

  /**
   * 获取函件类型名称
   *
   * @param letterType 函件类型
   * @return 函件类型名称
   */
  private String getLetterTypeName(final String letterType) {
    if (letterType == null) {
      return "函件";
    }
    return switch (letterType) {
      case "INTRODUCTION" -> "介绍信";
      case "MEETING" -> "会见函";
      case "INVESTIGATION" -> "调查函";
      case "FILE_REVIEW" -> "阅卷函";
      case "LEGAL_OPINION" -> "法律意见函";
      default -> "函件";
    };
  }
}
