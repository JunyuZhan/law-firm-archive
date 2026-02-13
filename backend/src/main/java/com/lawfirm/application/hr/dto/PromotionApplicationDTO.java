package com.lawfirm.application.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 晋升申请 DTO */
@Data
public class PromotionApplicationDTO {
  /** 申请ID */
  private Long id;

  /** 申请编号 */
  private String applicationNo;

  /** 员工ID */
  private Long employeeId;

  /** 员工姓名 */
  private String employeeName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** 当前级别ID */
  private Long currentLevelId;

  /** 当前级别名称 */
  private String currentLevelName;

  /** 目标级别ID */
  private Long targetLevelId;

  /** 目标级别名称 */
  private String targetLevelName;

  /** 申请理由 */
  private String applyReason;

  /** 工作业绩 */
  private String achievements;

  /** 自我评价 */
  private String selfEvaluation;

  /** 附件列表 */
  private List<String> attachments;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 评审分数 */
  private BigDecimal reviewScore;

  /** 评审结果 */
  private String reviewResult;

  /** 评审结果名称 */
  private String reviewResultName;

  /** 评审意见 */
  private String reviewComment;

  /** 审批人ID */
  private Long approvedBy;

  /** 审批人姓名 */
  private String approvedByName;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 审批意见 */
  private String approvalComment;

  /** 生效日期 */
  private LocalDate effectiveDate;

  /** 申请日期 */
  private LocalDate applyDate;

  /** 评审记录列表 */
  private List<PromotionReviewDTO> reviews;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
