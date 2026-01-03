package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 晋升申请实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_promotion_application")
public class PromotionApplication extends BaseEntity {

    /**
     * 申请编号
     */
    private String applicationNo;

    /**
     * 申请人ID
     */
    private Long employeeId;

    /**
     * 申请人姓名
     */
    private String employeeName;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

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
     * 申请理由
     */
    private String applyReason;

    /**
     * 主要业绩
     */
    private String achievements;

    /**
     * 自我评价
     */
    private String selfEvaluation;

    /**
     * 附件列表（JSON数组）
     */
    private String attachments;

    /**
     * 状态：PENDING-待审批, REVIEWING-评审中, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消
     */
    private String status;

    /**
     * 评审得分
     */
    private BigDecimal reviewScore;

    /**
     * 评审结果：PASS-通过, FAIL-不通过
     */
    private String reviewResult;

    /**
     * 评审意见
     */
    private String reviewComment;

    /**
     * 审批人ID
     */
    private Long approvedBy;

    /**
     * 审批人姓名
     */
    private String approvedByName;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 申请日期
     */
    private LocalDate applyDate;
}
