package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 问题整改实体（M10-032） */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("quality_issue")
public class QualityIssue extends BaseEntity {

  /** 问题编号 */
  private String issueNo;

  /** 关联检查ID */
  private Long checkId;

  /** 项目ID */
  private Long matterId;

  /** 问题类型：CRITICAL-严重, MAJOR-重要, MINOR-一般 */
  private String issueType;

  /** 问题描述 */
  private String issueDescription;

  /** 责任人ID */
  private Long responsibleUserId;

  /** 状态：OPEN-待整改, IN_PROGRESS-整改中, RESOLVED-已解决, CLOSED-已关闭 */
  private String status;

  /** 优先级：HIGH-高, MEDIUM-中, LOW-低 */
  private String priority;

  /** 整改期限 */
  private LocalDate dueDate;

  /** 整改措施 */
  private String resolution;

  /** 解决时间 */
  private LocalDateTime resolvedAt;

  /** 解决人ID */
  private Long resolvedBy;

  /** 验证时间 */
  private LocalDateTime verifiedAt;

  /** 验证人ID */
  private Long verifiedBy;

  /** 问题类型：严重 */
  public static final String TYPE_CRITICAL = "CRITICAL";

  /** 问题类型：重要 */
  public static final String TYPE_MAJOR = "MAJOR";

  /** 问题类型：一般 */
  public static final String TYPE_MINOR = "MINOR";

  /** 状态：待整改 */
  public static final String STATUS_OPEN = "OPEN";

  /** 状态：整改中 */
  public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

  /** 状态：已解决 */
  public static final String STATUS_RESOLVED = "RESOLVED";

  /** 状态：已关闭 */
  public static final String STATUS_CLOSED = "CLOSED";

  /** 优先级：高 */
  public static final String PRIORITY_HIGH = "HIGH";

  /** 优先级：中 */
  public static final String PRIORITY_MEDIUM = "MEDIUM";

  /** 优先级：低 */
  public static final String PRIORITY_LOW = "LOW";
}
