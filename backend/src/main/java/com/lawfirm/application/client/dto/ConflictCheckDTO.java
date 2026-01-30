package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 利冲检查 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConflictCheckDTO extends BaseDTO {

  /** 检查编号 */
  private String checkNo;

  /** 案件ID */
  private Long matterId;

  /** 案件名称 */
  private String matterName;

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 对方当事人 */
  private String opposingParty;

  /** 检查类型 */
  private String checkType;

  /** 检查类型名称 */
  private String checkTypeName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 检查结果 */
  private String result;

  /** 检查结果名称 */
  private String resultName;

  /** 申请人ID */
  private Long applicantId;

  /** 申请人名称 */
  private String applicantName;

  /** 申请时间 */
  private LocalDateTime applyTime;

  /** 审核人ID */
  private Long reviewerId;

  /** 审核人名称 */
  private String reviewerName;

  /** 审核时间 */
  private LocalDateTime reviewTime;

  /** 审核意见 */
  private String reviewComment;

  /** 备注 */
  private String remark;

  /** 检查项列表 */
  private List<ConflictCheckItemDTO> items;
}
