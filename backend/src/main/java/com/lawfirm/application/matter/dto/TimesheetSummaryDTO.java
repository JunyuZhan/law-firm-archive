package com.lawfirm.application.matter.dto;

import java.math.BigDecimal;
import lombok.Data;

/** 工时汇总DTO. */
@Data
public class TimesheetSummaryDTO {

  /** 用户ID. */
  private Long userId;

  /** 用户姓名. */
  private String userName;

  /** 案件ID. */
  private Long matterId;

  /** 案件名称. */
  private String matterName;

  /** 年份. */
  private Integer year;

  /** 月份. */
  private Integer month;

  /** 总工时. */
  private BigDecimal totalHours;

  /** 计费工时. */
  private BigDecimal billableHours;

  /** 非计费工时. */
  private BigDecimal nonBillableHours;

  /** 总金额. */
  private BigDecimal totalAmount;

  /** 草稿数. */
  private Integer draftCount;

  /** 已提交数. */
  private Integer submittedCount;

  /** 已批准数. */
  private Integer approvedCount;

  /** 已拒绝数. */
  private Integer rejectedCount;
}
