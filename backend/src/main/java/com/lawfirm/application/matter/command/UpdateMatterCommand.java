package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新案件命令. */
@Data
public class UpdateMatterCommand {

  /** 案件ID. */
  @NotNull(message = "案件ID不能为空")
  private Long id;

  /** 案件名称. */
  private String name;

  /** 案件类型. */
  private String matterType;

  /** 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政等. */
  private String caseType;

  /** 代理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行等. */
  private String litigationStage;

  /** 案由代码. */
  private String causeOfAction;

  /** 业务类型. */
  private String businessType;

  /** 客户ID. */
  private Long clientId;

  /** 对方当事人. */
  private String opposingParty;

  /** 对方律师信息. */
  private String opposingLawyerName;

  /** 对方律师执业证号. */
  private String opposingLawyerLicenseNo;

  /** 对方律师所在律所. */
  private String opposingLawyerFirm;

  /** 对方律师电话. */
  private String opposingLawyerPhone;

  /** 对方律师邮箱. */
  private String opposingLawyerEmail;

  /** 案件描述. */
  private String description;

  /** 发起人ID. */
  private Long originatorId;

  /** 主办律师ID. */
  private Long leadLawyerId;

  /** 部门ID. */
  private Long departmentId;

  /** 费用类型. */
  private String feeType;

  /** 预估费用. */
  private BigDecimal estimatedFee;

  /** 实际费用. */
  private BigDecimal actualFee;

  /** 立案日期. */
  private LocalDate filingDate;

  /** 预计结案日期. */
  private LocalDate expectedClosingDate;

  /** 实际结案日期. */
  private LocalDate actualClosingDate;

  /** 标的额. */
  private BigDecimal claimAmount;

  /** 案件结果. */
  private String outcome;

  /** 合同ID. */
  private Long contractId;

  /** 备注. */
  private String remark;
}
