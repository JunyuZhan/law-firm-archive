package com.lawfirm.domain.finance.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 合同审批通过事件
 * 触发时机：ContractAppService.approve() 方法执行成功后
 * 
 * Requirements: 1.1, 1.2
 */
@Getter
public class ContractApprovedEvent extends ApplicationEvent {
    
    /**
     * 合同ID
     */
    private final Long contractId;
    
    /**
     * 审批人ID
     */
    private final Long approverId;
    
    /**
     * 审批时间
     */
    private final LocalDateTime approvedAt;
    
    public ContractApprovedEvent(Object source, Long contractId, Long approverId) {
        super(source);
        this.contractId = contractId;
        this.approverId = approverId;
        this.approvedAt = LocalDateTime.now();
    }
}
