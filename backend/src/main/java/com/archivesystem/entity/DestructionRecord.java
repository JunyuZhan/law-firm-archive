package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 档案销毁记录实体类.
 * 对应数据库表: arc_destruction_record
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_destruction_record")
public class DestructionRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 销毁批次号 */
    private String destructionBatchNo;
    
    /** 档案ID */
    private Long archiveId;

    // ===== 销毁信息 =====
    
    /** 销毁原因 */
    private String destructionReason;
    
    /** 销毁方式 */
    private String destructionMethod;

    // ===== 审批信息 =====
    
    /** 状态 */
    @Builder.Default
    private String status = STATUS_PENDING;
    
    /** 提议人ID */
    private Long proposerId;
    
    /** 提议人姓名 */
    private String proposerName;
    
    /** 提议时间 */
    private LocalDateTime proposedAt;
    
    /** 审批人ID */
    private Long approverId;
    
    /** 审批人姓名 */
    private String approverName;
    
    /** 审批时间 */
    private LocalDateTime approvedAt;
    
    /** 审批意见 */
    private String approvalComment;
    
    /** 执行人ID */
    private Long executorId;
    
    /** 执行人姓名 */
    private String executorName;
    
    /** 销毁时间 */
    private LocalDateTime executedAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ===== 销毁方式常量 =====
    
    public static final String METHOD_LOGICAL = "LOGICAL";
    public static final String METHOD_PHYSICAL = "PHYSICAL";

    // ===== 状态常量 =====
    
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_EXECUTED = "EXECUTED";
}
