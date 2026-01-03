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
 * 考核指标实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_performance_indicator")
public class PerformanceIndicator extends BaseEntity {

    /**
     * 指标名称
     */
    private String name;

    /**
     * 指标编码
     */
    private String code;

    /**
     * 指标分类：WORK-工作业绩, ABILITY-能力素质, ATTITUDE-工作态度, OTHER-其他
     */
    private String category;

    /**
     * 指标描述
     */
    private String description;

    /**
     * 权重（百分比）
     */
    private BigDecimal weight;

    /**
     * 满分
     */
    private Integer maxScore;

    /**
     * 评分标准说明
     */
    private String scoringCriteria;

    /**
     * 适用角色：ALL-全部, LAWYER-律师, ASSISTANT-助理, ADMIN-行政
     */
    private String applicableRole;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态：ACTIVE-启用, INACTIVE-停用
     */
    private String status;

    /**
     * 备注
     */
    private String remarks;
}
