package com.lawfirm.application.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 合同打印数据 DTO 用于打印合同和收案审批表 */
@Data
public class ContractPrintDTO {

  // ========== 基本信息 ==========
  /** 合同ID */
  private Long id;

  /** 合同编号 */
  private String contractNo;

  /** 合同名称 */
  private String name;

  /** 合同类型 */
  private String contractType;

  /** 合同类型名称 */
  private String contractTypeName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  // ========== 委托人信息 ==========
  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 客户类型（INDIVIDUAL-个人, COMPANY-公司） */
  private String clientType;

  /** 客户类型名称 */
  private String clientTypeName;

  /** 委托人地址 */
  private String clientAddress;

  /** 委托人电话 */
  private String clientPhone;

  /** 身份证号/统一社会信用代码 */
  private String clientIdNumber;

  // ========== 律所信息 ==========
  /** 律所名称 */
  private String firmName;

  /** 律所地址 */
  private String firmAddress;

  /** 律所电话 */
  private String firmPhone;

  /** 法定代表人 */
  private String firmLegalRep;

  // ========== 案件信息 ==========
  /** 案件类型 */
  private String caseType;

  /** 案件类型名称 */
  private String caseTypeName;

  /** 案由代码 */
  private String causeOfAction;

  /** 案由名称 */
  private String causeOfActionName;

  /** 审理阶段 */
  private String trialStage;

  /** 审理阶段名称 */
  private String trialStageName;

  /** 关联当事人/对方当事人 */
  private String opposingParty;

  /** 办案单位/管辖法院 */
  private String jurisdictionCourt;

  /** 标的金额 */
  private BigDecimal claimAmount;

  /** 案情摘要 */
  private String description;

  // ========== 费用信息 ==========
  /** 收费方式 */
  private String feeType;

  /** 收费方式名称 */
  private String feeTypeName;

  /** 代理/辩护费 */
  private BigDecimal totalAmount;

  // ========== 时间信息 ==========
  /** 委托时间 */
  private LocalDate signDate;

  /** 生效日期 */
  private LocalDate effectiveDate;

  /** 到期日期 */
  private LocalDate expiryDate;

  // ========== 人员信息 ==========
  /** 签约人ID */
  private Long signerId;

  /** 签约律师 */
  private String signerName;

  /** 主办律师 */
  private String leadLawyerName;

  /** 协办律师 */
  private String assistLawyerNames;

  /** 案源人/接待人 */
  private String originatorName;

  // ========== 利冲信息 ==========
  /** 利冲审查状态 */
  private String conflictCheckStatus;

  /** 利冲审查状态名称 */
  private String conflictCheckStatusName;

  /** 有无利益冲突 */
  private String conflictCheckResult;

  // ========== 审批信息 ==========
  /** 审批信息列表 */
  private List<ApprovalInfo> approvals;

  // ========== 模板内容（变量替换后）==========
  /** 合同内容 */
  private String contractContent;

  /** 审批信息 */
  @Data
  public static class ApprovalInfo {
    /** 审批人姓名 */
    private String approverName;

    /** 审批人角色（接待律师/律所领导） */
    private String approverRole;

    /** 审批状态 */
    private String status;

    /** 审批状态名称 */
    private String statusName;

    /** 审批意见 */
    private String comment;

    /** 审批时间 */
    private LocalDateTime approvedAt;
  }
}
