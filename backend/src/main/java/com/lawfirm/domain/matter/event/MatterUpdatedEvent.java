package com.lawfirm.domain.matter.event;

import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** 项目更新事件 触发时机：MatterAppService.updateMatter() 方法执行成功后 用于触发自动推送等后续操作 */
@Getter
public class MatterUpdatedEvent extends ApplicationEvent {

  /** 项目ID */
  private final Long matterId;

  /** 客户ID */
  private final Long clientId;

  /** 操作人ID */
  private final Long operatorId;

  /** 更新时间 */
  private final LocalDateTime updatedAt;

  /**
   * 构造函数。
   *
   * @param source 事件源
   * @param matterId 项目ID
   * @param clientId 客户ID
   * @param operatorId 操作人ID
   */
  public MatterUpdatedEvent(
      final Object source, final Long matterId, final Long clientId, final Long operatorId) {
    super(source);
    this.matterId = matterId;
    this.clientId = clientId;
    this.operatorId = operatorId;
    this.updatedAt = LocalDateTime.now();
  }
}
