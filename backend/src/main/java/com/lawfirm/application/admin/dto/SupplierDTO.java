package com.lawfirm.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 供应商DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDTO {

    private Long id;
    private String supplierNo;
    private String name;
    private String supplierType;
    private String supplierTypeName;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String creditCode;
    private String bankName;
    private String bankAccount;
    private String supplyScope;
    private String rating;
    private String ratingName;
    private String status;
    private String statusName;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
