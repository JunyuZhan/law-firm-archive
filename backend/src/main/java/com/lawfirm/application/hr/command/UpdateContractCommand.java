package com.lawfirm.application.hr.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新劳动合同命令 */
@Data
public class UpdateContractCommand {

  /** 合同编号 */
  private String contractNo;

  /** 合同类型 */
  private String contractType;

  /** 开始日期 */
  private LocalDate startDate;

  /** 结束日期 */
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

  /** 状态 */
  private String status;

  /** 签约日期 */
  private LocalDate signDate;

  /** 到期日期 */
  private LocalDate expireDate;

  /** 合同文件URL */
  private String contractFileUrl;

  /** 备注 */
  private String remark;
}
