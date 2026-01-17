package com.lawfirm.domain.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI使用记录实体
 * 记录每次AI调用的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLog {

    private Long id;

    // 用户信息
    private Long userId;
    private String userName;
    private Long departmentId;
    private String departmentName;

    // AI模型信息
    private Long integrationId;
    private String integrationCode;
    private String integrationName;
    private String modelName;

    // 调用信息
    private String requestType;      // DOCUMENT_GENERATE/CHAT/SUMMARY/ANALYSIS
    private String businessType;     // MATTER/PERSONAL
    private Long businessId;

    // Token统计
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    // 费用信息（单价：元/千Token，费用：元）
    private BigDecimal promptPrice;
    private BigDecimal completionPrice;
    private BigDecimal totalCost;
    private BigDecimal userCost;
    private Integer chargeRatio;     // 收费比例快照（百分比）

    // 调用结果
    private Boolean success;
    private String errorMessage;
    private Integer durationMs;

    // 元数据
    private LocalDateTime createdAt;

    /**
     * 请求类型枚举
     */
    public static class RequestType {
        public static final String DOCUMENT_GENERATE = "DOCUMENT_GENERATE";
        public static final String CHAT = "CHAT";
        public static final String SUMMARY = "SUMMARY";
        public static final String ANALYSIS = "ANALYSIS";
        public static final String GENERAL = "GENERAL";
    }

    /**
     * 业务类型枚举
     */
    public static class BusinessType {
        public static final String MATTER = "MATTER";
        public static final String PERSONAL = "PERSONAL";
    }
}
