package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工资明细 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollItemDTO extends BaseDTO {

  /** 工资表ID */
  private Long payrollSheetId;

  /** 员工ID */
  private Long employeeId;

  /** 用户ID */
  private Long userId;

  /** 工号 */
  private String employeeNo;

  /** 员工姓名 */
  private String employeeName;

  /** 应发金额 */
  private BigDecimal grossAmount;

  /** 扣除金额 */
  private BigDecimal deductionAmount;

  /** 实发金额 */
  private BigDecimal netAmount;

  /** 确认状态 */
  private String confirmStatus;

  /** 确认状态名称 */
  private String confirmStatusName;

  /** 确认时间 */
  private LocalDateTime confirmedAt;

  /** 确认意见 */
  private String confirmComment;

  /** 确认截止时间 */
  private LocalDateTime confirmDeadline;

  /** 收入项列表 */
  private List<PayrollIncomeDTO> incomes;

  /** 扣减项列表 */
  private List<PayrollDeductionDTO> deductions;
}
