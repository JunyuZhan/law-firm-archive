package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 工资明细实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_payroll_item")
public class PayrollItem extends BaseEntity {

  /** 工资表ID */
  private Long payrollSheetId;

  /** 员工ID（关联hr_employee.id） */
  private Long employeeId;

  /** 用户ID（关联sys_user.id，冗余字段便于查询） */
  private Long userId;

  /** 工号（冗余字段） */
  private String employeeNo;

  /** 员工姓名（冗余字段） */
  private String employeeName;

  /** 应发工资（基本工资+提成+绩效+其他） */
  private BigDecimal grossAmount;

  /** 扣减总额（税+社保+公积金+其他） */
  private BigDecimal deductionAmount;

  /** 实发工资（应发-扣减） */
  private BigDecimal netAmount;

  /** 确认状态：PENDING-待确认, CONFIRMED-已确认, REJECTED-已拒绝 */
  private String confirmStatus;

  /** 确认时间 */
  private LocalDateTime confirmedAt;

  /** 确认意见（拒绝时填写原因） */
  private String confirmComment;

  /** 确认截止时间（超过此时间未确认将自动确认） 如果为空，使用工资表的autoConfirmDeadline */
  private LocalDateTime confirmDeadline;
}
