package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 质量检查DTO（M10-031）. */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityCheckDTO extends BaseDTO {

  /** 检查编号 */
  private String checkNo;

  /** 项目ID */
  private Long matterId;

  /** 项目名称 */
  private String matterName;

  /** 检查人ID */
  private Long checkerId;

  /** 检查人姓名 */
  private String checkerName;

  /** 检查日期 */
  private LocalDate checkDate;

  /** 检查类型 */
  private String checkType;

  /** 检查类型名称 */
  private String checkTypeName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 总分 */
  private BigDecimal totalScore;

  /** 是否合格 */
  private Boolean qualified;

  /** 检查摘要 */
  private String checkSummary;

  /** 检查详情列表 */
  private List<QualityCheckDetailDTO> details;
}
