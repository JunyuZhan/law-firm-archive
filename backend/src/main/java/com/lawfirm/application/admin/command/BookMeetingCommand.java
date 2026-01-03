package com.lawfirm.application.admin.command;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约会议命令
 */
@Data
public class BookMeetingCommand {
    /** 会议室ID */
    private Long roomId;
    /** 会议主题 */
    private String title;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;
    /** 参会人员ID列表 */
    private List<Long> attendeeIds;
    /** 会议描述 */
    private String description;
}
