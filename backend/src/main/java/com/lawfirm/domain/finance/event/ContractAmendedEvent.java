package com.lawfirm.domain.finance.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 合同变更事件
 * 当律师变更合同后触发，通知财务模块处理数据同步
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAmendedEvent {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 变更类型：AMOUNT-金额变更, PARTICIPANT-参与人变更, SCHEDULE-付款计划变更, OTHER-其他
     */
    private String amendmentType;

    /**
     * 变更人ID
     */
    private Long amendedBy;

    /**
     * 变更时间
     */
    private LocalDateTime amendedAt;

    /**
     * 变更说明
     */
    private String amendmentReason;

    /**
     * 变更前数据快照（JSON格式）
     */
    private String beforeSnapshot;

    /**
     * 变更后数据快照（JSON格式）
     */
    private String afterSnapshot;
}
