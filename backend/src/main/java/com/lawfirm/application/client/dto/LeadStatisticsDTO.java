package com.lawfirm.application.client.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** 案源统计DTO（M2-033~M2-034） */
@Data
public class LeadStatisticsDTO {

  /** 总案源数 */
  private Long totalLeads;

  /** 已转化案源数 */
  private Long convertedLeads;

  /** 转化率（百分比） */
  private Double conversionRate;

  /** 按来源渠道统计（M2-033） */
  private List<Map<String, Object>> bySourceChannel;

  /** 按状态统计 */
  private List<Map<String, Object>> byStatus;

  /** 按案源人统计 */
  private List<Map<String, Object>> byOriginator;

  /** 转化率分析（M2-034） */
  private List<Map<String, Object>> conversionAnalysis;

  /** 按时间统计转化趋势 */
  private List<Map<String, Object>> conversionTrend;
}
