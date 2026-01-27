package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用印申请实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("seal_application")
public class SealApplication extends BaseEntity {

    /**
     * 申请编号
     */
    private String applicationNo;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 印章ID
     */
    private Long sealId;

    /**
     * 印章名称
     */
    private String sealName;

    /**
     * 关联案件ID
     */
    private Long matterId;

    /**
     * 关联案件名称
     */
    private String matterName;

    /**
     * 用印文件名称
     */
    private String documentName;

    /**
     * 文件类型
     */
    private String documentType;

    /**
     * 份数
     */
    @lombok.Builder.Default
    private Integer copies = 1;

    /**
     * 用印目的
     */
    private String usePurpose;

    /**
     * 预计用印日期
     */
    private LocalDate expectedUseDate;

    /**
     * 实际用印日期
     */
    private LocalDate actualUseDate;

    /**
     * 状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, USED-已用印, CANCELLED-已取消
     */
    @lombok.Builder.Default
    private String status = "PENDING";

    /**
     * 审批人ID
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 用印人ID
     */
    private Long usedBy;

    /**
     * 用印时间
     */
    private LocalDateTime usedAt;

    /**
     * 用印备注
     */
    private String useRemark;

    /**
     * 附件文件URL（向后兼容字段）
     */
    private String attachmentUrl;

    /**
     * MinIO桶名称，默认law-firm
     */
    private String bucketName;

    /**
     * 存储路径：seal/M_{matterId}/{YYYY-MM}/用印附件/
     */
    private String storagePath;

    /**
     * 物理文件名：{YYYYMMDD}_{UUID}_{documentName}.{ext}（支持超长文件名，最大1000字符）
     */
    private String physicalName;

    /**
     * 文件Hash值（SHA-256），用于去重和校验
     */
    private String fileHash;
}
