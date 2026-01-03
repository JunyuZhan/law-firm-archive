package com.lawfirm.application.hr.command;

import lombok.Data;

/**
 * 审批离职申请命令
 */
@Data
public class ApproveResignationCommand {

    /**
     * 是否通过
     */
    private Boolean approved;

    /**
     * 审批意见
     */
    private String comment;
}

