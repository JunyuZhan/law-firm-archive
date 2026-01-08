package com.lawfirm.application.system.command;

import lombok.Data;

/**
 * 确认数据交接命令
 */
@Data
public class ConfirmHandoverCommand {

    /**
     * 交接单ID
     */
    private Long handoverId;

    /**
     * 确认备注
     */
    private String remark;
}

