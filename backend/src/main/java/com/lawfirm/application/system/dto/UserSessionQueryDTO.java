package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户会话查询 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSessionQueryDTO extends PageQuery {

  /** 用户ID. */
  private Long userId;

  /** 用户名. */
  private String username;

  /** 状态. */
  private String status;

  /** IP地址. */
  private String ipAddress;
}
