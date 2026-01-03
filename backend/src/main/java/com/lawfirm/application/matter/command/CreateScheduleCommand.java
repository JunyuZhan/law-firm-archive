package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建日程命令
 */
@Data
public class CreateScheduleCommand {

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 日程标题
     */
    @NotBlank(message = "日程标题不能为空")
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 地点
     */
    private String location;

    /**
     * 日程类型
     */
    @NotBlank(message = "日程类型不能为空")
    private String scheduleType;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 是否全天
     */
    private Boolean allDay;

    /**
     * 提前提醒分钟数
     */
    private Integer reminderMinutes;

    /**
     * 重复规则
     */
    private String recurrenceRule;
}
