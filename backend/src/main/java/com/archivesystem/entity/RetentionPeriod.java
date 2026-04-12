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
 * 保管期限实体类.
 * 对应数据库表: arc_retention_period
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_retention_period")
public class RetentionPeriod implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 期限代码 */
    private String periodCode;
    
    /** 期限名称 */
    private String periodName;
    
    /** 年数（永久为NULL） */
    private Integer periodYears;
    
    /** 说明 */
    private String description;
    
    /** 排序 */
    @Builder.Default
    private Integer sortOrder = 0;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public static final String PERMANENT = "PERMANENT";
    public static final String Y30 = "Y30";
    public static final String Y15 = "Y15";
    public static final String Y10 = "Y10";
    public static final String Y5 = "Y5";
}
