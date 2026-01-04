package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建合同命令
 */
@Data
public class CreateContractCommand {

    @NotBlank(message = "合同名称不能为空")
    private String name;

    @NotBlank(message = "合同类型不能为空")
    private String contractType;

    @NotNull(message = "客户不能为空")
    private Long clientId;

    private Long matterId;

    @NotBlank(message = "收费方式不能为空")
    private String feeType;

    @NotNull(message = "合同金额不能为空")
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
     * 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政等
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
     * 预支差旅费
     */
    private BigDecimal advanceTravelFee;
    
    /**
     * 风险代理比例（0-100）
     */
    private BigDecimal riskRatio;
}

