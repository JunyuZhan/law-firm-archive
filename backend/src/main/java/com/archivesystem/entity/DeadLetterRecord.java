package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 死信消息记录实体.
 * 记录消息队列处理失败的消息，支持人工干预和重试.
 */
@Data
@TableName("arc_dead_letter_record")
public class DeadLetterRecord {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RETRYING = "RETRYING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_IGNORED = "IGNORED";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始消息ID */
    private String messageId;

    /** 原队列名称 */
    private String queueName;

    /** 路由键 */
    private String routingKey;

    /** 消息内容（JSON） */
    private String messageBody;

    /** 关联的档案ID */
    private Long archiveId;

    /** 来源类型 */
    private String sourceType;

    /** 来源ID */
    private String sourceId;

    /** 错误信息 */
    private String errorMessage;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetries;

    /** 状态 */
    private String status;

    /** 处理人 */
    private Long processedBy;

    /** 处理时间 */
    private LocalDateTime processedAt;

    /** 处理备注 */
    private String processRemark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 是否可重试.
     */
    public boolean canRetry() {
        return STATUS_PENDING.equals(status) || STATUS_FAILED.equals(status);
    }

    /**
     * 是否超过最大重试次数.
     */
    public boolean isMaxRetryExceeded() {
        return retryCount != null && maxRetries != null && retryCount >= maxRetries;
    }

    /**
     * 增加重试次数.
     */
    public void incrementRetryCount() {
        if (retryCount == null) {
            retryCount = 0;
        }
        retryCount++;
    }
}
