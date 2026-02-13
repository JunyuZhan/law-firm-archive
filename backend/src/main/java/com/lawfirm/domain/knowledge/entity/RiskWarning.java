package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 风险预警实体（M10-033） */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("risk_warning")
public class RiskWarning extends BaseEntity {

  /** 预警编号 */
  private String warningNo;

  /** 项目ID */
  private Long matterId;

  /** 风险类型：DEADLINE-期限风险, QUALITY-质量风险, COST-成本风险, LEGAL-法律风险, OTHER-其他 */
  private String riskType;

  /** 风险等级：HIGH-高, MEDIUM-中, LOW-低 */
  private String riskLevel;

  /** 风险描述 */
  private String riskDescription;

  /** 预警原因 */
  private String warningReason;

  /** 建议措施 */
  private String suggestedAction;

  /** 状态：ACTIVE-活跃, ACKNOWLEDGED-已确认, RESOLVED-已解决, CLOSED-已关闭 */
  private String status;

  /** 确认时间 */
  private LocalDateTime acknowledgedAt;

  /** 确认人ID */
  private Long acknowledgedBy;

  /** 解决时间 */
  private LocalDateTime resolvedAt;

  /** 解决人ID */
  private Long resolvedBy;

  /** 风险类型：期限风险 */
  public static final String TYPE_DEADLINE = "DEADLINE";

  /** 风险类型：质量风险 */
  public static final String TYPE_QUALITY = "QUALITY";

  /** 风险类型：成本风险 */
  public static final String TYPE_COST = "COST";

  /** 风险类型：法律风险 */
  public static final String TYPE_LEGAL = "LEGAL";

  /** 风险类型：其他 */
  public static final String TYPE_OTHER = "OTHER";

  /** 风险等级：高 */
  public static final String LEVEL_HIGH = "HIGH";

  /** 风险等级：中 */
  public static final String LEVEL_MEDIUM = "MEDIUM";

  /** 风险等级：低 */
  public static final String LEVEL_LOW = "LOW";

  /** 状态：活跃 */
  public static final String STATUS_ACTIVE = "ACTIVE";

  /** 状态：已确认 */
  public static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";

  /** 状态：已解决 */
  public static final String STATUS_RESOLVED = "RESOLVED";

  /** 状态：已关闭 */
  public static final String STATUS_CLOSED = "CLOSED";
}
