package com.lawfirm.application.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户文件 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientFileDTO {

    private Long id;
    
    private Long matterId;
    
    private String matterName;
    
    private Long clientId;
    
    private String clientName;
    
    private String fileName;
    
    private String originalFileName;
    
    private Long fileSize;
    
    private String fileType;
    
    private String fileCategory;
    
    private String fileCategoryName;
    
    private String description;
    
    private String externalFileId;
    
    private String externalFileUrl;
    
    private String uploadedBy;
    
    private LocalDateTime uploadedAt;
    
    private String status;
    
    private String statusName;
    
    private Long localDocumentId;
    
    private Long targetDossierId;
    
    private String targetDossierName;
    
    private LocalDateTime syncedAt;
    
    private Long syncedBy;
    
    private String syncedByName;
    
    private String errorMessage;
    
    private LocalDateTime createdAt;
}
