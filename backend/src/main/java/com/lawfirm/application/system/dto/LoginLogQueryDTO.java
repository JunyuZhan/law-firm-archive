package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 登录日志查询 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQueryDTO extends PageQuery {

  /** 用户ID. */
  private Long userId;

  /** 用户名. */
  private String username;

  /** 状态. */
  private String status;

  /** 开始时间. */
  private LocalDateTime startTime;

  /** 结束时间. */
  private LocalDateTime endTime;
}
