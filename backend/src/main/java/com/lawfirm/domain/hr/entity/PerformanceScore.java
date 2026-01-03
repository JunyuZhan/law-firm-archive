package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 绩效评分明细实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_performance_score")
public class PerformanceScore extends BaseEntity {

    /**
     * 评价ID
     */
    private Long evaluationId;

    /**
     * 指标ID
     */
    private Long indicatorId;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 评语
     */
    private String comment;
}
