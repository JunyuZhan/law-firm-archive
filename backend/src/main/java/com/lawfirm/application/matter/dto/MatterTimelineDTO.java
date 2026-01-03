package com.lawfirm.application.matter.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目时间线事件DTO
 */
@Data
public class MatterTimelineDTO {

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型：CREATED, STATUS_CHANGED, TASK_COMPLETED, CONTRACT_SIGNED, PAYMENT_RECEIVED, TIMESHEET_RECORDED, MILESTONE_REACHED
     */
    private String eventType;

    /**
     * 事件类型名称
     */
    private String eventTypeName;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 事件标题
     */
    private String title;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 关联实体ID（如任务ID、合同ID等）
     */
    private Long relatedId;

    /**
     * 关联实体类型
     */
    private String relatedType;
}

