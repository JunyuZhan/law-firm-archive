package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.service.PermissionMatrixService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 权限矩阵管理 Controller */
@RestController
@RequestMapping("/system/permission-matrix")
@RequiredArgsConstructor
public class PermissionMatrixController {

  /** 权限矩阵服务 */
  private final PermissionMatrixService permissionMatrixService;

  /**
   * 获取权限矩阵
   *
   * @param module 模块名称
   * @param permissionType 权限类型
   * @return 权限矩阵
   */
  @GetMapping
  @RequirePermission("sys:role:list")
  @Operation(summary = "获取权限矩阵", description = "获取所有角色和权限的对应关系矩阵")
  public Result<PermissionMatrixService.PermissionMatrixDTO> getPermissionMatrix(
      @RequestParam(required = false) final String module,
      @RequestParam(required = false) final String permissionType) {
    PermissionMatrixService.PermissionMatrixDTO matrix =
        permissionMatrixService.getPermissionMatrix(module, permissionType);
    return Result.success(matrix);
  }

  /**
   * 获取角色权限详情
   *
   * @param roleId 角色ID
   * @return 角色权限详情
   */
  @GetMapping("/role/{roleId}")
  @RequirePermission("sys:role:list")
  @Operation(summary = "获取角色权限", description = "获取指定角色的所有权限")
  public Result<PermissionMatrixService.RolePermissionDTO> getRolePermissions(
      @PathVariable final Long roleId) {
    PermissionMatrixService.RolePermissionDTO result =
        permissionMatrixService.getRolePermissions(roleId);
    return Result.success(result);
  }

  /**
   * 对比角色权限
   *
   * @param request 对比权限请求
   * @return 权限对比结果
   */
  @PostMapping("/compare")
  @RequirePermission("sys:role:list")
  @Operation(summary = "对比权限", description = "对比多个角色的权限差异")
  public Result<PermissionMatrixService.PermissionCompareDTO> comparePermissions(
      @RequestBody @Valid final ComparePermissionRequest request) {
    PermissionMatrixService.PermissionCompareDTO result =
        permissionMatrixService.comparePermissions(request.getRoleIds());
    return Result.success(result);
  }

  /** 对比权限请求 */
  @Data
  public static class ComparePermissionRequest {
    /** 角色ID列表 */
    @jakarta.validation.constraints.NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
  }
}
