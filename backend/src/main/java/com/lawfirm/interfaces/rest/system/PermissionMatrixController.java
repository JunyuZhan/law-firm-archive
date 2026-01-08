package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.service.PermissionMatrixService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限矩阵管理 Controller
 */
@RestController
@RequestMapping("/system/permission-matrix")
@RequiredArgsConstructor
public class PermissionMatrixController {

    private final PermissionMatrixService permissionMatrixService;

    /**
     * 获取权限矩阵
     */
    @GetMapping
    @RequirePermission("sys:role:list")
    @Operation(summary = "获取权限矩阵", description = "获取所有角色和权限的对应关系矩阵")
    public Result<PermissionMatrixService.PermissionMatrixDTO> getPermissionMatrix(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String permissionType) {
        PermissionMatrixService.PermissionMatrixDTO matrix = permissionMatrixService.getPermissionMatrix(module, permissionType);
        return Result.success(matrix);
    }

    /**
     * 获取角色权限详情
     */
    @GetMapping("/role/{roleId}")
    @RequirePermission("sys:role:list")
    @Operation(summary = "获取角色权限", description = "获取指定角色的所有权限")
    public Result<PermissionMatrixService.RolePermissionDTO> getRolePermissions(@PathVariable Long roleId) {
        PermissionMatrixService.RolePermissionDTO result = permissionMatrixService.getRolePermissions(roleId);
        return Result.success(result);
    }

    /**
     * 对比角色权限
     */
    @PostMapping("/compare")
    @RequirePermission("sys:role:list")
    @Operation(summary = "对比权限", description = "对比多个角色的权限差异")
    public Result<PermissionMatrixService.PermissionCompareDTO> comparePermissions(
            @RequestBody @Valid ComparePermissionRequest request) {
        PermissionMatrixService.PermissionCompareDTO result = permissionMatrixService.comparePermissions(request.getRoleIds());
        return Result.success(result);
    }

    @Data
    public static class ComparePermissionRequest {
        @jakarta.validation.constraints.NotEmpty(message = "角色ID列表不能为空")
        private List<Long> roleIds;
    }
}

