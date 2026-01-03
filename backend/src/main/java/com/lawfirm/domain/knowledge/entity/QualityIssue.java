package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 问题整改实体（M10-032）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("quality_issue")
public class QualityIssue extends BaseEntity {

    /**
     * 问题编号
     */
    private String issueNo;

    /**
     * 关联检查ID
     */
    private Long checkId;

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 问题类型：CRITICAL-严重, MAJOR-重要, MINOR-一般
     */
    private String issueType;

    /**
     * 问题描述
     */
    private String issueDescription;

    /**
     * 责任人ID
     */
    private Long responsibleUserId;

    /**
     * 状态：OPEN-待整改, IN_PROGRESS-整改中, RESOLVED-已解决, CLOSED-已关闭
     */
    private String status;

    /**
     * 优先级：HIGH-高, MEDIUM-中, LOW-低
     */
    private String priority;

    /**
     * 整改期限
     */
    private LocalDate dueDate;

    /**
     * 整改措施
     */
    private String resolution;

    /**
     * 解决时间
     */
    private java.time.LocalDateTime resolvedAt;

    /**
     * 解决人ID
     */
    private Long resolvedBy;

    /**
     * 验证时间
     */
    private java.time.LocalDateTime verifiedAt;

    /**
     * 验证人ID
     */
    private Long verifiedBy;

    public static final String TYPE_CRITICAL = "CRITICAL";
    public static final String TYPE_MAJOR = "MAJOR";
    public static final String TYPE_MINOR = "MINOR";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_LOW = "LOW";
}

