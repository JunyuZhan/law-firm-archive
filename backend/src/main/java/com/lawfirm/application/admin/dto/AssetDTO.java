package com.lawfirm.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDTO {

    private Long id;
    private String assetNo;
    private String name;
    private String category;
    private String categoryName;
    private String brand;
    private String model;
    private String specification;
    private String serialNumber;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private String supplier;
    private LocalDate warrantyExpireDate;
    private Integer usefulLife;
    private String location;
    private Long currentUserId;
    private String currentUserName;
    private Long departmentId;
    private String departmentName;
    private String status;
    private String statusName;
    private String imageUrl;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 是否在保修期内
     */
    private Boolean inWarranty;
}
