package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 劳动合同 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractDTO extends BaseDTO {

  /** 员工ID */
  private Long employeeId;

  /** 用户ID */
  private Long userId;

  /** 员工姓名 */
  private String employeeName;

  /** 合同编号 */
  private String contractNo;

  /** 合同类型 */
  private String contractType;

  /** 合同类型名称 */
  private String contractTypeName;

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

  /** 状态名称 */
  private String statusName;

  /** 签约日期 */
  private LocalDate signDate;

  /** 到期日期 */
  private LocalDate expireDate;

  /** 续签次数 */
  private Integer renewCount;

  /** 合同文件URL */
  private String contractFileUrl;

  /** 备注 */
  private String remark;
}
