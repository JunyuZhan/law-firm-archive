package com.lawfirm.domain.workbench.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时报表执行记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("workbench_scheduled_report_log")
public class ScheduledReportLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 定时任务ID
     */
    private Long taskId;

    /**
     * 任务编号
     */
    private String taskNo;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;

    /**
     * 执行状态：RUNNING-执行中, SUCCESS-成功, FAILED-失败
     */
    private String status;

    /**
     * 生成的报表ID
     */
    private Long reportId;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 通知状态
     */
    private String notifyStatus;

    /**
     * 通知结果详情
     */
    private String notifyResult;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
