package com.archivesystem.dto.role;

import com.archivesystem.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色响应 DTO.
 */
@Data
@Builder
public class RoleResponse {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public static RoleResponse from(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .createdAt(role.getCreatedAt())
                .build();
    }
}
