package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 出函模板实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("letter_template")
public class LetterTemplate extends BaseEntity {

    /**
     * 模板编号
     */
    private String templateNo;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 函件类型：INTRODUCTION-介绍信, MEETING-会见函, INVESTIGATION-调查函, 
     * FILE_REVIEW-阅卷函, LEGAL_OPINION-法律意见函, OTHER-其他
     */
    private String letterType;

    /**
     * 模板内容（支持变量）
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
     * 排序
     */
    @lombok.Builder.Default
    private Integer sortOrder = 0;
}
