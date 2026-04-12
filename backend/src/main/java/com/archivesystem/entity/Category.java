package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 档案分类实体类.
 * 对应数据库表: arc_category
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("arc_category")
public class Category extends BaseEntity {

    /** 父分类ID */
    private Long parentId;
    
    /** 分类号 */
    @NotBlank(message = "分类号不能为空")
    @Size(max = 50, message = "分类号长度不能超过50个字符")
    private String categoryCode;
    
    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    private String categoryName;
    
    /** 档案门类 */
    @NotBlank(message = "档案门类不能为空")
    private String archiveType;
    
    /** 层级 */
    @Builder.Default
    private Integer level = 1;
    
    /** 排序 */
    @Builder.Default
    private Integer sortOrder = 0;
    
    /** 默认保管期限 */
    private String retentionPeriod;
    
    /** 说明 */
    private String description;
    
    /** 状态 */
    @Builder.Default
    private String status = STATUS_ACTIVE;
    
    /** 完整路径 */
    private String fullPath;

    /** 子分类列表（非数据库字段） */
    @TableField(exist = false)
    private List<Category> children;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    
    public static final String TYPE_DOCUMENT = "DOCUMENT";
    public static final String TYPE_SCIENCE = "SCIENCE";
    public static final String TYPE_ACCOUNTING = "ACCOUNTING";
    public static final String TYPE_PERSONNEL = "PERSONNEL";
    public static final String TYPE_SPECIAL = "SPECIAL";
    public static final String TYPE_AUDIOVISUAL = "AUDIOVISUAL";
}
