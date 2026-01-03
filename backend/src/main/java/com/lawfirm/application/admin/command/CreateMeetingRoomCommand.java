package com.lawfirm.application.admin.command;

import lombok.Data;

/**
 * 创建会议室命令
 */
@Data
public class CreateMeetingRoomCommand {
    /** 会议室名称 */
    private String name;
    /** 会议室编码 */
    private String code;
    /** 位置 */
    private String location;
    /** 容纳人数 */
    private Integer capacity;
    /** 设备 */
    private String equipment;
    /** 描述 */
    private String description;
    /** 排序 */
    private Integer sortOrder;
}
