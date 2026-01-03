package com.lawfirm.application.admin.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假申请命令
 */
@Data
public class ApplyLeaveCommand {
    /** 请假类型ID */
    private Long leaveTypeId;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;
    /** 请假时长(天) */
    private BigDecimal duration;
    /** 请假原因 */
    private String reason;
    /** 附件URL */
    private String attachmentUrl;
}
