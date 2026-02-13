package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志实体类.
 * 对应数据库表: arc_operation_log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "arc_operation_log", autoResultMap = true)
public class OperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    // ===== 操作对象 =====
    
    /** 档案ID */
    private Long archiveId;
    
    /** 对象类型 */
    private String objectType;
    
    /** 对象ID */
    private String objectId;

    // ===== 操作信息 =====
    
    /** 操作类型 */
    private String operationType;
    
    /** 操作描述 */
    private String operationDesc;
    
    /** 操作详情 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> operationDetail;

    // ===== 操作人信息 =====
    
    /** 操作人ID */
    private Long operatorId;
    
    /** 操作人姓名 */
    private String operatorName;
    
    /** 操作人IP */
    private String operatorIp;
    
    /** User Agent */
    private String operatorUa;
    
    /** 操作时间 */
    @Builder.Default
    private LocalDateTime operatedAt = LocalDateTime.now();

    // ===== 对象类型常量 =====
    
    public static final String OBJ_ARCHIVE = "ARCHIVE";
    public static final String OBJ_FILE = "FILE";
    public static final String OBJ_BORROW = "BORROW";
    public static final String OBJ_APPRAISAL = "APPRAISAL";
    public static final String OBJ_SYSTEM = "SYSTEM";

    // ===== 操作类型常量 =====
    
    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";
    public static final String OP_VIEW = "VIEW";
    public static final String OP_DOWNLOAD = "DOWNLOAD";
    public static final String OP_PRINT = "PRINT";
    public static final String OP_EXPORT = "EXPORT";
}
