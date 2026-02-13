package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 离职申请 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResignationDTO extends BaseDTO {

  /** 员工ID */
  private Long employeeId;

  /** 用户ID */
  private Long userId;

  /** 员工姓名 */
  private String employeeName;

  /** 申请编号 */
  private String applicationNo;

  /** 离职类型 */
  private String resignationType;

  /** 离职类型名称 */
  private String resignationTypeName;

  /** 离职日期 */
  private LocalDate resignationDate;

  /** 最后工作日 */
  private LocalDate lastWorkDate;

  /** 离职原因 */
  private String reason;

  /** 交接人ID */
  private Long handoverPersonId;

  /** 交接人姓名 */
  private String handoverPersonName;

  /** 交接状态 */
  private String handoverStatus;

  /** 交接状态名称 */
  private String handoverStatusName;

  /** 交接说明 */
  private String handoverNote;

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
