package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 登录日志实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_login_log")
public class LoginLog extends BaseEntity {

  /** 用户ID */
  private Long userId;

  /** 用户名 */
  private String username;

  /** 真实姓名 */
  private String realName;

  /** 登录IP */
  private String loginIp;

  /** 登录地点 */
  private String loginLocation;

  /** 用户代理 */
  private String userAgent;

  /** 浏览器 */
  private String browser;

  /** 操作系统 */
  private String os;

  /** 设备类型：PC, MOBILE, TABLET */
  private String deviceType;

  /** 状态：SUCCESS, FAILURE */
  private String status;

  /** 登录结果消息 */
  private String message;

  /** 登录时间 */
  private LocalDateTime loginTime;

  /** 登出时间 */
  private LocalDateTime logoutTime;
}
