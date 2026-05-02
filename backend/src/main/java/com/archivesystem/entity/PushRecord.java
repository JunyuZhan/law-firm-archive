package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.archivesystem.config.mybatis.PostgreSQLJsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 推送记录实体类.
 * 记录外部系统推送档案的请求
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "arc_push_record", autoResultMap = true)
public class PushRecord extends BaseEntity {

    /** 推送批次号 */
    private String pushBatchNo;

    // ===== 来源信息 =====
    
    /** 来源编码 */
    private String sourceCode;
    
    /** 来源类型 */
    private String sourceType;
    
    /** 来源业务ID */
    private String sourceId;
    
    /** 来源业务编号 */
    private String sourceNo;

    // ===== 档案关联 =====
    
    /** 档案ID */
    private Long archiveId;
    
    /** 档案号 */
    private String archiveNo;
    
    /** 档案标题 */
    private String title;

    // ===== 推送状态 =====
    
    /** 推送状态 */
    private String pushStatus;
    
    /** 文件处理状态 */
    private String fileStatus;

    // ===== 文件统计 =====
    
    /** 总文件数 */
    private Integer totalFiles;
    
    /** 成功文件数 */
    private Integer successFiles;
    
    /** 失败文件数 */
    private Integer failedFiles;

    // ===== 请求信息 =====
    
    /** 请求内容 */
    @JsonIgnore
    @TableField(typeHandler = PostgreSQLJsonbTypeHandler.class)
    private Map<String, Object> requestPayload;
    
    /** 回调地址 */
    @JsonIgnore
    private String callbackUrl;

    // ===== 错误信息 =====
    
    /** 错误代码 */
    private String errorCode;
    
    /** 错误信息 */
    private String errorMessage;

    // ===== 时间信息 =====
    
    /** 推送时间 */
    private LocalDateTime pushedAt;
    
    /** 处理完成时间 */
    private LocalDateTime processedAt;

    // ===== 状态常量 =====
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PARTIAL = "PARTIAL";

    public static final String FILE_STATUS_PENDING = "PENDING";
    public static final String FILE_STATUS_CALLBACK_PENDING = "CALLBACK_PENDING";
    public static final String FILE_STATUS_CALLBACK_SENT = "CALLBACK_SENT";
}
