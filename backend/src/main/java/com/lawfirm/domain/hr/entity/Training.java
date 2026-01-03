package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 培训计划实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_training")
public class Training extends BaseEntity {

    /**
     * 培训标题
     */
    private String title;

    /**
     * 培训类型：INTERNAL-内部培训, EXTERNAL-外部培训, ONLINE-在线培训
     */
    private String trainingType;

    /**
     * 培训分类：LAW-法律知识, SKILL-业务技能, MANAGEMENT-管理能力, OTHER-其他
     */
    private String category;

    /**
     * 培训描述
     */
    private String description;

    /**
     * 讲师/培训机构
     */
    private String trainer;

    /**
     * 培训地点
     */
    private String location;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 培训时长（小时）
     */
    private Integer duration;

    /**
     * 学分
     */
    private Integer credits;

    /**
     * 最大参与人数
     */
    private Integer maxParticipants;

    /**
     * 当前报名人数
     */
    private Integer currentParticipants;

    /**
     * 报名截止日期
     */
    private LocalDate enrollDeadline;

    /**
     * 状态：DRAFT-草稿, PUBLISHED-已发布, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消
     */
    private String status;

    /**
     * 培训材料URL
     */
    private String materialsUrl;

    /**
     * 备注
     */
    private String remarks;
}
