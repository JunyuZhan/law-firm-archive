package com.lawfirm.application.workbench.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 统计数据 DTO. */
@Data
public class StatisticsDTO {
  /** 收入统计. */
  @Data
  public static class RevenueStats {
    /** 总收入. */
    private BigDecimal totalRevenue;

    /** 本月收入. */
    private BigDecimal monthlyRevenue;

    /** 本年收入. */
    private BigDecimal yearlyRevenue;

    /** 待收金额. */
    private BigDecimal pendingRevenue;

    /** 增长率. */
    private BigDecimal growthRate;

    /** 收入趋势. */
    private List<RevenueTrend> trends;
  }

  /** 收入趋势. */
  @Data
  public static class RevenueTrend {
    /** 时间段（如 2026-01）. */
    private String period;

    /** 金额. */
    private BigDecimal amount;
  }

  /** 项目统计. */
  @Data
  public static class MatterStats {
    /** 总案件数. */
    private Long totalMatters;

    /** 进行中案件. */
    private Long activeMatters;

    /** 已完成案件. */
    private Long completedMatters;

    /** 各状态案件数. */
    private Map<String, Long> statusCount;

    /** 各类型案件数. */
    private Map<String, Long> typeCount;
  }

  /** 客户统计. */
  @Data
  public static class ClientStats {
    /** 总客户数. */
    private Long totalClients;

    /** 正式客户. */
    private Long formalClients;

    /** 潜在客户. */
    private Long potentialClients;

    /** 本月新增. */
    private Long newClientsThisMonth;

    /** 各类型客户数. */
    private Map<String, Long> typeCount;
  }

  /** 律师业绩统计. */
  @Data
  public static class LawyerPerformance {
    /** 律师ID. */
    private Long lawyerId;

    /** 律师姓名. */
    private String lawyerName;

    /** 案件数. */
    private Long matterCount;

    /** 收入. */
    private BigDecimal revenue;

    /** 提成. */
    private BigDecimal commission;

    /** 工时. */
    private Double hours;

    /** 排名. */
    private Integer rank;
  }
}
