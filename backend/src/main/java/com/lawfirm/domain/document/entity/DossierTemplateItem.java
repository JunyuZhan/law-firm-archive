package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 卷宗目录项实体
 * 模板中的具体目录项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dossier_template_item")
public class DossierTemplateItem {

    private Long id;

    /**
     * 所属模板ID
     */
    private Long templateId;

    /**
     * 父目录ID，0表示顶级
     */
    @Builder.Default
    private Long parentId = 0L;

    /**
     * 目录名称
     */
    private String name;

    /**
     * 类型: FOLDER-目录, FILE-文件占位
     */
    @Builder.Default
    private String itemType = "FOLDER";

    /**
     * 对应的文件分类
     */
    private String fileCategory;

    /**
     * 排序号
     */
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 是否必需
     */
    @Builder.Default
    private Boolean required = false;

    /**
     * 说明
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

