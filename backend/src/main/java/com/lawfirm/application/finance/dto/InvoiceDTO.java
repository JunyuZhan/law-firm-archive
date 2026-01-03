package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 发票 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvoiceDTO extends BaseDTO {

    private String invoiceNo;
    private Long feeId;
    private Long contractId;
    private Long clientId;
    private String clientName;
    private String invoiceType;
    private String invoiceTypeName;
    private String title;
    private String taxNo;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private String content;
    private LocalDate invoiceDate;
    private String status;
    private String statusName;
    private Long applicantId;
    private String applicantName;
    private Long issuerId;
    private String issuerName;
    private String fileUrl;
    private String remark;
}

