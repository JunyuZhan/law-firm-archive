package com.lawfirm.application.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 发票统计DTO（M4-034）
 */
@Data
public class InvoiceStatisticsDTO {

    /**
     * 总开票金额
     */
    private BigDecimal totalAmount;

    /**
     * 本月开票金额
     */
    private BigDecimal monthlyAmount;

    /**
     * 本年开票金额
     */
    private BigDecimal yearlyAmount;

    /**
     * 按客户统计
     */
    private List<Map<String, Object>> byClient;

    /**
     * 按发票类型统计
     */
    private List<Map<String, Object>> byType;

    /**
     * 按状态统计
     */
    private List<Map<String, Object>> byStatus;

    /**
     * 按时间统计（趋势）
     */
    private List<Map<String, Object>> byDate;
}

