package com.lawfirm.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产操作记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRecordDTO {

    private Long id;
    private Long assetId;
    private String assetNo;
    private String assetName;
    private String recordType;
    private String recordTypeName;
    private Long operatorId;
    private String operatorName;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private LocalDate operateDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private String reason;
    private BigDecimal maintenanceCost;
    private String approvalStatus;
    private Long approverId;
    private String approverName;
    private String approvalComment;
    private String remarks;
    private LocalDateTime createdAt;
}
