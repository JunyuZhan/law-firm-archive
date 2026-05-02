package com.archivesystem.dto.user;

import lombok.Builder;
import lombok.Value;

/**
 * 用户账号锁定状态响应 DTO.
 */
@Value
@Builder
public class UserLockStatusResponse {

    boolean locked;
    long remainingLockoutSeconds;
    int remainingAttempts;
}
