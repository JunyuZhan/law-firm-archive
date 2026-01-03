package com.lawfirm.application.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {

    private Long id;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 描述
     */
    private String description;

    /**
     * 数据范围
     */
    private String dataScope;

    /**
     * 状态
     */
    private String status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 菜单ID列表
     */
    private List<Long> menuIds;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
