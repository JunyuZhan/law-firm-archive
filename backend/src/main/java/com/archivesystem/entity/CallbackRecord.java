package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.archivesystem.config.mybatis.PostgreSQLJsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 回调记录实体类.
 * 记录向来源系统发起的回调
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "arc_callback_record", autoResultMap = true)
public class CallbackRecord extends BaseEntity {

    // ===== 档案关联 =====
    
    /** 档案ID */
    private Long archiveId;
    
    /** 档案号 */
    private String archiveNo;
    
    /** 关联推送记录ID */
    private Long pushRecordId;

    // ===== 回调信息 =====
    
    /** 回调地址 */
    private String callbackUrl;
    
    /** 回调类型 */
    private String callbackType;

    // ===== 状态信息 =====
    
    /** 回调状态 */
    private String callbackStatus;

    // ===== 请求响应 =====
    
    /** 请求体 */
    @TableField(typeHandler = PostgreSQLJsonbTypeHandler.class)
    private Map<String, Object> requestBody;
    
    /** HTTP状态码 */
    private Integer responseCode;
    
    /** 响应内容 */
    private String responseBody;

    // ===== 重试信息 =====
    
    /** 已重试次数 */
    private Integer retryCount;
    
    /** 最大重试次数 */
    private Integer maxRetries;
    
    /** 下次重试时间 */
    private LocalDateTime nextRetryAt;

    // ===== 错误信息 =====
    
    /** 错误信息 */
    private String errorMessage;

    // ===== 时间信息 =====
    
    /** 首次回调时间 */
    private LocalDateTime callbackAt;
    
    /** 完成时间 */
    private LocalDateTime completedAt;

    // ===== 状态常量 =====
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    // ===== 回调类型常量 =====
    public static final String TYPE_ARCHIVE_RECEIVED = "ARCHIVE_RECEIVED";
    public static final String TYPE_FILE_TRANSFERRED = "FILE_TRANSFERRED";
    public static final String TYPE_ARCHIVE_STORED = "ARCHIVE_STORED";
}
