package com.lawfirm.domain.evidence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 证据实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("evidence")
public class Evidence extends BaseEntity {

    /**
     * 证据编号
     */
    private String evidenceNo;

    /**
     * 关联案件ID
     */
    private Long matterId;

    /**
     * 证据名称
     */
    private String name;

    /**
     * 证据类型
     */
    private String evidenceType;

    /**
     * 证据来源
     */
    private String source;

    /**
     * 证据分组
     */
    private String groupName;

    /**
     * 排序
     */
    @lombok.Builder.Default
    private Integer sortOrder = 0;

    /**
     * 证明目的
     */
    private String provePurpose;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否原件
     */
    @lombok.Builder.Default
    private Boolean isOriginal = false;

    /**
     * 原件份数
     */
    @lombok.Builder.Default
    private Integer originalCount = 0;

    /**
     * 复印件份数
     */
    @lombok.Builder.Default
    private Integer copyCount = 0;

    /**
     * 起始页码
     */
    private Integer pageStart;

    /**
     * 结束页码
     */
    private Integer pageEnd;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型分类（image/pdf/word/excel/ppt/audio/video/other）
     */
    private String fileType;

    /**
     * 缩略图URL（仅图片文件）
     */
    private String thumbnailUrl;

    /**
     * 质证状态
     */
    @lombok.Builder.Default
    private String crossExamStatus = "PENDING";

    /**
     * 状态
     */
    @lombok.Builder.Default
    private String status = "ACTIVE";
}
