package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新合同命令
 */
@Data
public class UpdateContractCommand {

    @NotNull(message = "合同ID不能为空")
    private Long id;

    private String name;
    private String contractType;
    private Long clientId;
    private Long matterId;
    private String feeType;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private Long signerId;
    private Long departmentId;
    private String paymentTerms;
    private String fileUrl;
    private String remark;
    
    // ========== 扩展字段（合同模块完善）==========
    
    /**
     * 案件类型
     */
    private String caseType;
    
    /**
     * 案由代码
     */
    private String causeOfAction;
    
    /**
     * 审理阶段
     */
    private String trialStage;
    
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
    
    /**
     * 归档状态
     */
    private String archiveStatus;
    
    /**
     * 预支差旅费
     */
    private BigDecimal advanceTravelFee;
    
    /**
     * 风险代理比例（0-100）
     */
    private BigDecimal riskRatio;
    
    /**
     * 印章使用记录
     */
    private String sealRecord;

    // ========== 提成分配方案 ==========

    /**
     * 提成规则ID（选择的预设方案）
     */
    private Long commissionRuleId;

    /**
     * 律所比例（%）
     */
    private BigDecimal firmRate;

    /**
     * 主办律师比例（%）
     */
    private BigDecimal leadLawyerRate;

    /**
     * 协办律师比例（%）
     */
    private BigDecimal assistLawyerRate;

    /**
     * 辅助人员比例（%）
     */
    private BigDecimal supportStaffRate;

    /**
     * 案源人比例（%）
     */
    private BigDecimal originatorRate;

    /**
     * 案情摘要（用于审批表）
     */
    private String caseSummary;
}

