package com.lawfirm.application.matter.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计时器会话 DTO（M3-044）
 */
@Data
public class TimerSessionDTO {

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 案件名称
     */
    private String matterName;

    /**
     * 工作类型
     */
    private String workType;

    /**
     * 工作内容
     */
    private String workContent;

    /**
     * 是否计费
     */
    private Boolean billable;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 暂停时间
     */
    private LocalDateTime pauseTime;

    /**
     * 恢复时间
     */
    private LocalDateTime resumeTime;

    /**
     * 已累计的秒数
     */
    private Long elapsedSeconds;

    /**
     * 当前总秒数（包括正在运行的时间）
     */
    private Long totalSeconds;

    /**
     * 状态：RUNNING-运行中, PAUSED-已暂停, STOPPED-已停止
     */
    private String status;

    /**
     * 格式化后的时间显示（HH:mm:ss）
     */
    private String formattedTime;
}

