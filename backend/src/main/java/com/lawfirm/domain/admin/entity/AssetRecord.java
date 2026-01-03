package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 资产操作记录实体（领用、归还、维修、报废等）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("admin_asset_record")
public class AssetRecord extends BaseEntity {

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 操作类型：RECEIVE-领用, RETURN-归还, TRANSFER-转移, MAINTENANCE-维修, SCRAP-报废
     */
    private String recordType;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 原使用人ID
     */
    private Long fromUserId;

    /**
     * 新使用人ID
     */
    private Long toUserId;

    /**
     * 操作日期
     */
    private LocalDate operateDate;

    /**
     * 预计归还日期（领用时填写）
     */
    private LocalDate expectedReturnDate;

    /**
     * 实际归还日期
     */
    private LocalDate actualReturnDate;

    /**
     * 维修原因/报废原因
     */
    private String reason;

    /**
     * 维修费用
     */
    private java.math.BigDecimal maintenanceCost;

    /**
     * 审批状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝
     */
    private String approvalStatus;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 备注
     */
    private String remarks;
}
