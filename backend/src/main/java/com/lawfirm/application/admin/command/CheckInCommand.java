package com.lawfirm.application.admin.command;

import lombok.Data;

/**
 * 签到命令
 */
@Data
public class CheckInCommand {
    /** 签到地点 */
    private String location;
    /** 签到设备 */
    private String device;
}
