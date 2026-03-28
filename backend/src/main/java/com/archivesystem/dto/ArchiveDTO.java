package com.archivesystem.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案DTO.
 */
@Data
public class ArchiveDTO {

    private Long id;

    /** 档案编号 */
    private String archiveNo;

    /** 档案名称 */
    private String archiveName;

    /** 档案类型 */
    private String archiveType;

    /** 档案类型名称 */
    private String archiveTypeName;

    /** 档案分类 */
    private String category;

    /** 分类名称 */
    private String categoryName;

    /** 档案描述 */
    private String description;

    // ===== 来源信息 =====
    
    /** 来源类型 */
    private String sourceType;

    /** 来源类型名称 */
    private String sourceTypeName;

    /** 来源系统ID */
    private String sourceId;

    /** 来源系统编号 */
    private String sourceNo;

    /** 来源系统名称 */
    private String sourceName;

    // ===== 关联信息 =====
    
    /** 客户名称 */
    private String clientName;

    /** 主办人 */
    private String responsiblePerson;

    /** 案件结案日期 */
    private LocalDate caseCloseDate;

    // ===== 档案物理信息 =====
    
    /** 卷数 */
    private Integer volumeCount;

    /** 页数 */
    private Integer pageCount;

    /** 档案目录 */
    private String catalog;

    /** 存放位置ID */
    private Long locationId;

    /** 存放位置名称 */
    private String locationName;

    /** 盒号 */
    private String boxNo;

    /** 是否有电子档案 */
    private Boolean hasElectronic;

    // ===== 保管信息 =====
    
    /** 保管期限 */
    private String retentionPeriod;

    /** 保管期限名称 */
    private String retentionPeriodName;

    /** 保管到期日期 */
    private LocalDate retentionExpireDate;

    // ===== 状态信息 =====
    
    /** 状态 */
    private String status;

    /** 状态名称 */
    private String statusName;

    /** 入库人 */
    private String storedByName;

    /** 入库时间 */
    private LocalDateTime storedAt;

    /** 接收时间 */
    private LocalDateTime receivedAt;

    /** 备注 */
    private String remarks;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // ===== 关联数据 =====
    
    /** 档案文件列表 */
    private List<ArchiveFileDTO> files;

    /** 文件数量 */
    private Integer fileCount;

    /**
     * 档案文件DTO.
     */
    @Data
    public static class ArchiveFileDTO {
        private Long id;
        private String fileName;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
        private String category;
        private String categoryName;
        private Integer sortOrder;
        private String description;
        private String downloadUrl;
        private LocalDateTime createdAt;
    }
}
