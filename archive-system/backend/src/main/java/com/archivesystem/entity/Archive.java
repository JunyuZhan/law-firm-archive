package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 电子档案实体类.
 * 对应数据库表: arc_archive
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "arc_archive", autoResultMap = true)
public class Archive extends BaseEntity {

    // ===== 档案标识 =====
    
    /** 档案号（系统生成，唯一标识） */
    private String archiveNo;

    // ===== 分类信息 =====
    
    /** 全宗ID */
    private Long fondsId;
    
    /** 全宗号（冗余） */
    private String fondsNo;
    
    /** 分类ID */
    private Long categoryId;
    
    /** 分类号（冗余） */
    private String categoryCode;
    
    /** 档案门类 */
    private String archiveType;

    // ===== 基本信息 =====
    
    /** 题名 */
    private String title;
    
    /** 文件编号（原文件号） */
    private String fileNo;
    
    /** 责任者 */
    private String responsibility;
    
    /** 归档日期 */
    private LocalDate archiveDate;
    
    /** 文件日期 */
    private LocalDate documentDate;
    
    /** 页数 */
    private Integer pageCount;
    
    /** 件数 */
    @Builder.Default
    private Integer piecesCount = 1;

    // ===== 保管信息 =====
    
    /** 保管期限代码 */
    private String retentionPeriod;
    
    /** 保管到期日期 */
    private LocalDate retentionExpireDate;
    
    /** 密级 */
    @Builder.Default
    private String securityLevel = SECURITY_INTERNAL;
    
    /** 解密日期 */
    private LocalDate securityExpireDate;

    // ===== 来源信息 =====
    
    /** 来源类型 */
    private String sourceType;
    
    /** 来源系统名称 */
    private String sourceSystem;
    
    /** 来源系统ID */
    private String sourceId;
    
    /** 来源系统编号 */
    private String sourceNo;
    
    /** 回调URL（处理完成后通知） */
    private String callbackUrl;

    // ===== 业务关联（律所业务） =====
    
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

    // ===== 存储信息 =====
    
    /** 是否有电子文件 */
    @Builder.Default
    private Boolean hasElectronic = true;
    
    /** 存储位置描述 */
    private String storageLocation;
    
    /** 总文件大小（字节） */
    @Builder.Default
    private Long totalFileSize = 0L;
    
    /** 文件数量 */
    @Builder.Default
    private Integer fileCount = 0;

    // ===== 状态信息 =====
    
    /** 状态 */
    @Builder.Default
    private String status = STATUS_RECEIVED;

    // ===== 操作信息 =====
    
    /** 接收时间 */
    private LocalDateTime receivedAt;
    
    /** 接收人 */
    private Long receivedBy;
    
    /** 著录时间 */
    private LocalDateTime catalogedAt;
    
    /** 著录人 */
    private Long catalogedBy;
    
    /** 归档时间（正式入库） */
    private LocalDateTime archivedAt;
    
    /** 归档人 */
    private Long archivedBy;

    // ===== 扩展信息 =====
    
    /** 关键词（逗号分隔） */
    private String keywords;
    
    /** 摘要 */
    @TableField("\"abstract\"")
    private String archiveAbstract;
    
    /** 备注 */
    private String remarks;
    
    /** 扩展数据（JSON格式） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;

    // ===== 状态常量 =====
    
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_CATALOGING = "CATALOGING";
    public static final String STATUS_STORED = "STORED";
    public static final String STATUS_BORROWED = "BORROWED";
    public static final String STATUS_APPRAISAL = "APPRAISAL";
    public static final String STATUS_DESTROYED = "DESTROYED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_FAILED = "FAILED";

    // ===== 来源类型常量 =====
    
    public static final String SOURCE_LAW_FIRM = "LAW_FIRM";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_IMPORT = "IMPORT";
    public static final String SOURCE_TRANSFER = "TRANSFER";

    // ===== 档案门类常量 =====
    
    public static final String TYPE_DOCUMENT = "DOCUMENT";
    public static final String TYPE_SCIENCE = "SCIENCE";
    public static final String TYPE_ACCOUNTING = "ACCOUNTING";
    public static final String TYPE_PERSONNEL = "PERSONNEL";
    public static final String TYPE_SPECIAL = "SPECIAL";
    public static final String TYPE_AUDIOVISUAL = "AUDIOVISUAL";

    // ===== 密级常量 =====
    
    public static final String SECURITY_PUBLIC = "PUBLIC";
    public static final String SECURITY_INTERNAL = "INTERNAL";
    public static final String SECURITY_SECRET = "SECRET";
    public static final String SECURITY_CONFIDENTIAL = "CONFIDENTIAL";
}
