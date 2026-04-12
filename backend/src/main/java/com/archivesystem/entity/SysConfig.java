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
 * 系统配置实体类
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_config")
public class SysConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 值类型：STRING, NUMBER, BOOLEAN, JSON */
    private String configType;

    /** 配置分组 */
    private String configGroup;

    /** 配置描述 */
    private String description;

    /** 是否可编辑 */
    @Builder.Default
    private Boolean editable = true;

    /** 排序 */
    @Builder.Default
    private Integer sortOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    // 配置类型常量
    public static final String TYPE_STRING = "STRING";
    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_BOOLEAN = "BOOLEAN";
    public static final String TYPE_JSON = "JSON";

    // 配置分组常量
    public static final String GROUP_ARCHIVE_NO = "ARCHIVE_NO";
    public static final String GROUP_RETENTION = "RETENTION";
    public static final String GROUP_SYSTEM = "SYSTEM";
}
