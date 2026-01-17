package com.lawfirm.domain.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI模型定价配置实体
 * 管理员可配置不同模型的单价
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPricingConfig {

    private Long id;

    /**
     * 集成编码（如AI_DEEPSEEK）
     */
    private String integrationCode;

    /**
     * 模型名称（可为空，表示该集成的默认定价）
     */
    private String modelName;

    /**
     * 输入Token单价（元/千Token）
     */
    private BigDecimal promptPrice;

    /**
     * 输出Token单价（元/千Token）
     */
    private BigDecimal completionPrice;

    /**
     * 每次调用固定费用（可选）
     */
    private BigDecimal perCallPrice;

    /**
     * 计费模式：TOKEN/PER_CALL
     */
    private String pricingMode;

    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean deleted;

    /**
     * 计费模式枚举
     */
    public static class PricingMode {
        public static final String TOKEN = "TOKEN";
        public static final String PER_CALL = "PER_CALL";
    }

    /**
     * 判断是否按Token计费
     */
    public boolean isTokenBased() {
        return PricingMode.TOKEN.equals(pricingMode) || pricingMode == null;
    }

    /**
     * 判断是否免费
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
