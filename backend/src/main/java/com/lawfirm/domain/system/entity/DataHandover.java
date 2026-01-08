package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 数据交接记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_handover")
public class DataHandover extends BaseEntity {

    /**
     * 交接单号
     */
    private String handoverNo;

    /**
     * 移交人ID
     */
    private Long fromUserId;

    /**
     * 移交人姓名
     */
    private String fromUsername;

    /**
     * 接收人ID
     */
    private Long toUserId;

    /**
     * 接收人姓名
     */
    private String toUsername;

    /**
     * 交接类型：RESIGNATION-离职交接, PROJECT-项目移交, CLIENT-客户移交, LEAD-案源移交
     */
    private String handoverType;

    /**
     * 交接原因
     */
    private String handoverReason;

    /**
     * 状态：PENDING_APPROVAL-待审批, APPROVED-审批通过待执行, REJECTED-已拒绝, CONFIRMED-已确认, CANCELLED-已取消
     */
    @lombok.Builder.Default
    private String status = "PENDING_APPROVAL";

    /**
     * 关联的审批记录ID
     */
    private Long approvalId;

    /**
     * 移交项目数
     */
    @lombok.Builder.Default
    private Integer matterCount = 0;

    /**
     * 移交客户数
     */
    @lombok.Builder.Default
    private Integer clientCount = 0;

    /**
     * 移交案源数
     */
    @lombok.Builder.Default
    private Integer leadCount = 0;

    /**
     * 移交任务数
     */
    @lombok.Builder.Default
    private Integer taskCount = 0;

    /**
     * 提交人ID
     */
    private Long submittedBy;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 确认人ID
     */
    private Long confirmedBy;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedAt;

    /**
     * 备注
     */
    private String remark;
}

