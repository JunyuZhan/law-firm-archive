package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款记录 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentDTO extends BaseDTO {

    private String paymentNo;
    private Long feeId;
    private Long contractId;
    private Long matterId;
    private Long clientId;
    private String clientName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentMethodName;
    private LocalDate paymentDate;
    private String bankAccount;
    private String transactionNo;
    private String status;
    private String statusName;
    private Long confirmerId;
    private String confirmerName;
    private String remark;
}

