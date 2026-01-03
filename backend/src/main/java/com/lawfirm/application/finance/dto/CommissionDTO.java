package com.lawfirm.application.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提成记录 DTO
 */
@Data
public class CommissionDTO {

    private Long id;
    private Long paymentId;
    private Long matterId;
    private Long userId;
    private String userName;
    private Long ruleId;
    private BigDecimal grossAmount;
    private BigDecimal taxAmount;
    private BigDecimal managementFee;
    private BigDecimal costAmount;
    private BigDecimal netAmount;
    private BigDecimal distributionRatio;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private String compensationType;
    private String status;
    private String remark;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}

