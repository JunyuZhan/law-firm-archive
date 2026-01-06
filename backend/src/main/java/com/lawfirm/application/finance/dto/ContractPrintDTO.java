package com.lawfirm.application.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同打印数据 DTO
 * 用于打印合同和收案审批表
 */
@Data
public class ContractPrintDTO {

    // ========== 基本信息 ==========
    private Long id;
    private String contractNo;
    private String name;
    private String contractType;
    private String contractTypeName;
    private String status;
    private String statusName;
    
    // ========== 委托人信息 ==========
    private Long clientId;
    private String clientName;
    private String clientType;        // INDIVIDUAL-个人, COMPANY-公司
    private String clientTypeName;
    private String clientAddress;     // 委托人地址
    private String clientPhone;       // 委托人电话
    private String clientIdNumber;    // 身份证号/统一社会信用代码
    
    // ========== 律所信息 ==========
    private String firmName;
    private String firmAddress;
    private String firmPhone;
    private String firmLegalRep;
    
    // ========== 案件信息 ==========
    private String caseType;
    private String caseTypeName;
    private String causeOfAction;
    private String causeOfActionName;
    private String trialStage;
    private String trialStageName;
    private String opposingParty;       // 关联当事人/对方当事人
    private String jurisdictionCourt;   // 办案单位/管辖法院
    private BigDecimal claimAmount;     // 标的金额
    private String description;         // 案情摘要
    
    // ========== 费用信息 ==========
    private String feeType;
    private String feeTypeName;
    private BigDecimal totalAmount;     // 代理/辩护费
    
    // ========== 时间信息 ==========
    private LocalDate signDate;         // 委托时间
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    
    // ========== 人员信息 ==========
    private Long signerId;
    private String signerName;          // 签约律师
    private String leadLawyerName;      // 主办律师
    private String assistLawyerNames;   // 协办律师
    private String originatorName;      // 案源人/接待人
    
    // ========== 利冲信息 ==========
    private String conflictCheckStatus;
    private String conflictCheckStatusName;
    private String conflictCheckResult; // 有无利益冲突
    
    // ========== 审批信息 ==========
    private List<ApprovalInfo> approvals;
    
    // ========== 模板内容（变量替换后）==========
    private String contractContent;
    
    /**
     * 审批信息
     */
    @Data
    public static class ApprovalInfo {
        private String approverName;    // 审批人姓名
        private String approverRole;    // 审批人角色（接待律师/律所领导）
        private String status;          // 审批状态
        private String statusName;
        private String comment;         // 审批意见
        private LocalDateTime approvedAt; // 审批时间
    }
}

