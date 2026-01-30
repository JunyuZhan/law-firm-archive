package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 财务合同变更记录实体
 *
 * <p>业务说明： 当律师变更合同后，财务模块收到通知，创建此记录。 财务人员需要审核变更内容，决定是否同步更新财务数据。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_contract_amendment")
public class FinanceContractAmendment extends BaseEntity {

  /** 变更编号 */
  private String amendmentNo;

  /** 合同ID */
  private Long contractId;

  /** 变更类型：AMOUNT-金额变更, PARTICIPANT-参与人变更, SCHEDULE-付款计划变更, OTHER-其他 */
  private String amendmentType;

  /** 变更前数据快照（JSON格式） */
  private String beforeSnapshot;

  /** 变更后数据快照（JSON格式） */
  private String afterSnapshot;

  /** 变更说明 */
  private String amendmentReason;

  /** 律师变更人ID */
  private Long lawyerAmendedBy;

  /** 律师变更时间 */
  private LocalDateTime lawyerAmendedAt;

  /** 状态：PENDING-待处理, SYNCED-已同步, IGNORED-已忽略, PARTIAL-部分同步 */
  private String status;

  /** 财务处理人ID */
  private Long financeHandledBy;

  /** 财务处理时间 */
  private LocalDateTime financeHandledAt;

  /** 财务处理备注 */
  private String financeRemark;

  /** 是否影响已有收款（如果有已确认的收款，变更需要特别处理） */
  private Boolean affectsPayments;

  /** 受影响的收款ID列表（JSON数组） */
  private String affectedPaymentIds;
}
