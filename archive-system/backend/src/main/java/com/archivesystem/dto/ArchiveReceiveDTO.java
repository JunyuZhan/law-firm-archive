package com.archivesystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 档案接收DTO.
 * 用于接收来自律所系统或其他外部系统的档案数据。
 */
@Data
public class ArchiveReceiveDTO {

    // ===== 来源信息 =====
    
    /** 来源系统类型：LAW_FIRM, COURT, ENTERPRISE, OTHER */
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    /** 来源系统编码（如配置的来源编码） */
    private String sourceCode;

    /** 来源系统中的档案ID */
    private String sourceId;

    /** 来源系统中的档案编号 */
    private String sourceNo;

    // ===== 档案基本信息 =====
    
    /** 档案名称 */
    @NotBlank(message = "档案名称不能为空")
    private String archiveName;

    /** 档案类型：LITIGATION, NON_LITIGATION, CONSULTATION, OTHER */
    private String archiveType;

    /** 档案分类 */
    private String category;

    /** 档案描述 */
    private String description;

    // ===== 关联信息 =====
    
    /** 客户名称 */
    private String clientName;

    /** 主办人/负责人 */
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

    // ===== 保管信息 =====
    
    /** 保管期限 */
    private String retentionPeriod;

    // ===== 附件信息 =====
    
    /** 电子档案文件列表 */
    private List<ArchiveFileDTO> files;

    // ===== 原始数据快照 =====
    
    /** 来源系统的完整数据快照（JSON格式） */
    private Map<String, Object> sourceSnapshot;

    /** 备注 */
    private String remarks;

    /**
     * 档案文件DTO.
     */
    @Data
    public static class ArchiveFileDTO {
        /** 文件名称 */
        @NotBlank(message = "文件名不能为空")
        private String fileName;

        /** 文件类型 */
        private String fileType;

        /** 文件大小 */
        private Long fileSize;

        /** 文件下载URL */
        private String downloadUrl;

        /** 文件分类 */
        private String category;

        /** 排序号 */
        private Integer sortOrder;

        /** 描述 */
        private String description;
    }
}
