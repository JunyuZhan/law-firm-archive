package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 离职申请实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_resignation")
public class Resignation extends BaseEntity {

  /** 员工ID */
  private Long employeeId;

  /** 申请编号 */
  private String applicationNo;

  /** 离职类型：VOLUNTARY-主动离职, DISMISSED-辞退, RETIREMENT-退休, CONTRACT_EXPIRED-合同到期 */
  private String resignationType;

  /** 申请日期 */
  private LocalDate resignationDate;

  /** 最后工作日 */
  private LocalDate lastWorkDate;

  /** 离职原因 */
  private String reason;

  /** 交接人ID */
  private Long handoverPersonId;

  /** 交接状态：PENDING-待交接, IN_PROGRESS-交接中, COMPLETED-已完成 */
  private String handoverStatus;

  /** 交接说明 */
  private String handoverNote;

  /** 状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, COMPLETED-已完成 */
  private String status;

  /** 审批人ID */
  private Long approverId;

  /** 审批日期 */
  private LocalDate approvedDate;

  /** 审批意见 */
  private String comment;
}
