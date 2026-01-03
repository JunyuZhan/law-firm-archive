package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 案例学习笔记实体（M10-013）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("case_study_note")
public class CaseStudyNote extends BaseEntity {

    /**
     * 案例ID
     */
    private Long caseId;

    /**
     * 学习人ID
     */
    private Long userId;

    /**
     * 学习笔记内容
     */
    private String noteContent;

    /**
     * 关键要点
     */
    private String keyPoints;

    /**
     * 个人见解
     */
    private String personalInsights;
}

