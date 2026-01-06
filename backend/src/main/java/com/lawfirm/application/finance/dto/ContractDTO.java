package com.lawfirm.application.finance.dto;

import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 合同 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractDTO extends BaseDTO {

    private String contractNo;
    private String name;
    private String contractType;
    private String contractTypeName;
    private Long clientId;
    private String clientName;
    private Long matterId;
    private String matterName;
    private String feeType;
    private String feeTypeName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private String currency;
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String status;
    private String statusName;
    private Long signerId;
    private String signerName;
    private Long departmentId;
    private String departmentName;
    private Long createdBy;  // 创建人ID，用于前端判断操作权限
    private String paymentTerms;
    private String fileUrl;
    private String remark;
    
    // ========== 扩展字段（合同模块完善）==========
    
    /**
     * 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政
     */
    private String caseType;
    private String caseTypeName;
    
    /**
     * 案由代码
     */
    private String causeOfAction;
    
    /**
     * 案由名称
     */
    private String causeOfActionName;
    
    /**
     * 审理阶段
     */
    private String trialStage;
    private String trialStageName;
    
    /**
     * 标的金额
     */
    private BigDecimal claimAmount;
    
    /**
     * 管辖法院
     */
    private String jurisdictionCourt;
    
    /**
     * 对方当事人
     */
    private String opposingParty;
    
    /**
     * 利冲审查状态
     */
    private String conflictCheckStatus;
    private String conflictCheckStatusName;
    
    /**
     * 归档状态
     */
    private String archiveStatus;
    private String archiveStatusName;
    
    /**
     * 预支差旅费
     */
    private BigDecimal advanceTravelFee;
    
    /**
     * 风险代理比例
     */
    private BigDecimal riskRatio;
    
    /**
     * 印章使用记录
     */
    private String sealRecord;
    
    /**
     * 付款计划列表
     */
    private List<ContractPaymentScheduleDTO> paymentSchedules;
    
    /**
     * 参与人列表
     */
    private List<ContractParticipantDTO> participants;
    
    /**
     * 关联的审批单列表（最新的在前）
     */
    private List<ApprovalDTO> approvals;
    
    /**
     * 当前待审批的审批单（如果有）
     */
    private ApprovalDTO currentApproval;
    
    // ========== 提成分配方案 ==========
    
    /**
     * 提成规则ID
     */
    private Long commissionRuleId;
    
    /**
     * 律所比例(%)
     */
    private BigDecimal firmRate;
    
    /**
     * 主办律师比例(%)
     */
    private BigDecimal leadLawyerRate;
    
    /**
     * 协办律师比例(%)
     */
    private BigDecimal assistLawyerRate;
    
    /**
     * 辅助人员比例(%)
     */
    private BigDecimal supportStaffRate;
    
    /**
     * 案源人比例(%)
     */
    private BigDecimal originatorRate;

    /**
     * 案情摘要（用于审批表）
     */
    private String caseSummary;
}

