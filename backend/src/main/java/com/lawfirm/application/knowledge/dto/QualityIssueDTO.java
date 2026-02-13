package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 问题整改DTO（M10-032）. */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityIssueDTO extends BaseDTO {

  /** 问题编号 */
  private String issueNo;

  /** 检查ID */
  private Long checkId;

  /** 项目ID */
  private Long matterId;

  /** 项目名称 */
  private String matterName;

  /** 问题类型 */
  private String issueType;

  /** 问题类型名称 */
  private String issueTypeName;

  /** 问题描述 */
  private String issueDescription;

  /** 责任人ID */
  private Long responsibleUserId;

  /** 责任人姓名 */
  private String responsibleUserName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 优先级 */
  private String priority;

  /** 优先级名称 */
  private String priorityName;

  /** 到期日期 */
  private LocalDate dueDate;

  /** 整改方案 */
  private String resolution;

  /** 解决时间 */
  private LocalDateTime resolvedAt;

  /** 解决人ID */
  private Long resolvedBy;

  /** 验证时间 */
  private LocalDateTime verifiedAt;

  /** 验证人ID */
  private Long verifiedBy;
}
