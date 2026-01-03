package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 风险预警实体（M10-033）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("risk_warning")
public class RiskWarning extends BaseEntity {

    /**
     * 预警编号
     */
    private String warningNo;

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 风险类型：DEADLINE-期限风险, QUALITY-质量风险, COST-成本风险, LEGAL-法律风险, OTHER-其他
     */
    private String riskType;

    /**
     * 风险等级：HIGH-高, MEDIUM-中, LOW-低
     */
    private String riskLevel;

    /**
     * 风险描述
     */
    private String riskDescription;

    /**
     * 预警原因
     */
    private String warningReason;

    /**
     * 建议措施
     */
    private String suggestedAction;

    /**
     * 状态：ACTIVE-活跃, ACKNOWLEDGED-已确认, RESOLVED-已解决, CLOSED-已关闭
     */
    private String status;

    /**
     * 确认时间
     */
    private java.time.LocalDateTime acknowledgedAt;

    /**
     * 确认人ID
     */
    private Long acknowledgedBy;

    /**
     * 解决时间
     */
    private java.time.LocalDateTime resolvedAt;

    /**
     * 解决人ID
     */
    private Long resolvedBy;

    public static final String TYPE_DEADLINE = "DEADLINE";
    public static final String TYPE_QUALITY = "QUALITY";
    public static final String TYPE_COST = "COST";
    public static final String TYPE_LEGAL = "LEGAL";
    public static final String TYPE_OTHER = "OTHER";
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_LOW = "LOW";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CLOSED = "CLOSED";
}

