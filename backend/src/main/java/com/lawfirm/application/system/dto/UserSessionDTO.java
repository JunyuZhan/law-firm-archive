package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户会话 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSessionDTO extends BaseDTO {

  /** 用户ID. */
  private Long userId;

  /** 用户名. */
  private String username;

  /** 用户真实姓名. */
  private String userRealName;

  /** IP地址. */
  private String ipAddress;

  /** User-Agent. */
  private String userAgent;

  /** 设备类型. */
  private String deviceType;

  /** 设备类型名称. */
  private String deviceTypeName;

  /** 浏览器. */
  private String browser;

  /** 操作系统. */
  private String os;

  /** 地理位置. */
  private String location;

  /** 登录时间. */
  private LocalDateTime loginTime;

  /** 最后访问时间. */
  private LocalDateTime lastAccessTime;

  /** 过期时间. */
  private LocalDateTime expireTime;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 是否当前会话. */
  private Boolean isCurrent;
}
