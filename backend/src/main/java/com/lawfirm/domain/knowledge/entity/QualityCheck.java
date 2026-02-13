package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 质量检查实体（M10-031） */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("quality_check")
public class QualityCheck extends BaseEntity {

  /** 检查编号 */
  private String checkNo;

  /** 项目ID */
  private Long matterId;

  /** 检查人ID */
  private Long checkerId;

  /** 检查日期 */
  private LocalDate checkDate;

  /** 检查类型：ROUTINE-常规, RANDOM-随机, SPECIAL-专项 */
  private String checkType;

  /** 状态：IN_PROGRESS-进行中, COMPLETED-已完成 */
  private String status;

  /** 总分 */
  private BigDecimal totalScore;

  /** 是否合格 */
  private Boolean qualified;

  /** 检查总结 */
  private String checkSummary;

  /** 检查类型：常规 */
  public static final String TYPE_ROUTINE = "ROUTINE";

  /** 检查类型：随机 */
  public static final String TYPE_RANDOM = "RANDOM";

  /** 检查类型：专项 */
  public static final String TYPE_SPECIAL = "SPECIAL";

  /** 状态：进行中 */
  public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

  /** 状态：已完成 */
  public static final String STATUS_COMPLETED = "COMPLETED";
}
