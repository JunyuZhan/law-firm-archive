package com.archivesystem.dto.user;

import com.archivesystem.entity.User;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 当前用户资料响应 DTO.
 */
@Value
@Builder
public class CurrentUserProfileResponse {
    String username;
    String realName;
    String email;
    String phone;
    String department;
    String status;
    LocalDateTime lastLoginAt;
    String lastLoginIp;

    public static CurrentUserProfileResponse from(User user) {
        if (user == null) {
            return null;
        }

        return CurrentUserProfileResponse.builder()
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .build();
    }
}
