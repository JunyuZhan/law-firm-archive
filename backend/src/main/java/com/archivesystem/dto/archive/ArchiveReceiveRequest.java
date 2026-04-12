package com.archivesystem.dto.archive;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 档案接收请求DTO.
 * 用于接收外部系统（如律所管理系统）推送的归档档案
 * @author junyuzhan
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
    @JsonAlias("responsiblePerson")
    private String responsibility;
    
    /** 全宗号（管理系统指定） */
    private String fondsCode;
    
    /** 分类号（管理系统指定） */
    private String categoryCode;

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
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate caseCloseDate;
    
    /** 结案日期（字符串格式，兼容管理系统） */
    private String caseCloseDateStr;
    
    /**
     * 接收字符串格式的结案日期（兼容管理系统发送的 caseCloseDate 字段）.
     * Jackson 会先尝试解析为 LocalDate，失败时调用此方法
     */
    @JsonSetter("caseCloseDate")
    public void setCaseCloseDateFromString(Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof LocalDate) {
            this.caseCloseDate = (LocalDate) value;
        } else if (value instanceof String) {
            String strValue = (String) value;
            if (!strValue.isEmpty()) {
                try {
                    this.caseCloseDate = LocalDate.parse(strValue);
                } catch (DateTimeParseException e) {
                    this.caseCloseDateStr = strValue;
                }
            }
        }
    }

    @JsonIgnore
    public void setCaseCloseDate(LocalDate caseCloseDate) {
        this.caseCloseDate = caseCloseDate;
        this.caseCloseDateStr = null;
    }

    // ===== 电子文件列表 =====

    /** 电子文件列表 */
    @Valid
    private List<FileInfo> files;

    /** 扩展元数据 */
    private Map<String, Object> metadata;
    
    /** 扩展属性（兼容管理系统字段名） */
    private Map<String, Object> extraAttributes;

    /** 关键词 */
    private String keywords;

    /** 摘要 */
    @Size(max = 2000, message = "摘要不能超过2000个字符")
    private String archiveAbstract;
    
    /** 描述（兼容管理系统字段名，映射到摘要） */
    private String description;

    /** 备注 */
    private String remarks;
    
    // ===== 兼容字段处理方法 =====
    
    /**
     * 获取结案日期（优先使用 LocalDate，其次解析字符串）.
     */
    public LocalDate getEffectiveCaseCloseDate() {
        if (caseCloseDate != null) {
            return caseCloseDate;
        }
        if (caseCloseDateStr != null && !caseCloseDateStr.isEmpty()) {
            try {
                return LocalDate.parse(caseCloseDateStr);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 获取摘要（优先使用 archiveAbstract，其次使用 description）.
     */
    public String getEffectiveAbstract() {
        if (archiveAbstract != null && !archiveAbstract.isEmpty()) {
            return archiveAbstract;
        }
        return description;
    }
    
    /**
     * 获取扩展数据（合并 metadata 和 extraAttributes）.
     */
    public Map<String, Object> getEffectiveMetadata() {
        if (metadata != null && extraAttributes != null) {
            Map<String, Object> merged = new java.util.HashMap<>(metadata);
            merged.putAll(extraAttributes);
            return merged;
        }
        if (metadata != null) {
            return metadata;
        }
        return extraAttributes;
    }

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
        
        /** MIME类型（兼容管理系统字段名） */
        private String mimeType;

        /** 下载URL */
        @NotBlank(message = "文件下载URL不能为空")
        private String downloadUrl;

        /** 文件大小（字节） */
        private Long fileSize;

        /** 文件分类 */
        private String fileCategory;

        /** 文件描述 */
        private String description;
        
        /** 排序号（兼容管理系统字段） */
        private Integer sortOrder;

        /** 案卷卷号 */
        private Integer volumeNo;

        /** 案卷分段类型 */
        private String sectionType;

        /** 件号/文号 */
        private String documentNo;

        /** 起始页码 */
        private Integer pageStart;

        /** 截止页码 */
        private Integer pageEnd;

        /** 版本标识 */
        private String versionLabel;

        /** 文件来源类型 */
        private String fileSourceType;

        /** 扫描批次号 */
        private String scanBatchNo;

        /** 扫描操作人 */
        private String scanOperator;

        /** 扫描时间 */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime scanTime;

        /** 扫描复核状态 */
        private String scanCheckStatus;

        /** 扫描复核人 */
        private String scanCheckBy;

        /** 扫描复核时间 */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime scanCheckTime;
        
        /**
         * 获取文件类型（优先使用 fileType，其次使用 mimeType）.
         */
        public String getEffectiveFileType() {
            if (fileType != null && !fileType.isEmpty()) {
                return fileType;
            }
            return mimeType;
        }
    }
}
