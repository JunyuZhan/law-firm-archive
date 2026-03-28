package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 档案鉴定记录实体类.
 * 对应数据库表: arc_appraisal_record
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("arc_appraisal_record")
public class AppraisalRecord extends BaseEntity {

    /** 档案ID */
    private Long archiveId;

    // ===== 鉴定类型 =====
    
    /** 鉴定类型 */
    private String appraisalType;

    // ===== 鉴定信息 =====
    
    /** 原值 */
    private String originalValue;
    
    /** 新值 */
    private String newValue;
    
    /** 鉴定原因 */
    private String appraisalReason;
    
    /** 鉴定意见 */
    private String appraisalOpinion;

    // ===== 审批信息 =====
    
    /** 状态 */
    @Builder.Default
    private String status = STATUS_PENDING;
    
    /** 鉴定人ID */
    private Long appraiserId;
    
    /** 鉴定人姓名 */
    private String appraiserName;
    
    /** 鉴定时间 */
    private LocalDateTime appraisedAt;
    
    /** 审批人ID */
    private Long approverId;
    
    /** 审批人姓名 */
    private String approverName;
    
    /** 审批时间 */
    private LocalDateTime approvedAt;
    
    /** 审批意见 */
    private String approvalComment;

    // ===== 鉴定类型常量 =====
    
    public static final String TYPE_VALUE = "VALUE";
    public static final String TYPE_SECURITY = "SECURITY";
    public static final String TYPE_OPEN = "OPEN";
    public static final String TYPE_RETENTION = "RETENTION";

    // ===== 状态常量 =====
    
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
}
