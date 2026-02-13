package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建合同命令. */
@Data
public class CreateContractCommand {

  /** 合同名称（可选，为空时自动生成）. */
  private String name;

  /** 合同类型. */
  @NotBlank(message = "合同类型不能为空")
  private String contractType;

  /** 客户ID. */
  @NotNull(message = "客户不能为空")
  private Long clientId;

  /** 案件ID. */
  private Long matterId;

  /** 收费方式. */
  @NotBlank(message = "收费方式不能为空")
  private String feeType;

  /** 合同金额. */
  @NotNull(message = "合同金额不能为空")
  private BigDecimal totalAmount;

  /** 币种. */
  private String currency;

  /** 签约日期. */
  private LocalDate signDate;

  /** 生效日期. */
  private LocalDate effectiveDate;

  /** 到期日期. */
  private LocalDate expiryDate;

  /** 签约人ID. */
  private Long signerId;

  /** 部门ID. */
  private Long departmentId;

  /** 付款条件. */
  private String paymentTerms;

  /** 文件URL. */
  private String fileUrl;

  /** 备注. */
  private String remark;

  // ========== 扩展字段（合同模块完善）==========

  /** 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政等. */
  private String caseType;

  /** 案由代码. */
  private String causeOfAction;

  /** 审理阶段. */
  private String trialStage;

  /** 标的金额. */
  private BigDecimal claimAmount;

  /** 管辖法院. */
  private String jurisdictionCourt;

  /** 对方当事人. */
  private String opposingParty;

  /** 利冲审查状态. */
  private String conflictCheckStatus;

  /** 预支差旅费. */
  private BigDecimal advanceTravelFee;

  /** 风险代理比例（0-100）. */
  private BigDecimal riskRatio;

  // ========== 提成分配方案 ==========

  /** 提成规则ID（选择的预设方案）. */
  private Long commissionRuleId;

  /** 律所比例（%）. */
  private BigDecimal firmRate;

  /** 主办律师比例（%）. */
  private BigDecimal leadLawyerRate;

  /** 协办律师比例（%）. */
  private BigDecimal assistLawyerRate;

  /** 辅助人员比例（%）. */
  private BigDecimal supportStaffRate;

  /** 案源人比例（%）. */
  private BigDecimal originatorRate;

  /** 案情摘要（用于审批表）. */
  private String caseSummary;

  // ========== 模板变量字段（用于生成合同文本）==========

  /** 主办律师姓名（多人用顿号分隔）. */
  private String lawyerNames;

  /** 律师助理姓名. */
  private String assistantNames;

  /** 代理权限类型：一般代理/特别代理. */
  private String authorizationType;

  /** 付款期限描述. */
  private String paymentDeadline;

  /** 争议解决方式：1-法院管辖，2-仲裁. */
  private String disputeResolution;

  /** 仲裁委员会名称. */
  private String arbitrationCommittee;

  /** 特别约定. */
  private String specialTerms;

  /** 被告人/犯罪嫌疑人姓名（刑事案件）. */
  private String defendantName;

  /** 涉嫌罪名（刑事案件）. */
  private String criminalCharge;

  /** 辩护阶段（刑事案件）. */
  private String defenseStage;

  /** 合伙人收费标准（计时收费）. */
  private BigDecimal partnerRate;

  /** 资深律师收费标准（计时收费）. */
  private BigDecimal seniorRate;

  /** 助理收费标准（计时收费）. */
  private BigDecimal assistantRate;

  /** 服务小时数（顾问合同）. */
  private Integer serviceHours;

  /** 合同金额大写（前端传入或后端自动计算）. */
  private String totalAmountCN;

  /** 标的金额大写（前端传入或后端自动计算）. */
  private String claimAmountChinese;
}
