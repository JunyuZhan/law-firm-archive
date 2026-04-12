package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 档案元数据实体类.
 * 对应数据库表: arc_metadata
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_metadata")
public class ArchiveMetadata implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 档案ID */
    private Long archiveId;
    
    /** 字段代码 */
    private String fieldCode;
    
    /** 字段名称 */
    private String fieldName;
    
    /** 字段值 */
    private String fieldValue;
    
    /** 字段类型 */
    @Builder.Default
    private String fieldType = TYPE_TEXT;
    
    /** 排序 */
    @Builder.Default
    private Integer sortOrder = 0;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_BOOLEAN = "BOOLEAN";
}
