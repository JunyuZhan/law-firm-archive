package com.lawfirm.application.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人 DTO
 */
@Data
public class ContactDTO {
    private Long id;
    private Long clientId;
    private String contactName;
    private String position;
    private String department;
    private String mobilePhone;
    private String officePhone;
    private String email;
    private String wechat;
    private Boolean isPrimary;
    private String relationshipNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

