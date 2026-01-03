package com.lawfirm.domain.matter.entity;

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
 * 期限提醒实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("matter_deadline")
public class Deadline extends BaseEntity {

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 期限类型：EVIDENCE_SUBMISSION-举证期, APPEAL-上诉期, REPLY-答辩期, EXECUTION_APPLICATION-执行申请期, HEARING-开庭期, OTHER-其他
     */
    private String deadlineType;

    /**
     * 期限名称
     */
    private String deadlineName;

    /**
     * 基准日期（如立案日期、开庭日期、判决日期等）
     */
    private LocalDate baseDate;

    /**
     * 期限日期
     */
    private LocalDate deadlineDate;

    /**
     * 提前提醒天数（默认7天）
     */
    private Integer reminderDays;

    /**
     * 是否已发送提醒
     */
    private Boolean reminderSent;

    /**
     * 提醒发送时间
     */
    private LocalDateTime reminderSentAt;

    /**
     * 状态：ACTIVE-有效, COMPLETED-已完成, EXPIRED-已过期, CANCELLED-已取消
     */
    private String status;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 完成人ID
     */
    private Long completedBy;

    /**
     * 期限说明
     */
    private String description;
}

