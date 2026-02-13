package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.UpdateExternalIntegrationCommand;
import com.lawfirm.application.system.dto.ExternalIntegrationDTO;
import com.lawfirm.application.system.dto.ExternalIntegrationQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.security.AesEncryptionService;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.repository.ExternalIntegrationRepository;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 外部系统集成应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIntegrationAppService {

  /** 外部集成仓储 */
  private final ExternalIntegrationRepository integrationRepository;

  /** 外部集成Mapper */
  private final ExternalIntegrationMapper integrationMapper;

  /** AES加密服务 */
  private final AesEncryptionService encryptionService;

  /** 连接超时时间（毫秒） */
  @Value("${law-firm.integration.connect-timeout-ms:5000}")
  private int connectTimeoutMs;

  /** 读取超时时间（毫秒） */
  @Value("${law-firm.integration.read-timeout-ms:5000}")
  private int readTimeoutMs;

  /** HTTP成功状态码范围起始值 */
  private static final int HTTP_SUCCESS_START = 200;

  /** HTTP成功状态码范围结束值 */
  private static final int HTTP_SUCCESS_END = 400;

  /** HTTP状态码：成功 */
  private static final int HTTP_STATUS_OK = 200;

  /** HTTP状态码：未授权 */
  private static final int HTTP_STATUS_UNAUTHORIZED = 401;

  /** HTTP状态码：禁止访问 */
  private static final int HTTP_STATUS_FORBIDDEN = 403;

  /** HTTP状态码：请求过多 */
  private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

  /**
   * 分页查询
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<ExternalIntegrationDTO> listIntegrations(
      final ExternalIntegrationQueryDTO query) {
    IPage<ExternalIntegration> page =
        integrationMapper.selectPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getIntegrationType(),
            query.getEnabled(),
            query.getKeyword());

    List<ExternalIntegrationDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取所有集成配置
   *
   * @return 集成配置列表
   */
  public List<ExternalIntegrationDTO> listAllIntegrations() {
    return integrationMapper.selectAllIntegrations().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取指定类型的启用集成
   *
   * @param type 集成类型
   * @return 集成配置列表
   */
  public List<ExternalIntegrationDTO> listEnabledByType(final String type) {
    return integrationMapper.selectEnabledByType(type).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 根据ID获取集成配置
   *
   * @param id 集成配置ID
   * @return 集成配置DTO
   */
  public ExternalIntegrationDTO getIntegrationById(final Long id) {
    ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");
    return toDTO(integration);
  }

  /**
   * 根据编码获取集成配置
   *
   * @param code 集成编码
   * @return 集成配置DTO
   */
  public ExternalIntegrationDTO getIntegrationByCode(final String code) {
    ExternalIntegration integration = integrationMapper.selectByCode(code);
    if (integration == null) {
      throw new BusinessException("集成配置不存在: " + code);
    }
    return toDTO(integration);
  }

  /**
   * 创建集成配置
   *
   * @param command 更新集成配置命令
   * @return 集成配置DTO
   */
  @Transactional
  public ExternalIntegrationDTO createIntegration(final UpdateExternalIntegrationCommand command) {
    // 权限验证：只有管理员才能创建集成配置
    if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能创建集成配置");
    }

    // 检查编码是否重复
    ExternalIntegration existing = integrationMapper.selectByCode(command.getIntegrationCode());
    if (existing != null) {
      throw new BusinessException("集成编码已存在: " + command.getIntegrationCode());
    }

    ExternalIntegration integration = new ExternalIntegration();
    integration.setIntegrationCode(command.getIntegrationCode());
    integration.setIntegrationName(command.getIntegrationName());
    integration.setIntegrationType(command.getIntegrationType());
    integration.setApiUrl(command.getApiUrl());
    integration.setAuthType(command.getAuthType());
    integration.setExtraConfig(command.getExtraConfig());
    integration.setDescription(command.getDescription());
    integration.setEnabled(false); // 默认禁用

    // 加密存储 API 密钥
    if (StringUtils.hasText(command.getApiKey())) {
      integration.setApiKey(encryptionService.encrypt(command.getApiKey()));
    }
    if (StringUtils.hasText(command.getApiSecret())) {
      integration.setApiSecret(encryptionService.encrypt(command.getApiSecret()));
    }

    integrationMapper.insert(integration);
    log.info(
        "创建集成配置成功: integrationCode={}, operator={}",
        integration.getIntegrationCode(),
        SecurityUtils.getUserId());

    return toDTO(integration);
  }

  /**
   * 更新集成配置
   *
   * @param command 更新集成配置命令
   */
  @Transactional
  public void updateIntegration(final UpdateExternalIntegrationCommand command) {
    // ✅ 权限验证：只有管理员才能修改集成配置
    if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能修改集成配置");
    }

    ExternalIntegration integration =
        integrationRepository.getByIdOrThrow(command.getId(), "集成配置不存在");

    // 记录是否修改了敏感信息
    boolean sensitiveChanged = false;

    if (StringUtils.hasText(command.getApiUrl())) {
      integration.setApiUrl(command.getApiUrl());
    }
    if (StringUtils.hasText(command.getApiKey())) {
      // ✅ 问题531已修复：使用AES加密存储API密钥
      String encryptedKey = encryptionService.encrypt(command.getApiKey());
      integration.setApiKey(encryptedKey);
      sensitiveChanged = true;
    }
    if (StringUtils.hasText(command.getApiSecret())) {
      // ✅ 问题531已修复：使用AES加密存储API密钥
      String encryptedSecret = encryptionService.encrypt(command.getApiSecret());
      integration.setApiSecret(encryptedSecret);
      sensitiveChanged = true;
    }
    if (StringUtils.hasText(command.getAuthType())) {
      integration.setAuthType(command.getAuthType());
    }
    if (command.getExtraConfig() != null) {
      integration.setExtraConfig(command.getExtraConfig());
    }
    if (StringUtils.hasText(command.getDescription())) {
      integration.setDescription(command.getDescription());
    }

    integrationRepository.updateById(integration);

    // ✅ 敏感信息修改时记录审计日志
    if (sensitiveChanged) {
      log.warn(
          "【敏感操作】集成配置敏感信息已修改: integrationCode={}, operator={}, operatorName={}",
          integration.getIntegrationCode(),
          SecurityUtils.getUserId(),
          SecurityUtils.getUsername());
    } else {
      log.info("集成配置更新成功: {}", integration.getIntegrationCode());
    }
  }

  /**
   * 启用集成
   *
   * @param id 集成配置ID
   */
  @Transactional
  public void enableIntegration(final Long id) {
    // ✅ 权限验证：只有管理员才能启用集成
    if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能启用集成");
    }

    ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");

    // 检查必要配置是否完整
    if (!StringUtils.hasText(integration.getApiUrl())) {
      throw new BusinessException("请先配置API地址");
    }
    if (!StringUtils.hasText(integration.getApiKey())
        && !StringUtils.hasText(integration.getApiSecret())) {
      throw new BusinessException("请先配置API密钥");
    }

    integrationMapper.updateEnabled(id, true);
    log.info(
        "集成已启用: integrationCode={}, operator={}",
        integration.getIntegrationCode(),
        SecurityUtils.getUserId());
  }

  /**
   * 禁用集成
   *
   * @param id 集成配置ID
   */
  @Transactional
  public void disableIntegration(final Long id) {
    // ✅ 权限验证：只有管理员才能禁用集成
    if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能禁用集成");
    }

    ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");
    integrationMapper.updateEnabled(id, false);
    log.info(
        "集成已禁用: integrationCode={}, operator={}",
        integration.getIntegrationCode(),
        SecurityUtils.getUserId());
  }

  /**
   * 测试连接 注意：不使用@Transactional，因为网络请求可能耗时较长
   *
   * @param id 集成配置ID
   * @return 集成配置DTO（包含测试结果）
   */
  public ExternalIntegrationDTO testConnection(final Long id) {
    // ✅ 权限验证：只有管理员才能测试连接
    if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
      throw new BusinessException("权限不足：只有管理员才能测试连接");
    }

    ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");

    if (!StringUtils.hasText(integration.getApiUrl())) {
      updateTestResultInTransaction(id, ExternalIntegration.TEST_FAILED, "API地址未配置");
      throw new BusinessException("API地址未配置");
    }

    String result;
    String message;

    try {
      // 根据集成类型选择不同的测试方式
      if (ExternalIntegration.TYPE_AI.equals(integration.getIntegrationType())) {
        // AI 集成：验证 API Key 是否有效
        TestResult testResult = testAiApiKey(integration);
        result =
            testResult.success ? ExternalIntegration.TEST_SUCCESS : ExternalIntegration.TEST_FAILED;
        message = testResult.message;
      } else if (ExternalIntegration.TYPE_CLIENT_SERVICE.equals(integration.getIntegrationType())) {
        // 客户服务系统：测试 /api/health 端点
        TestResult testResult = testClientServiceHealth(integration);
        result =
            testResult.success ? ExternalIntegration.TEST_SUCCESS : ExternalIntegration.TEST_FAILED;
        message = testResult.message;
      } else {
        // 其他集成：简单的连通性测试
        String apiUrl = integration.getApiUrl().trim();
        URL url = URI.create(apiUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
          connection.setConnectTimeout(connectTimeoutMs);
          connection.setReadTimeout(readTimeoutMs);
          connection.setRequestMethod("HEAD");

          int responseCode = connection.getResponseCode();
          if (responseCode >= HTTP_SUCCESS_START && responseCode < HTTP_SUCCESS_END) {
            result = ExternalIntegration.TEST_SUCCESS;
            message = "连接成功，响应码: " + responseCode;
          } else {
            result = ExternalIntegration.TEST_FAILED;
            message = "连接失败，响应码: " + responseCode;
          }
        } finally {
          connection.disconnect();
        }
      }
    } catch (Exception e) {
      result = ExternalIntegration.TEST_FAILED;
      message = "连接异常: " + e.getMessage();
      log.error("测试连接失败: {}", integration.getIntegrationCode(), e);
    }

    // ✅ 单独的事务更新测试结果
    updateTestResultInTransaction(id, result, message);

    // 重新获取更新后的记录
    return getIntegrationById(id);
  }

  /**
   * 测试 AI API Key 是否有效 通过调用 /models 接口验证密钥
   *
   * @param integration 集成配置
   * @return 测试结果
   */
  private TestResult testAiApiKey(final ExternalIntegration integration) {
    String apiUrl = integration.getApiUrl().trim();
    if (!apiUrl.endsWith("/")) {
      apiUrl += "/";
    }

    // 解密 API Key
    String apiKey = integration.getApiKey();
    if (StringUtils.hasText(apiKey)) {
      try {
        apiKey = encryptionService.decrypt(apiKey);
      } catch (Exception e) {
        log.warn("API密钥解密失败，使用原值");
      }
    }

    if (!StringUtils.hasText(apiKey)) {
      return new TestResult(false, "API Key 未配置");
    }

    HttpURLConnection connection = null;
    try {
      // 调用 models 接口验证 API Key
      URL url = URI.create(apiUrl + "models").toURL();
      connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(connectTimeoutMs);
      connection.setReadTimeout(readTimeoutMs);
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Content-Type", "application/json");

      int responseCode = connection.getResponseCode();

      if (responseCode == HTTP_STATUS_OK) {
        return new TestResult(true, "API Key 验证成功");
      } else if (responseCode == HTTP_STATUS_UNAUTHORIZED) {
        return new TestResult(false, "API Key 无效或已过期 (401)");
      } else if (responseCode == HTTP_STATUS_FORBIDDEN) {
        return new TestResult(false, "API Key 权限不足 (403)");
      } else if (responseCode == HTTP_STATUS_TOO_MANY_REQUESTS) {
        return new TestResult(false, "API 调用次数超限 (429)");
      } else {
        return new TestResult(false, "API 验证失败，响应码: " + responseCode);
      }
    } catch (java.net.SocketTimeoutException e) {
      return new TestResult(false, "连接超时，请检查网络");
    } catch (Exception e) {
      return new TestResult(false, "连接异常: " + e.getMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  /**
   * 测试客户服务系统健康状态 通过调用 /api/health 接口验证服务可用性
   *
   * @param integration 集成配置
   * @return 测试结果
   */
  private TestResult testClientServiceHealth(final ExternalIntegration integration) {
    String apiUrl = integration.getApiUrl().trim();
    // 移除末尾的斜杠，避免双斜杠
    if (apiUrl.endsWith("/")) {
      apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
    }
    // 确保路径以 /api 结尾，然后拼接 /health
    String healthUrl = apiUrl.endsWith("/api") ? apiUrl + "/health" : apiUrl + "/api/health";

    HttpURLConnection connection = null;
    try {
      // 调用 health 接口验证服务可用性
      URL url = URI.create(healthUrl).toURL();
      connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(connectTimeoutMs);
      connection.setReadTimeout(readTimeoutMs);
      connection.setRequestMethod("HEAD");

      int responseCode = connection.getResponseCode();

      if (responseCode == HTTP_STATUS_OK) {
        return new TestResult(true, "客户服务系统连接成功，响应码: " + responseCode);
      } else {
        return new TestResult(false, "客户服务系统连接失败，响应码: " + responseCode);
      }
    } catch (java.net.SocketTimeoutException e) {
      return new TestResult(false, "连接超时，请检查网络和API地址配置");
    } catch (java.net.UnknownHostException e) {
      return new TestResult(false, "无法解析主机名，请检查API地址配置");
    } catch (java.net.ConnectException e) {
      return new TestResult(false, "无法连接到客户服务系统，请检查服务是否运行");
    } catch (Exception e) {
      return new TestResult(false, "连接异常: " + e.getMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  /** 测试结果内部类 */
  private static class TestResult {
    /** 是否成功 */
    final boolean success;

    /** 消息 */
    final String message;

    TestResult(boolean success, String message) {
      this.success = success;
      this.message = message;
    }
  }

  /**
   * 在单独事务中更新测试结果（避免网络请求占用事务）
   *
   * @param id 集成ID
   * @param result 测试结果
   * @param message 消息
   */
  @Transactional
  public void updateTestResultInTransaction(
      final Long id, final String result, final String message) {
    integrationMapper.updateTestResult(id, result, message);
  }

  // ============ 通用集成查询方法（消除代码重复 - 问题551）============

  /**
   * 根据类型获取第一个启用的集成配置（通用方法）
   *
   * @param type 集成类型
   * @return 集成配置，如果没有启用的则返回null
   */
  public ExternalIntegration getFirstEnabledIntegrationByType(final String type) {
    List<ExternalIntegration> list = integrationMapper.selectEnabledByType(type);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * 根据类型获取所有启用的集成配置列表（通用方法）
   *
   * @param type 集成类型
   * @return 集成配置列表
   */
  public List<ExternalIntegration> getAllEnabledIntegrationsByType(final String type) {
    return integrationMapper.selectEnabledByType(type);
  }

  /**
   * 根据ID获取指定类型的集成配置（通用方法）
   *
   * @param id 集成ID
   * @param expectedType 期望的类型
   * @param typeName 类型名称（用于错误提示）
   * @return 集成配置
   */
  public ExternalIntegration getIntegrationByIdAndType(
      final Long id, final String expectedType, final String typeName) {
    ExternalIntegration integration =
        integrationRepository.getByIdOrThrow(id, typeName + "集成配置不存在");
    if (!expectedType.equals(integration.getIntegrationType())) {
      throw new BusinessException("指定的集成配置不是" + typeName + "类型");
    }
    return integration;
  }

  // ============ 解密API密钥方法（问题531修复）============

  /**
   * 获取解密后的API密钥 供需要实际使用密钥调用外部API时使用
   *
   * @param integrationId 集成配置ID
   * @return 解密后的API密钥
   */
  public String getDecryptedApiKey(final Long integrationId) {
    ExternalIntegration integration =
        integrationRepository.getByIdOrThrow(integrationId, "集成配置不存在");

    if (!StringUtils.hasText(integration.getApiKey())) {
      return null;
    }

    try {
      return encryptionService.decrypt(integration.getApiKey());
    } catch (Exception e) {
      // 兼容历史未加密数据
      log.warn("API密钥解密失败，可能是未加密的历史数据: integrationId={}", integrationId);
      return integration.getApiKey();
    }
  }

  /**
   * 获取解密后的API密钥Secret 供需要实际使用密钥调用外部API时使用
   *
   * @param integrationId 集成配置ID
   * @return 解密后的API密钥Secret
   */
  public String getDecryptedApiSecret(final Long integrationId) {
    ExternalIntegration integration =
        integrationRepository.getByIdOrThrow(integrationId, "集成配置不存在");

    if (!StringUtils.hasText(integration.getApiSecret())) {
      return null;
    }

    try {
      return encryptionService.decrypt(integration.getApiSecret());
    } catch (Exception e) {
      // 兼容历史未加密数据
      log.warn("API密钥Secret解密失败，可能是未加密的历史数据: integrationId={}", integrationId);
      return integration.getApiSecret();
    }
  }

  /**
   * 获取解密后的完整集成配置 包含解密后的API密钥，用于实际调用外部API
   *
   * @param id 集成配置ID
   * @return 包含解密密钥的集成实体
   */
  public ExternalIntegration getIntegrationWithDecryptedKeys(final Long id) {
    ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");

    // 解密API密钥
    if (StringUtils.hasText(integration.getApiKey())) {
      try {
        integration.setApiKey(encryptionService.decrypt(integration.getApiKey()));
      } catch (Exception e) {
        log.warn("API密钥解密失败，使用原值: integrationId={}", id);
      }
    }

    if (StringUtils.hasText(integration.getApiSecret())) {
      try {
        integration.setApiSecret(encryptionService.decrypt(integration.getApiSecret()));
      } catch (Exception e) {
        log.warn("API密钥Secret解密失败，使用原值: integrationId={}", id);
      }
    }

    return integration;
  }

  // ============ AI集成便捷方法 ============

  /**
   * 获取AI集成配置（供业务使用） 返回第一个启用的 AI 集成
   *
   * @return AI集成配置
   */
  public ExternalIntegration getEnabledAIIntegration() {
    return getFirstEnabledIntegrationByType(ExternalIntegration.TYPE_AI);
  }

  /**
   * 获取所有启用的 AI 集成列表
   *
   * @return AI集成配置列表
   */
  public List<ExternalIntegration> getAllEnabledAIIntegrations() {
    return getAllEnabledIntegrationsByType(ExternalIntegration.TYPE_AI);
  }

  /**
   * 根据 ID 获取 AI 集成配置
   *
   * @param id 集成配置ID
   * @return AI集成配置
   */
  public ExternalIntegration getAIIntegrationById(final Long id) {
    return getIntegrationByIdAndType(id, ExternalIntegration.TYPE_AI, "AI");
  }

  // ============ 档案系统集成便捷方法 ============

  /**
   * 获取档案系统集成配置（供业务使用）
   *
   * @return 档案系统集成配置
   */
  public ExternalIntegration getEnabledArchiveIntegration() {
    return getFirstEnabledIntegrationByType(ExternalIntegration.TYPE_ARCHIVE);
  }

  /**
   * 获取所有启用的档案系统集成列表
   *
   * @return 档案系统集成配置列表
   */
  public List<ExternalIntegration> getAllEnabledArchiveIntegrations() {
    return getAllEnabledIntegrationsByType(ExternalIntegration.TYPE_ARCHIVE);
  }

  /**
   * 根据ID获取档案系统集成配置
   *
   * @param id 集成配置ID
   * @return 档案系统集成配置
   */
  public ExternalIntegration getArchiveIntegrationById(final Long id) {
    return getIntegrationByIdAndType(id, ExternalIntegration.TYPE_ARCHIVE, "档案系统");
  }

  // ============ OCR集成便捷方法 ============

  /**
   * 获取OCR集成配置
   *
   * @return OCR集成配置
   */
  public ExternalIntegration getEnabledOCRIntegration() {
    return getFirstEnabledIntegrationByType(ExternalIntegration.TYPE_OCR);
  }

  /**
   * 获取所有启用的OCR集成列表
   *
   * @return OCR集成配置列表
   */
  public List<ExternalIntegration> getAllEnabledOCRIntegrations() {
    return getAllEnabledIntegrationsByType(ExternalIntegration.TYPE_OCR);
  }

  /**
   * 转换为DTO（脱敏处理）
   *
   * @param entity 集成配置实体
   * @return 集成配置DTO
   */
  private ExternalIntegrationDTO toDTO(final ExternalIntegration entity) {
    ExternalIntegrationDTO dto = new ExternalIntegrationDTO();
    dto.setId(entity.getId());
    dto.setIntegrationCode(entity.getIntegrationCode());
    dto.setIntegrationName(entity.getIntegrationName());
    dto.setIntegrationType(entity.getIntegrationType());
    dto.setDescription(entity.getDescription());
    dto.setApiUrl(entity.getApiUrl());

    // API密钥脱敏显示
    if (StringUtils.hasText(entity.getApiKey())) {
      String key = entity.getApiKey();
      if (key.length() > 8) {
        dto.setApiKey(key.substring(0, 4) + "****" + key.substring(key.length() - 4));
      } else {
        dto.setApiKey("****");
      }
    }
    dto.setHasApiSecret(StringUtils.hasText(entity.getApiSecret()));

    dto.setAuthType(entity.getAuthType());
    dto.setExtraConfig(entity.getExtraConfig());
    dto.setEnabled(entity.getEnabled());
    dto.setLastTestTime(entity.getLastTestTime());
    dto.setLastTestResult(entity.getLastTestResult());
    dto.setLastTestMessage(entity.getLastTestMessage());
    dto.setCreatedAt(entity.getCreatedAt());
    dto.setUpdatedAt(entity.getUpdatedAt());

    return dto;
  }
}
