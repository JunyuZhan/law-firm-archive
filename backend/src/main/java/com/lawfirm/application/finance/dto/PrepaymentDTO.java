package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预收款DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrepaymentDTO extends BaseDTO {

    private String prepaymentNo;
    private Long clientId;
    private String clientName;
    private Long contractId;
    private String contractNo;
    private Long matterId;
    private String matterNo;
    private String matterName;
    private BigDecimal amount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private String currency;
    private LocalDate receiptDate;
    private String paymentMethod;
    private String paymentMethodName;
    private String bankAccount;
    private String transactionNo;
    private String status;
    private String statusName;
    private Long confirmerId;
    private String confirmerName;
    private LocalDateTime confirmedAt;
    private String purpose;
    private String remark;

    /**
     * 核销记录列表
     */
    private List<PrepaymentUsageDTO> usages;
}
