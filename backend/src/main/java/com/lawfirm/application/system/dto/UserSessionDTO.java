package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户会话 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSessionDTO extends BaseDTO {

    private Long userId;
    private String username;
    private String userRealName;
    private String ipAddress;
    private String userAgent;
    private String deviceType;
    private String deviceTypeName;
    private String browser;
    private String os;
    private String location;
    private LocalDateTime loginTime;
    private LocalDateTime lastAccessTime;
    private LocalDateTime expireTime;
    private String status;
    private String statusName;
    private Boolean isCurrent;
}

