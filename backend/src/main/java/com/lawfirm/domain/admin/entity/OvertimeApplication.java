package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 加班申请实体（M8-004）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("overtime_application")
public class OvertimeApplication extends BaseEntity {

    /**
     * 申请编号
     */
    private String applicationNo;

    /**
     * 申请人ID
     */
    private Long userId;

    /**
     * 加班日期
     */
    private LocalDate overtimeDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 加班时长(小时)
     */
    private BigDecimal overtimeHours;

    /**
     * 加班原因
     */
    private String reason;

    /**
     * 工作内容
     */
    private String workContent;

    /**
     * 状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝
     */
    private String status;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批时间
     */
    private java.time.LocalDateTime approvedAt;

    /**
     * 审批意见
     */
    private String approvalComment;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
}

