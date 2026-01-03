package com.lawfirm.application.admin.command;

import lombok.Data;

/**
 * 审批请假命令
 */
@Data
public class ApproveLeaveCommand {
    /** 申请ID */
    private Long applicationId;
    /** 是否批准 */
    private Boolean approved;
    /** 审批意见 */
    private String comment;
}
