package com.archivesystem.dto.role;

import com.archivesystem.entity.Role;
import lombok.Builder;
import lombok.Value;

/**
 * 角色选项响应 DTO.
 */
@Value
@Builder
public class RoleOptionResponse {

    Long id;
    String roleName;
    String description;
    String status;

    public static RoleOptionResponse from(Role role) {
        if (role == null) {
            return null;
        }

        return RoleOptionResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .build();
    }
}
