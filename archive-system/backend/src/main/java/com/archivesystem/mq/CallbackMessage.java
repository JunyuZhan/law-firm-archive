package com.archivesystem.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 回调消息 DTO
 * 用于通知外部系统档案处理结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 档案ID
     */
    private Long archiveId;
    
    /**
     * 档案号
     */
    private String archiveNo;
    
    /**
     * 来源类型
     */
    private String sourceType;
    
    /**
     * 来源系统ID
     */
    private String sourceId;
    
    /**
     * 回调URL
     */
    private String callbackUrl;
    
    /**
     * 处理状态: SUCCESS, FAILED, PARTIAL
     */
    private String status;
    
    /**
     * 成功处理的文件数
     */
    private int successCount;
    
    /**
     * 失败的文件数
     */
    private int failedCount;
    
    /**
     * 总文件数
     */
    private int totalCount;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
    
    /**
     * 处理完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 最大重试次数
     */
    private int maxRetries;
    
    // 状态常量
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PARTIAL = "PARTIAL";
}
