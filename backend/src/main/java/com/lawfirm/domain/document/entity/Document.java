package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 文档实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "doc_document", autoResultMap = true)
public class Document extends BaseEntity {

    /**
     * 文档编号
     */
    private String docNo;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 关联案件ID
     */
    private Long matterId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（扩展名）
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 版本号
     */
    @lombok.Builder.Default
    private Integer version = 1;

    /**
     * 是否最新版本
     */
    @lombok.Builder.Default
    private Boolean isLatest = true;

    /**
     * 父文档ID（用于版本关联）
     */
    private Long parentDocId;

    /**
     * 安全级别：PUBLIC-公开, INTERNAL-内部, CONFIDENTIAL-机密, TOP_SECRET-绝密
     */
    @lombok.Builder.Default
    private String securityLevel = "INTERNAL";

    /**
     * 文档阶段（关联案件阶段）
     */
    private String stage;

    /**
     * 标签
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：ACTIVE-正常, ARCHIVED-已归档, DELETED-已删除
     */
    @lombok.Builder.Default
    private String status = "ACTIVE";
}
