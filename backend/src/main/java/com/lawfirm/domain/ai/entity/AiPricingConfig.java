package com.lawfirm.domain.ai.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI模型定价配置实体 管理员可配置不同模型的单价 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPricingConfig {

  /** 主键ID */
  private Long id;

  /** 集成编码（如AI_DEEPSEEK） */
  private String integrationCode;

  /** 模型名称（可为空，表示该集成的默认定价） */
  private String modelName;

  /** 输入Token单价（元/千Token） */
  private BigDecimal promptPrice;

  /** 输出Token单价（元/千Token） */
  private BigDecimal completionPrice;

  /** 每次调用固定费用（可选） */
  private BigDecimal perCallPrice;

  /** 计费模式：TOKEN/PER_CALL */
  private String pricingMode;

  /** 是否启用 */
  private Boolean enabled;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;

  /** 创建人ID */
  private Long createdBy;

  /** 更新人ID */
  private Long updatedBy;

  /** 删除标记 */
  private Boolean deleted;

  /** 计费模式枚举 */
  public static class PricingMode {
    /** Token计费模式 */
    public static final String TOKEN = "TOKEN";

    /** 按次计费模式 */
    public static final String PER_CALL = "PER_CALL";
  }

  /**
   * 判断是否按Token计费。
   *
   * @return 如果按Token计费返回true，否则返回false
   */
  public boolean isTokenBased() {
    return PricingMode.TOKEN.equals(pricingMode) || pricingMode == null;
  }

  /**
   * 判断是否免费。
   *
   * @return 如果免费返回true，否则返回false
   */
  public boolean isFree() {
    if (isTokenBased()) {
      return (promptPrice == null || promptPrice.compareTo(BigDecimal.ZERO) == 0)
          && (completionPrice == null || completionPrice.compareTo(BigDecimal.ZERO) == 0);
    } else {
      return perCallPrice == null || perCallPrice.compareTo(BigDecimal.ZERO) == 0;
    }
  }
}
