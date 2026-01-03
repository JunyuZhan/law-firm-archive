package com.lawfirm.application.workbench.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalQueryDTO extends PageQuery {
    /**
     * 审批状态
     */
    private String status;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 发起人ID（查询我发起的）
     */
    private Long applicantId;

    /**
     * 审批人ID（查询待审批/已审批）
     */
    private Long approverId;
}

