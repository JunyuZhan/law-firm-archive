package com.lawfirm.domain.workbench.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 审批记录实体
 * 统一管理所有业务模块的审批流程
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("workbench_approval")
public class Approval extends BaseEntity {

    /**
     * 审批编号
     */
    private String approvalNo;

    /**
     * 业务类型（CONTRACT-合同, SEAL_APPLICATION-用印申请, CONFLICT_CHECK-利冲检查, etc.）
     */
    private String businessType;

    /**
     * 业务ID（关联具体业务表的主键）
     */
    private Long businessId;

    /**
     * 业务编号（如合同编号、用印申请编号等）
     */
    private String businessNo;

    /**
     * 业务标题
     */
    private String businessTitle;

    /**
     * 发起人ID
     */
    private Long applicantId;

    /**
     * 发起人姓名
     */
    private String applicantName;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批状态（PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消）
     */
    private String status;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 审批时间
     */
    private java.time.LocalDateTime approvedAt;

    /**
     * 优先级（HIGH-高, MEDIUM-中, LOW-低）
     */
    private String priority;

    /**
     * 紧急程度（URGENT-紧急, NORMAL-普通）
     */
    private String urgency;

    /**
     * 业务数据快照（JSON格式，保存审批时的业务数据）
     */
    private String businessSnapshot;

    /**
     * 审批附件文件URL（向后兼容字段）
     */
    private String fileUrl;

    /**
     * MinIO桶名称，默认law-firm
     */
    private String bucketName;

    /**
     * 存储路径：approval/{businessType}/M_{matterId}/{YYYY-MM}/审批附件/
     */
    private String storagePath;

    /**
     * 物理文件名：20260127_uuid_审批附件.pdf（支持超长文件名，最大1000字符）
     */
    private String physicalName;

    /**
     * 文件Hash值（SHA-256），用于去重和校验
     */
    private String fileHash;
}

