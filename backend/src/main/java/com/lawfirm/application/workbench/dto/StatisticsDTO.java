package com.lawfirm.application.workbench.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计数据 DTO
 */
@Data
public class StatisticsDTO {
    /**
     * 收入统计
     */
    @Data
    public static class RevenueStats {
        private BigDecimal totalRevenue;           // 总收入
        private BigDecimal monthlyRevenue;          // 本月收入
        private BigDecimal yearlyRevenue;          // 本年收入
        private BigDecimal pendingRevenue;         // 待收金额
        private BigDecimal growthRate;             // 增长率
        private List<RevenueTrend> trends;         // 收入趋势
    }

    /**
     * 收入趋势
     */
    @Data
    public static class RevenueTrend {
        private String period;                     // 时间段（如 2026-01）
        private BigDecimal amount;                 // 金额
    }

    /**
     * 项目统计
     */
    @Data
    public static class MatterStats {
        private Long totalMatters;                 // 总案件数
        private Long activeMatters;                // 进行中案件
        private Long completedMatters;             // 已完成案件
        private Map<String, Long> statusCount;    // 各状态案件数
        private Map<String, Long> typeCount;       // 各类型案件数
    }

    /**
     * 客户统计
     */
    @Data
    public static class ClientStats {
        private Long totalClients;                 // 总客户数
        private Long formalClients;                // 正式客户
        private Long potentialClients;             // 潜在客户
        private Long newClientsThisMonth;          // 本月新增
        private Map<String, Long> typeCount;       // 各类型客户数
    }

    /**
     * 律师业绩统计
     */
    @Data
    public static class LawyerPerformance {
        private Long lawyerId;
        private String lawyerName;
        private Long matterCount;                  // 案件数
        private BigDecimal revenue;                // 收入
        private BigDecimal commission;             // 提成
        private Double hours;                      // 工时
        private Integer rank;                      // 排名
    }
}

