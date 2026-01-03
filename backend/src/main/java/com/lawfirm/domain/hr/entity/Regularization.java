package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 转正申请实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_regularization")
public class Regularization extends BaseEntity {

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 申请编号
     */
    private String applicationNo;

    /**
     * 试用期开始日期
     */
    private LocalDate probationStartDate;

    /**
     * 试用期结束日期
     */
    private LocalDate probationEndDate;

    /**
     * 申请日期
     */
    private LocalDate applicationDate;

    /**
     * 预计转正日期
     */
    private LocalDate expectedRegularDate;

    /**
     * 自我评价
     */
    private String selfEvaluation;

    /**
     * 上级评价
     */
    private String supervisorEvaluation;

    /**
     * HR评价
     */
    private String hrEvaluation;

    /**
     * 状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝
     */
    private String status;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批日期
     */
    private LocalDate approvedDate;

    /**
     * 审批意见
     */
    private String comment;
}

