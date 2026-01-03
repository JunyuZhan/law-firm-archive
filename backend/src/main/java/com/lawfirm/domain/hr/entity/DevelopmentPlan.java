package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 个人发展规划实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_development_plan")
public class DevelopmentPlan extends BaseEntity {

    /**
     * 规划编号
     */
    private String planNo;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 规划年度
     */
    private Integer planYear;

    /**
     * 规划标题
     */
    private String planTitle;

    /**
     * 当前职级ID
     */
    private Long currentLevelId;

    /**
     * 当前职级名称
     */
    private String currentLevelName;

    /**
     * 目标职级ID
     */
    private Long targetLevelId;

    /**
     * 目标职级名称
     */
    private String targetLevelName;

    /**
     * 目标达成日期
     */
    private LocalDate targetDate;

    /**
     * 职业目标（JSON数组）
     */
    private String careerGoals;

    /**
     * 技能目标（JSON数组）
     */
    private String skillGoals;

    /**
     * 业绩目标（JSON数组）
     */
    private String performanceGoals;

    /**
     * 行动计划（JSON数组）
     */
    private String actionPlans;

    /**
     * 所需培训
     */
    private String requiredTraining;

    /**
     * 所需资源
     */
    private String requiredResources;

    /**
     * 导师ID
     */
    private Long mentorId;

    /**
     * 导师姓名
     */
    private String mentorName;

    /**
     * 完成进度百分比
     */
    private Integer progressPercentage;

    /**
     * 进度备注
     */
    private String progressNotes;

    /**
     * 状态：DRAFT-草稿, ACTIVE-执行中, COMPLETED-已完成, CANCELLED-已取消
     */
    private String status;

    /**
     * 审核人ID
     */
    private Long reviewedBy;

    /**
     * 审核人姓名
     */
    private String reviewedByName;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 审核意见
     */
    private String reviewComment;
}
