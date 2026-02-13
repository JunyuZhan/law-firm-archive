package com.archivesystem.dto.archive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 档案接收请求DTO.
 * 用于接收外部系统（如律所管理系统）推送的归档档案
 */
@Data
public class ArchiveReceiveRequest {

    /** 来源类型 */
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    /** 来源系统ID */
    @NotBlank(message = "来源系统ID不能为空")
    private String sourceId;

    /** 来源系统编号 */
    private String sourceNo;

    /** 回调URL（处理完成后通知外部系统） */
    private String callbackUrl;
    
    /** 是否异步处理（默认true） */
    private Boolean async = true;

    /** 档案题名 */
    @NotBlank(message = "档案题名不能为空")
    @Size(max = 500, message = "档案题名不能超过500个字符")
    private String title;

    /** 档案门类 */
    @NotBlank(message = "档案门类不能为空")
    private String archiveType;

    /** 保管期限代码 */
    @NotBlank(message = "保管期限不能为空")
    private String retentionPeriod;

    /** 责任者 */
    private String responsibility;

    /** 文件日期 */
    private LocalDate documentDate;

    /** 密级 */
    private String securityLevel;

    // ===== 业务关联信息（律所业务）=====

    /** 案件编号 */
    private String caseNo;

    /** 案件名称 */
    private String caseName;

    /** 委托人 */
    private String clientName;

    /** 主办律师 */
    private String lawyerName;

    /** 结案日期 */
    private LocalDate caseCloseDate;

    // ===== 电子文件列表 =====

    /** 电子文件列表 */
    @Valid
    private List<FileInfo> files;

    /** 扩展元数据 */
    private Map<String, Object> metadata;

    /** 关键词 */
    private String keywords;

    /** 摘要 */
    @Size(max = 2000, message = "摘要不能超过2000个字符")
    private String archiveAbstract;

    /** 备注 */
    private String remarks;

    /**
     * 文件信息.
     */
    @Data
    public static class FileInfo {
        /** 文件名 */
        @NotBlank(message = "文件名不能为空")
        private String fileName;

        /** 文件类型 */
        private String fileType;

        /** 下载URL */
        @NotBlank(message = "文件下载URL不能为空")
        private String downloadUrl;

        /** 文件大小（字节） */
        private Long fileSize;

        /** 文件分类 */
        private String fileCategory;

        /** 文件描述 */
        private String description;
    }
}
