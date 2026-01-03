package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 绩效评价实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_performance_evaluation")
public class PerformanceEvaluation extends BaseEntity {

    /**
     * 考核任务ID
     */
    private Long taskId;

    /**
     * 被考核人ID
     */
    private Long employeeId;

    /**
     * 评价人ID
     */
    private Long evaluatorId;

    /**
     * 评价类型：SELF-自评, PEER-互评, SUPERVISOR-上级评价
     */
    private String evaluationType;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 评价等级：A-优秀, B-良好, C-合格, D-待改进, E-不合格
     */
    private String grade;

    /**
     * 综合评语
     */
    private String comment;

    /**
     * 优点
     */
    private String strengths;

    /**
     * 改进建议
     */
    private String improvements;

    /**
     * 评价时间
     */
    private LocalDateTime evaluatedAt;

    /**
     * 状态：PENDING-待评价, COMPLETED-已完成
     */
    private String status;
}
