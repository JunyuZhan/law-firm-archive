package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 风险预警DTO（M10-033）. */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskWarningDTO extends BaseDTO {

  /** 预警编号 */
  private String warningNo;

  /** 项目ID */
  private Long matterId;

  /** 项目名称 */
  private String matterName;

  /** 风险类型 */
  private String riskType;

  /** 风险类型名称 */
  private String riskTypeName;

  /** 风险等级 */
  private String riskLevel;

  /** 风险等级名称 */
  private String riskLevelName;

  /** 风险描述 */
  private String riskDescription;

  /** 预警原因 */
  private String warningReason;

  /** 建议措施 */
  private String suggestedAction;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 确认时间 */
  private LocalDateTime acknowledgedAt;

  /** 确认人ID */
  private Long acknowledgedBy;

  /** 确认人姓名 */
  private String acknowledgedByName;

  /** 解决时间 */
  private LocalDateTime resolvedAt;

  /** 解决人ID */
  private Long resolvedBy;

  /** 解决人姓名 */
  private String resolvedByName;
}
