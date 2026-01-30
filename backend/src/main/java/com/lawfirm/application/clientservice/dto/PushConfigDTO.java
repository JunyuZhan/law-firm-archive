package com.lawfirm.application.clientservice.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** 推送配置 DTO */
@Data
@Builder
public class PushConfigDTO {

  /** ID */
  private Long id;

  /** 项目ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 是否启用推送 */
  private Boolean enabled;

  /** 推送范围 */
  private List<String> scopes;

  /** 项目更新时自动推送 */
  private Boolean autoPushOnUpdate;

  /** 数据有效期（天） */
  private Integer validDays;

  /** 客户服务系统是否已连接（外部集成配置是否存在且启用） */
  private Boolean clientServiceConnected;

  /** 客户服务系统连接信息（如未连接则显示原因） */
  private String connectionMessage;
}
