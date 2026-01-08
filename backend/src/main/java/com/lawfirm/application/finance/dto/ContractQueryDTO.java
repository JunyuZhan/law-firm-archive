package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合同查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractQueryDTO extends PageQuery {

    /**
     * 合同编号（模糊）
     */
    private String contractNo;

    /**
     * 合同名称（模糊）
     */
    private String name;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 合同类型
     */
    private String contractType;

    /**
     * 状态
     */
    private String status;

    /**
     * 签约开始日期
     */
    private LocalDate signDateFrom;

    /**
     * 签约结束日期
     */
    private LocalDate signDateTo;

    /**
     * 签约人ID
     */
    private Long signerId;
    
    /**
     * 收费方式
     */
    private String feeType;
    
    /**
     * 部门ID
     */
    private Long departmentId;
    
    /**
     * 生效日期开始
     */
    private LocalDate effectiveDateFrom;
    
    /**
     * 生效日期结束
     */
    private LocalDate effectiveDateTo;
    
    /**
     * 到期日期开始
     */
    private LocalDate expiryDateFrom;
    
    /**
     * 到期日期结束
     */
    private LocalDate expiryDateTo;
    
    /**
     * 合同金额最小值
     */
    private BigDecimal amountMin;
    
    /**
     * 合同金额最大值
     */
    private BigDecimal amountMax;
    
    /**
     * 标的金额最小值
     */
    private BigDecimal claimAmountMin;
    
    /**
     * 标的金额最大值
     */
    private BigDecimal claimAmountMax;
    
    /**
     * 审理阶段
     */
    private String trialStage;
    
    /**
     * 利冲审查状态
     */
    private String conflictCheckStatus;
    
    /**
     * 归档状态
     */
    private String archiveStatus;
    
    /**
     * 创建时间开始
     */
    private LocalDateTime createdAtFrom;
    
    /**
     * 创建时间结束
     */
    private LocalDateTime createdAtTo;
}

