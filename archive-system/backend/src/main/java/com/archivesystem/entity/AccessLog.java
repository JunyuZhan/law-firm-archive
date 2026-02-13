package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 访问日志实体类.
 * 对应数据库表: arc_access_log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_access_log")
public class AccessLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 档案ID */
    private Long archiveId;
    
    /** 文件ID */
    private Long fileId;
    
    /** 访问类型 */
    private String accessType;
    
    /** 访问IP */
    private String accessIp;
    
    /** User Agent */
    private String accessUa;
    
    /** 用户ID */
    private Long userId;
    
    /** 用户姓名 */
    private String userName;
    
    /** 访问时间 */
    @Builder.Default
    private LocalDateTime accessedAt = LocalDateTime.now();

    public static final String TYPE_VIEW = "VIEW";
    public static final String TYPE_DOWNLOAD = "DOWNLOAD";
    public static final String TYPE_PRINT = "PRINT";
    public static final String TYPE_PREVIEW = "PREVIEW";
}
