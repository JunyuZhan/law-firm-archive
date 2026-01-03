package com.lawfirm.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购入库DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReceiveDTO {

    private Long id;
    private String receiveNo;
    private Long requestId;
    private String requestNo;
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private LocalDate receiveDate;
    private Long receiverId;
    private String receiverName;
    private String location;
    private Boolean convertToAsset;
    private Long assetId;
    private String assetNo;
    private String remarks;
    private LocalDateTime createdAt;
}
