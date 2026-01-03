package com.lawfirm.application.hr.command;

import lombok.Data;

/**
 * 审批转正申请命令
 */
@Data
public class ApproveRegularizationCommand {

    /**
     * 是否通过
     */
    private Boolean approved;

    /**
     * 审批意见
     */
    private String comment;
}

