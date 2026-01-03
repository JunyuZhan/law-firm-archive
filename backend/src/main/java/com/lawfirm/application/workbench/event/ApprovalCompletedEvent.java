package com.lawfirm.application.workbench.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 审批完成事件
 */
@Getter
public class ApprovalCompletedEvent extends ApplicationEvent {
    
    private final Long approvalId;
    private final String businessType;
    private final Long businessId;
    private final String result;  // APPROVED 或 REJECTED
    private final String comment;

    public ApprovalCompletedEvent(Object source, Long approvalId, String businessType, 
                                 Long businessId, String result, String comment) {
        super(source);
        this.approvalId = approvalId;
        this.businessType = businessType;
        this.businessId = businessId;
        this.result = result;
        this.comment = comment;
    }
}

