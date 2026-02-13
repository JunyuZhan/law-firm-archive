package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 转正申请 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegularizationDTO extends BaseDTO {

  /** 员工ID */
  private Long employeeId;

  /** 用户ID */
  private Long userId;

  /** 员工姓名 */
  private String employeeName;

  /** 申请编号 */
  private String applicationNo;

  /** 试用期开始日期 */
  private LocalDate probationStartDate;

  /** 试用期结束日期 */
  private LocalDate probationEndDate;

  /** 申请日期 */
  private LocalDate applicationDate;

  /** 预计转正日期 */
  private LocalDate expectedRegularDate;

  /** 自我评价 */
  private String selfEvaluation;

  /** 主管评价 */
  private String supervisorEvaluation;

  /** HR评价 */
  private String hrEvaluation;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 审批人ID */
  private Long approverId;

  /** 审批人姓名 */
  private String approverName;

  /** 审批日期 */
  private LocalDate approvedDate;

  /** 审批意见 */
  private String comment;
}
