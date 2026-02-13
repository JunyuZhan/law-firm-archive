package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.entity.Role;
import com.archivesystem.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器.
 */
@Tag(name = "角色管理", description = "角色CRUD接口")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "创建角色")
    @PostMapping
    public Result<Role> create(@Valid @RequestBody CreateRoleRequest request) {
        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());

        Role created = roleService.create(role);
        return Result.success(created);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public Result<Role> update(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());

        Role updated = roleService.update(id, role);
        return Result.success(updated);
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @Operation(summary = "获取角色列表")
    @GetMapping
    public Result<List<Role>> list() {
        return Result.success(roleService.list());
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    // ========== Request DTOs ==========

    @Data
    public static class CreateRoleRequest {
        @NotBlank(message = "角色代码不能为空")
        private String roleCode;
        @NotBlank(message = "角色名称不能为空")
        private String roleName;
        private String description;
    }

    @Data
    public static class UpdateRoleRequest {
        @NotBlank(message = "角色代码不能为空")
        private String roleCode;
        @NotBlank(message = "角色名称不能为空")
        private String roleName;
        private String description;
        private String status;
    }
}
