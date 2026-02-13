package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 案源查询 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeadQueryDTO extends PageQuery {

  /** 案源名称 */
  private String leadName;

  /** 状态 */
  private String status;

  /** 创建人ID */
  private Long originatorId;

  /** 负责人ID */
  private Long responsibleUserId;

  /** 来源渠道 */
  private String sourceChannel;
}
