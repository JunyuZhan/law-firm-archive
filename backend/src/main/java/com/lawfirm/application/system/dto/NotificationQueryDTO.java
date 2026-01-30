package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 通知查询参数. */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQueryDTO extends PageQuery {
  /** 通知类型 */
  private String type;

  /** 是否已读 */
  private Boolean isRead;
}
