package com.lawfirm.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购申请DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDTO {

    private Long id;
    private String requestNo;
    private String title;
    private Long applicantId;
    private String applicantName;
    private Long departmentId;
    private String departmentName;
    private String purchaseType;
    private String purchaseTypeName;
    private BigDecimal estimatedAmount;
    private BigDecimal actualAmount;
    private LocalDate expectedDate;
    private String reason;
    private String status;
    private String statusName;
    private Long approverId;
    private String approverName;
    private LocalDate approvalDate;
    private String approvalComment;
    private Long supplierId;
    private String supplierName;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 采购明细列表
     */
    private List<PurchaseItemDTO> items;
}
