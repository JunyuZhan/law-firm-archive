package com.lawfirm.application.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 登录日志 DTO. */
@Data
public class LoginLogDTO {

  /** ID. */
  private Long id;

  /** 用户ID. */
  private Long userId;

  /** 用户名. */
  private String username;

  /** 真实姓名. */
  private String realName;

  /** 登录IP. */
  private String loginIp;

  /** 登录地点. */
  private String loginLocation;

  /** User-Agent. */
  private String userAgent;

  /** 浏览器. */
  private String browser;

  /** 操作系统. */
  private String os;

  /** 设备类型. */
  private String deviceType;

  /** 状态. */
  private String status;

  /** 消息. */
  private String message;

  /** 登录时间. */
  private LocalDateTime loginTime;

  /** 登出时间. */
  private LocalDateTime logoutTime;

  /** 创建时间. */
  private LocalDateTime createdAt;
}
