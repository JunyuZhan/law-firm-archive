package com.lawfirm.application.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用报销 DTO
 */
@Data
public class ExpenseDTO {

    private Long id;
    private String expenseNo;
    private Long matterId;
    private String matterName;
    private Long applicantId;
    private String applicantName;
    private String expenseType;
    private String expenseCategory;
    private LocalDate expenseDate;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String vendorName;
    private String invoiceNo;
    private String invoiceUrl;
    private String status;
    private Long approverId;
    private String approverName;
    private LocalDateTime approvedAt;
    private String approvalComment;
    private LocalDateTime paidAt;
    private Long paidBy;
    private String paidByName;
    private String paymentMethod;
    private Boolean isCostAllocation;
    private Long allocatedToMatterId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

