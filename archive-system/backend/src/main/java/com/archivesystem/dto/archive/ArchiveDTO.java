package com.archivesystem.dto.archive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 档案DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveDTO {

    private Long id;
    private String archiveNo;
    
    // 分类信息
    private Long fondsId;
    private String fondsNo;
    private String fondsName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String archiveType;
    
    // 基本信息
    private String title;
    private String fileNo;
    private String responsibility;
    private LocalDate archiveDate;
    private LocalDate documentDate;
    private Integer pageCount;
    private Integer piecesCount;
    
    // 保管信息
    private String retentionPeriod;
    private String retentionPeriodName;
    private LocalDate retentionExpireDate;
    private String securityLevel;
    private LocalDate securityExpireDate;
    
    // 来源信息
    private String sourceType;
    private String sourceSystem;
    private String sourceId;
    private String sourceNo;
    
    // 业务关联
    private String caseNo;
    private String caseName;
    private String clientName;
    private String lawyerName;
    private LocalDate caseCloseDate;
    
    // 存储信息
    private String archiveForm;
    private Boolean hasElectronic;
    private Boolean hasPhysical;
    private Long locationId;
    private String storageLocation;
    private String boxNo;
    private Long totalFileSize;
    private Integer fileCount;
    
    // 状态信息
    private String status;
    private String statusName;
    
    // 操作信息
    private LocalDateTime receivedAt;
    private String receivedByName;
    private LocalDateTime catalogedAt;
    private String catalogedByName;
    private LocalDateTime archivedAt;
    private String archivedByName;
    
    // 扩展信息
    private String keywords;
    private String archiveAbstract;
    private String remarks;
    private Map<String, Object> extraData;
    
    // 文件列表
    private List<DigitalFileDTO> files;
    
    // 系统字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
