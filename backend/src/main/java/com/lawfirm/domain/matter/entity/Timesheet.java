package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("timesheet")
public class Timesheet extends BaseEntity {

    /**
     * 工时编号
     */
    private String timesheetNo;

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 工作日期
     */
    private LocalDate workDate;

    /**
     * 工时（小时）
     */
    private BigDecimal hours;

    /**
     * 工作类型
     */
    private String workType;

    /**
     * 工作内容
     */
    private String workContent;

    /**
     * 是否计费
     */
    @lombok.Builder.Default
    private Boolean billable = true;

    /**
     * 小时费率
     */
    private BigDecimal hourlyRate;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 状态
     */
    @lombok.Builder.Default
    private String status = "DRAFT"; // 使用 TimesheetStatus.DRAFT

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 审批人ID
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批意见
     */
    private String approvalComment;
}
