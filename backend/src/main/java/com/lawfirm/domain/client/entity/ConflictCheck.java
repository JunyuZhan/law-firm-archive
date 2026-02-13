package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 利益冲突检查实体。 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_conflict_check")
public class ConflictCheck extends BaseEntity {

  /** 检查编号 */
  private String checkNo;

  /** 检查类型：NEW_CLIENT-新客户, NEW_MATTER-新案件, MANUAL-手动检查 */
  private String checkType;

  /** 关联客户ID */
  private Long clientId;

  /** 关联案件ID */
  private Long matterId;

  /** 客户名称（检查时快照） */
  private String clientName;

  /** 对方当事人 */
  private String opposingParty;

  /** 相关方列表（JSON） */
  private String relatedParties;

  /**
   * 检查状态：PENDING-待检查, CHECKING-检查中, PASSED-通过, CONFLICT-存在冲突, EXEMPTION_PENDING-豁免待审批, WAIVED-已豁免,
   * REJECTED-已拒绝
   */
  private String status;

  /** 检查结果说明 */
  private String resultDescription;

  /** 冲突详情（JSON） */
  private String conflictDetails;

  /** 申请人ID */
  private Long applicantId;

  /** 审核人ID */
  private Long reviewerId;

  /** 审核时间 */
  private LocalDateTime reviewedAt;

  /** 审核意见 */
  private String reviewComment;

  /** 备注 */
  private String remark;
}
