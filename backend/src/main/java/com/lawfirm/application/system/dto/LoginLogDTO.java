package com.lawfirm.application.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志 DTO
 */
@Data
public class LoginLogDTO {

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String loginIp;
    private String loginLocation;
    private String userAgent;
    private String browser;
    private String os;
    private String deviceType;
    private String status;
    private String message;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private LocalDateTime createdAt;
}

