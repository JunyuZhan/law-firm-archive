package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 文档模板实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "doc_template", autoResultMap = true)
public class DocumentTemplate extends BaseEntity {

    /**
     * 模板编号
     */
    private String templateNo;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 模板类型：CONTRACT, LEGAL_OPINION, POWER_OF_ATTORNEY, COMPLAINT, DEFENSE, OTHER
     */
    private String templateType;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 变量定义（逗号分隔的变量名，如: 客户名称,项目名称,承办律师）
     */
    private String variables;

    /**
     * 模板内容（文本格式，包含变量占位符）
     */
    private String content;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：ACTIVE-启用, DISABLED-停用
     */
    @lombok.Builder.Default
    private String status = "ACTIVE";

    /**
     * 使用次数
     */
    @lombok.Builder.Default
    private Integer useCount = 0;
}
