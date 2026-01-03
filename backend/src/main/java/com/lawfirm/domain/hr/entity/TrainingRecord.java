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
 * 培训记录实体（员工参与培训的记录）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_training_record")
public class TrainingRecord extends BaseEntity {

    /**
     * 培训ID
     */
    private Long trainingId;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 报名时间
     */
    private LocalDateTime enrollTime;

    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 签退时间
     */
    private LocalDateTime checkOutTime;

    /**
     * 实际参与时长（小时）
     */
    private BigDecimal actualDuration;

    /**
     * 状态：ENROLLED-已报名, ATTENDED-已参加, ABSENT-缺席, CANCELLED-已取消
     */
    private String status;

    /**
     * 考核成绩
     */
    private BigDecimal score;

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 获得学分
     */
    private Integer earnedCredits;

    /**
     * 培训反馈/评价
     */
    private String feedback;

    /**
     * 评分（1-5）
     */
    private Integer rating;

    /**
     * 证书URL
     */
    private String certificateUrl;

    /**
     * 备注
     */
    private String remarks;
}
