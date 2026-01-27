package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用印申请DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SealApplicationDTO extends BaseDTO {

    private Long id;
    private String applicationNo;
    
    private Long applicantId;
    private String applicantName;
    private Long departmentId;
    private String departmentName;
    
    private Long sealId;
    private String sealName;
    private String sealType;
    
    private Long matterId;
    private String matterName;
    
    private String documentName;
    private String documentType;
    private Integer copies;
    private String usePurpose;
    
    private LocalDate expectedUseDate;
    private LocalDate actualUseDate;
    
    private String status;
    private String statusName;
    
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String approvalComment;
    
    private Long usedBy;
    private String usedByName;
    private LocalDateTime usedAt;
    private String useRemark;
    
    /**
     * 附件文件URL（向后兼容字段）
     */
    private String attachmentUrl;
    
    /**
     * MinIO桶名称，默认law-firm
     */
    private String bucketName;
    
    /**
     * 存储路径：seal/M_{matterId}/{YYYY-MM}/用印附件/
     */
    private String storagePath;
    
    /**
     * 物理文件名：{YYYYMMDD}_{UUID}_{documentName}.{ext}
     */
    private String physicalName;
    
    /**
     * 文件Hash值（SHA-256），用于去重和校验
     */
    private String fileHash;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
