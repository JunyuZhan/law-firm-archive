package com.lawfirm.application.workbench.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** 审批完成事件. */
@Getter
public class ApprovalCompletedEvent extends ApplicationEvent {

  /** 审批ID. */
  private final Long approvalId;

  /** 业务类型. */
  private final String businessType;

  /** 业务ID. */
  private final Long businessId;

  /** 审批结果（APPROVED 或 REJECTED）. */
  private final String result;

  /** 审批意见. */
  private final String comment;

  /**
   * 构造函数
   *
   * @param source 事件源
   * @param approvalId 审批ID
   * @param businessType 业务类型
   * @param businessId 业务ID
   * @param result 审批结果（APPROVED 或 REJECTED）
   * @param comment 审批意见
   */
  public ApprovalCompletedEvent(
      Object source,
      Long approvalId,
      String businessType,
      Long businessId,
      String result,
      String comment) {
    super(source);
    this.approvalId = approvalId;
    this.businessType = businessType;
    this.businessId = businessId;
    this.result = result;
    this.comment = comment;
  }
}
