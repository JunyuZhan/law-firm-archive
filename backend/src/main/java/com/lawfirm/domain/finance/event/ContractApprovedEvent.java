package com.lawfirm.domain.finance.event;

import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 合同审批通过事件 触发时机：ContractAppService.approve() 方法执行成功后
 *
 * <p>Requirements: 1.1, 1.2
 */
@Getter
public class ContractApprovedEvent extends ApplicationEvent {

  /** 合同ID */
  private final Long contractId;

  /** 审批人ID */
  private final Long approverId;

  /** 审批时间 */
  private final LocalDateTime approvedAt;

  /**
   * 构造函数。
   *
   * @param source 事件源
   * @param contractId 合同ID
   * @param approverId 审批人ID
   */
  public ContractApprovedEvent(final Object source, final Long contractId, final Long approverId) {
    super(source);
    this.contractId = contractId;
    this.approverId = approverId;
    this.approvedAt = LocalDateTime.now();
  }
}
