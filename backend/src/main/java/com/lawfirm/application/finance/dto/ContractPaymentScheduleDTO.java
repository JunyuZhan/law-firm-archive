package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同付款计划 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractPaymentScheduleDTO extends BaseDTO {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 阶段名称
     */
    private String phaseName;

    /**
     * 付款金额
     */
    private BigDecimal amount;

    /**
     * 比例（风险代理时使用）
     */
    private BigDecimal percentage;

    /**
     * 计划收款日期
     */
    private LocalDate plannedDate;

    /**
     * 实际收款日期
     */
    private LocalDate actualDate;

    /**
     * 状态
     */
    private String status;
    
    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;
}
