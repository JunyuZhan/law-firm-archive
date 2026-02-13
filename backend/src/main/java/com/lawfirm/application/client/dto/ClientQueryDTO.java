package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户查询条件 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientQueryDTO extends PageQuery {

  /** 客户名称（模糊） */
  private String name;

  /** 客户类型 */
  private String clientType;

  /** 状态 */
  private String status;

  /** 客户级别 */
  private String level;

  /** 负责律师ID */
  private Long responsibleLawyerId;

  /** 案源人ID */
  private Long originatorId;

  /** 关键字（搜索名称、联系人、电话） */
  private String keyword;
}
