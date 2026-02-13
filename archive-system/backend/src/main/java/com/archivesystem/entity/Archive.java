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
 * 档案实体类.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "archive", autoResultMap = true)
public class Archive extends BaseEntity {

    // ===== 档案基本信息 =====
    
    /** 档案编号（本系统生成） */
    private String archiveNo;

    /** 档案名称 */
    private String archiveName;

    /** 档案类型：LITIGATION-诉讼, NON_LITIGATION-非诉, CONSULTATION-咨询, OTHER-其他 */
    private String archiveType;

    /** 档案分类：CASE-案件档案, CONTRACT-合同档案, PERSONNEL-人事档案, FINANCE-财务档案, OTHER-其他 */
    private String category;

    /** 档案描述 */
    private String description;

    // ===== 来源信息 =====
    
    /** 来源类型：LAW_FIRM-律所系统, MANUAL-手动录入, IMPORT-批量导入, EXTERNAL-外部系统 */
    private String sourceType;

    /** 来源系统ID（外部系统的档案ID） */
    private String sourceId;

    /** 来源系统编号（如律所系统的案件编号） */
    private String sourceNo;

    /** 来源系统名称 */
    private String sourceName;

    /** 来源数据快照（JSON格式，保存原始数据） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> sourceSnapshot;

    // ===== 关联信息 =====
    
    /** 客户名称 */
    private String clientName;

    /** 主办人/负责人名称 */
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

    /** 盒号 */
    private String boxNo;

    /** 是否有电子档案 */
    private Boolean hasElectronic;

    // ===== 保管信息 =====
    
    /** 保管期限：PERMANENT-永久, 30_YEARS-30年, 15_YEARS-15年, 10_YEARS-10年, 5_YEARS-5年 */
    private String retentionPeriod;

    /** 保管到期日期 */
    private LocalDate retentionExpireDate;

    // ===== 状态信息 =====
    
    /** 
     * 状态：
     * RECEIVED-已接收, PENDING-待入库, STORED-已入库, 
     * BORROWED-借出中, PENDING_DESTROY-待销毁, DESTROYED-已销毁
     */
    private String status;

    /** 入库人ID */
    private Long storedBy;

    /** 入库时间 */
    private LocalDateTime storedAt;

    /** 接收时间 */
    private LocalDateTime receivedAt;

    /** 备注 */
    private String remarks;

    // ===== 状态常量 =====
    
    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_STORED = "STORED";
    public static final String STATUS_BORROWED = "BORROWED";
    public static final String STATUS_PENDING_DESTROY = "PENDING_DESTROY";
    public static final String STATUS_DESTROYED = "DESTROYED";

    // ===== 来源类型常量 =====
    
    public static final String SOURCE_LAW_FIRM = "LAW_FIRM";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_IMPORT = "IMPORT";
    public static final String SOURCE_EXTERNAL = "EXTERNAL";

    // ===== 档案类型常量 =====
    
    public static final String TYPE_LITIGATION = "LITIGATION";
    public static final String TYPE_NON_LITIGATION = "NON_LITIGATION";
    public static final String TYPE_CONSULTATION = "CONSULTATION";
    public static final String TYPE_OTHER = "OTHER";

    // ===== 分类常量 =====
    
    public static final String CATEGORY_CASE = "CASE";
    public static final String CATEGORY_CONTRACT = "CONTRACT";
    public static final String CATEGORY_PERSONNEL = "PERSONNEL";
    public static final String CATEGORY_FINANCE = "FINANCE";
    public static final String CATEGORY_OTHER = "OTHER";
}
