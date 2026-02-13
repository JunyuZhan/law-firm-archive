package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建劳动合同命令 */
@Data
public class CreateContractCommand {

  /** 员工ID */
  @NotNull(message = "员工ID不能为空")
  private Long employeeId;

  /** 合同编号 */
  private String contractNo;

  /** 合同类型 */
  @NotNull(message = "合同类型不能为空")
  private String contractType;

  /** 合同开始日期 */
  @NotNull(message = "合同开始日期不能为空")
  private LocalDate startDate;

  /** 合同结束日期 */
  private LocalDate endDate;

  /** 试用期月数 */
  private Integer probationMonths;

  /** 试用期结束日期 */
  private LocalDate probationEndDate;

  /** 基本工资 */
  private BigDecimal baseSalary;

  /** 绩效奖金 */
  private BigDecimal performanceBonus;

  /** 其他津贴 */
  private BigDecimal otherAllowance;

  /** 签约日期 */
  private LocalDate signDate;

  /** 合同文件URL */
  private String contractFileUrl;

  /** 备注 */
  private String remark;
}
