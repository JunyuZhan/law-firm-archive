package com.lawfirm.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.domain.ai.entity.AiPricingConfig;
import com.lawfirm.domain.ai.entity.AiUsageLog;
import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiPricingConfigRepository;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.infrastructure.security.LoginUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AI使用量记录器
 *
 * <p>负责： 1. 解析API响应获取Token数量 2. 计算费用 3. 异步写入数据库 4. 更新用户配额
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiUsageRecorder {

  /** AI使用日志仓储 */
  private final AiUsageLogRepository usageLogRepository;

  /** AI定价配置仓储 */
  private final AiPricingConfigRepository pricingRepository;

  /** AI用户配额仓储 */
  private final AiUserQuotaRepository quotaRepository;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /** 截断长度 */
  private static final int TRUNCATE_LENGTH = 500;

  /** 截断后长度 */
  private static final int TRUNCATED_LENGTH = 497;

  /** Token计费除数（每千Token） */
  private static final int TOKEN_DIVISOR = 1000;

  /** 百分比除数 */
  private static final int PERCENTAGE_DIVISOR = 100;

  /** 字符到Token的转换比例 */
  private static final int CHAR_TO_TOKEN_RATIO = 2;

  /** 是否启用计费 */
  @Value("${law-firm.ai.billing.enabled:true}")
  private boolean billingEnabled;

  /** 默认计费比例 */
  @Value("${law-firm.ai.billing.charge-ratio:100}")
  private int defaultChargeRatio;

  /**
   * 记录AI使用量（异步执行，不影响主流程）
   *
   * @param integration AI集成配置
   * @param requestType 请求类型（DOCUMENT_GENERATE/CHAT/SUMMARY等）
   * @param businessType 业务类型（MATTER/PERSONAL）
   * @param businessId 业务ID（如项目ID）
   * @param responseBody API响应体
   * @param durationMs 响应时间
   * @param success 是否成功
   * @param errorMessage 错误信息
   */
  @Async
  public void recordUsage(
      final ExternalIntegration integration,
      final String requestType,
      final String businessType,
      final Long businessId,
      final String responseBody,
      final long durationMs,
      final boolean success,
      final String errorMessage) {

    try {
      // 获取当前用户信息
      LoginUser loginUser = getLoginUserSafely();
      if (loginUser == null) {
        log.warn("无法记录AI使用量：用户未登录");
        return;
      }

      Long userId = loginUser.getUserId();

      // 1. 检查用户是否免计费
      if (isUserExempt(userId)) {
        log.debug("用户 {} 免计费，跳过记录", userId);
        return;
      }

      // 2. 解析Token数量
      TokenUsage tokenUsage = parseTokenUsage(integration.getIntegrationCode(), responseBody);

      // 3. 获取定价配置
      String modelName = getModelName(integration);
      AiPricingConfig pricing = getPricing(integration.getIntegrationCode(), modelName);

      // 4. 计算费用
      BigDecimal totalCost = calculateCost(tokenUsage, pricing);
      int chargeRatio = getChargeRatio(userId);
      BigDecimal userCost = calculateUserCost(totalCost, chargeRatio);

      // 5. 创建使用记录
      AiUsageLog usageLog =
          AiUsageLog.builder()
              .userId(userId)
              .userName(loginUser.getRealName())
              .departmentId(loginUser.getDepartmentId())
              .departmentName(null) // 可从缓存或数据库获取
              .integrationId(integration.getId())
              .integrationCode(integration.getIntegrationCode())
              .integrationName(integration.getIntegrationName())
              .modelName(modelName)
              .requestType(requestType != null ? requestType : AiUsageLog.RequestType.GENERAL)
              .businessType(businessType)
              .businessId(businessId)
              .promptTokens(tokenUsage.getPromptTokens())
              .completionTokens(tokenUsage.getCompletionTokens())
              .totalTokens(tokenUsage.getTotalTokens())
              .promptPrice(pricing != null ? pricing.getPromptPrice() : BigDecimal.ZERO)
              .completionPrice(pricing != null ? pricing.getCompletionPrice() : BigDecimal.ZERO)
              .totalCost(totalCost)
              .userCost(userCost)
              .chargeRatio(chargeRatio)
              .success(success)
              .errorMessage(sanitizeErrorMessage(errorMessage))
              .durationMs((int) durationMs)
              .build();

      usageLogRepository.save(usageLog);

      // 6. 更新用户配额
      updateUserQuota(userId, tokenUsage.getTotalTokens(), userCost);

      log.info(
          "AI使用记录已保存: user={}, model={}, tokens={}, cost={}元(用户承担{}元)",
          userId,
          integration.getIntegrationCode(),
          tokenUsage.totalTokens,
          totalCost,
          userCost);

    } catch (Exception e) {
      log.error("记录AI使用量失败", e);
      // 不抛出异常，避免影响主流程
    }
  }

  /**
   * 简化版记录方法（向后兼容）
   *
   * @param integration AI集成配置
   * @param responseBody API响应体
   * @param durationMs 响应时间
   * @param success 是否成功
   * @param errorMessage 错误信息
   */
  public void recordUsage(
      final ExternalIntegration integration,
      final String responseBody,
      final long durationMs,
      final boolean success,
      final String errorMessage) {
    recordUsage(
        integration,
        AiUsageLog.RequestType.GENERAL,
        null,
        null,
        responseBody,
        durationMs,
        success,
        errorMessage);
  }

  /**
   * 安全获取当前登录用户
   *
   * @return 登录用户，如果未登录返回null
   */
  private LoginUser getLoginUserSafely() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
        return null;
      }
      Object principal = authentication.getPrincipal();
      if (principal instanceof LoginUser) {
        return (LoginUser) principal;
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 检查用户是否免计费
   *
   * @param userId 用户ID
   * @return 是否免计费
   */
  private boolean isUserExempt(final Long userId) {
    if (!billingEnabled) {
      return true; // 计费未启用，所有用户免费
    }
    try {
      AiUserQuota quota = quotaRepository.findByUserId(userId);
      return quota != null && Boolean.TRUE.equals(quota.getExemptBilling());
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 解析Token使用量 不同模型的响应格式不同，需要适配
   *
   * @param integrationCode 集成代码
   * @param responseBody 响应体
   * @return Token使用量
   */
  private TokenUsage parseTokenUsage(final String integrationCode, final String responseBody) {
    TokenUsage usage = new TokenUsage();

    if (responseBody == null || responseBody.isEmpty()) {
      return usage;
    }

    try {
      JsonNode json = objectMapper.readTree(responseBody);

      // OpenAI 风格（DeepSeek、Moonshot、vLLM等兼容）
      JsonNode usageNode = json.path("usage");
      if (!usageNode.isMissingNode()) {
        usage.setPromptTokens(usageNode.path("prompt_tokens").asInt(0));
        usage.setCompletionTokens(usageNode.path("completion_tokens").asInt(0));
        usage.setTotalTokens(
            usageNode
                .path("total_tokens")
                .asInt(usage.getPromptTokens() + usage.getCompletionTokens()));
        return usage;
      }

      // 通义千问格式
      if (json.has("usage")) {
        JsonNode qwenUsage = json.get("usage");
        if (qwenUsage.has("input_tokens")) {
          usage.setPromptTokens(qwenUsage.path("input_tokens").asInt(0));
          usage.setCompletionTokens(qwenUsage.path("output_tokens").asInt(0));
          usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
          return usage;
        }
      }

      // Claude格式
      if (json.has("usage")) {
        JsonNode claudeUsage = json.get("usage");
        if (claudeUsage.has("input_tokens")) {
          usage.setPromptTokens(claudeUsage.path("input_tokens").asInt(0));
          usage.setCompletionTokens(claudeUsage.path("output_tokens").asInt(0));
          usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
          return usage;
        }
      }

      // 如果解析失败，估算Token数（按字符数粗略估计）
      String content = extractContent(json);
      if (content != null && !content.isEmpty()) {
        // 中文约1.5字符=1Token，英文约4字符=1Token，取中间值约2字符=1Token
        usage.setTotalTokens(content.length() / CHAR_TO_TOKEN_RATIO);
        usage.setCompletionTokens(usage.getTotalTokens());
      }

    } catch (Exception e) {
      log.warn("解析Token使用量失败: {}", e.getMessage());
    }

    return usage;
  }

  /**
   * 从响应中提取内容（用于估算Token）
   *
   * @param json JSON节点
   * @return 提取的内容
   */
  private String extractContent(final JsonNode json) {
    // OpenAI格式
    JsonNode choices = json.path("choices");
    if (choices.isArray() && !choices.isEmpty()) {
      return choices.get(0).path("message").path("content").asText(null);
    }
    // Claude格式
    JsonNode content = json.path("content");
    if (content.isArray() && !content.isEmpty()) {
      return content.get(0).path("text").asText(null);
    }
    // 其他格式
    if (json.has("result")) {
      return json.path("result").asText(null);
    }
    if (json.has("answer")) {
      return json.path("answer").asText(null);
    }
    if (json.has("response")) {
      return json.path("response").asText(null);
    }
    return null;
  }

  /**
   * 获取定价配置
   *
   * @param integrationCode 集成代码
   * @param modelName 模型名称
   * @return 定价配置
   */
  private AiPricingConfig getPricing(final String integrationCode, final String modelName) {
    try {
      // 先精确匹配模型
      AiPricingConfig config = pricingRepository.findByCodeAndModel(integrationCode, modelName);
      if (config != null) {
        return config;
      }
      // 再匹配该集成的默认配置
      return pricingRepository.findByCodeAndModel(integrationCode, null);
    } catch (Exception e) {
      log.warn("获取定价配置失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 计算费用
   *
   * @param usage Token使用量
   * @param pricing 定价配置
   * @return 总费用
   */
  private BigDecimal calculateCost(final TokenUsage usage, final AiPricingConfig pricing) {
    if (pricing == null || pricing.isFree()) {
      return BigDecimal.ZERO;
    }

    if (pricing.isTokenBased()) {
      // 按Token计费（单价是每千Token）
      BigDecimal promptCost =
          pricing
              .getPromptPrice()
              .multiply(BigDecimal.valueOf(usage.promptTokens))
              .divide(BigDecimal.valueOf(TOKEN_DIVISOR), 6, RoundingMode.HALF_UP);

      BigDecimal completionCost =
          pricing
              .getCompletionPrice()
              .multiply(BigDecimal.valueOf(usage.completionTokens))
              .divide(BigDecimal.valueOf(TOKEN_DIVISOR), 6, RoundingMode.HALF_UP);

      return promptCost.add(completionCost).setScale(4, RoundingMode.HALF_UP);
    } else {
      // 按次计费
      return pricing.getPerCallPrice() != null ? pricing.getPerCallPrice() : BigDecimal.ZERO;
    }
  }

  /**
   * 获取收费比例
   *
   * @param userId 用户ID
   * @return 收费比例
   */
  private int getChargeRatio(final Long userId) {
    try {
      // 1. 检查用户是否有自定义比例
      AiUserQuota quota = quotaRepository.findByUserId(userId);
      if (quota != null && quota.getCustomChargeRatio() != null) {
        return quota.getCustomChargeRatio();
      }
    } catch (Exception e) {
      // 忽略
    }

    // 2. 使用默认配置
    return defaultChargeRatio;
  }

  /**
   * 计算用户应付费用 公式：用户费用 = 总费用 × 收费比例%
   *
   * @param totalCost 总费用
   * @param chargeRatio 收费比例
   * @return 用户应付费用
   */
  private BigDecimal calculateUserCost(final BigDecimal totalCost, final int chargeRatio) {
    if (chargeRatio <= 0) {
      return BigDecimal.ZERO;
    }
    if (chargeRatio >= PERCENTAGE_DIVISOR) {
      return totalCost;
    }

    return totalCost
        .multiply(BigDecimal.valueOf(chargeRatio))
        .divide(BigDecimal.valueOf(PERCENTAGE_DIVISOR), 4, RoundingMode.HALF_UP);
  }

  /**
   * 更新用户配额
   *
   * @param userId 用户ID
   * @param tokens Token数量
   * @param cost 费用
   */
  private void updateUserQuota(final Long userId, final int tokens, final BigDecimal cost) {
    try {
      AiUserQuota quota = quotaRepository.findByUserId(userId);
      LocalDate today = LocalDate.now();

      if (quota == null) {
        // 创建配额记录
        quota =
            AiUserQuota.builder()
                .userId(userId)
                .currentMonthTokens((long) tokens)
                .currentMonthCost(cost)
                .quotaResetDate(today.withDayOfMonth(1))
                .exemptBilling(false)
                .build();
        quotaRepository.save(quota);
      } else {
        // 检查是否需要重置（新月份）
        if (quota.needsReset(today)) {
          quota.resetMonthlyUsage(today);
        }

        // 累加使用量
        quota.addUsage(tokens, cost);
        quotaRepository.update(quota);
      }
    } catch (Exception e) {
      log.warn("更新用户配额失败: {}", e.getMessage());
    }
  }

  /**
   * 从集成配置获取模型名称
   *
   * @param integration 集成配置
   * @return 模型名称
   */
  private String getModelName(final ExternalIntegration integration) {
    if (integration.getExtraConfig() != null) {
      Object model = integration.getExtraConfig().get("model");
      return model != null ? model.toString() : null;
    }
    return null;
  }

  /**
   * 脱敏并裁剪错误信息
   *
   * @param errorMessage 错误信息
   * @return 脱敏后的错误信息
   */
  private String sanitizeErrorMessage(final String errorMessage) {
    if (errorMessage == null) {
      return null;
    }
    // 移除可能的敏感信息（API Key等）
    String sanitized =
        errorMessage
            .replaceAll(
                "(?i)(api[_-]?key|token|secret|password)[\"']?\\s*[:=]\\s*[\"']?[\\w-]+[\"']?",
                "$1=***")
            .replaceAll("Bearer\\s+[\\w-]+", "Bearer ***");

    // 裁剪至500字符
    if (sanitized.length() > TRUNCATE_LENGTH) {
      sanitized = sanitized.substring(0, TRUNCATED_LENGTH) + "...";
    }
    return sanitized;
  }

  /** Token使用量内部类. */
  private static class TokenUsage {
    /** 提示词Token数 */
    private int promptTokens = 0;

    /** 完成Token数 */
    private int completionTokens = 0;

    /** 总Token数 */
    private int totalTokens = 0;

    /**
     * 获取提示词Token数.
     *
     * @return 提示词Token数
     */
    public int getPromptTokens() {
      return promptTokens;
    }

    /**
     * 设置提示词Token数.
     *
     * @param promptTokens 提示词Token数
     */
    public void setPromptTokens(final int promptTokens) {
      this.promptTokens = promptTokens;
    }

    /**
     * 获取完成Token数.
     *
     * @return 完成Token数
     */
    public int getCompletionTokens() {
      return completionTokens;
    }

    /**
     * 设置完成Token数.
     *
     * @param completionTokens 完成Token数
     */
    public void setCompletionTokens(final int completionTokens) {
      this.completionTokens = completionTokens;
    }

    /**
     * 获取总Token数.
     *
     * @return 总Token数
     */
    public int getTotalTokens() {
      return totalTokens;
    }

    /**
     * 设置总Token数.
     *
     * @param totalTokens 总Token数
     */
    public void setTotalTokens(final int totalTokens) {
      this.totalTokens = totalTokens;
    }
  }
}
