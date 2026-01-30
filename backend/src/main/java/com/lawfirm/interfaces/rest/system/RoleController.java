package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateRoleCommand;
import com.lawfirm.application.system.command.UpdateRoleCommand;
import com.lawfirm.application.system.dto.RoleDTO;
import com.lawfirm.application.system.service.RoleAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 角色管理 Controller */
@Tag(name = "角色管理", description = "角色管理相关接口")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class RoleController {

  /** 角色应用服务 */
  private final RoleAppService roleAppService;

  /**
   * 分页查询角色列表
   *
   * @param query 分页查询条件
   * @param keyword 关键词（可选）
   * @return 分页结果
   */
  @Operation(summary = "分页查询角色列表")
  @GetMapping("/list")
  @RequirePermission("sys:role:list")
  public Result<PageResult<RoleDTO>> listRoles(
      final PageQuery query, @RequestParam(required = false) final String keyword) {
    PageResult<RoleDTO> result = roleAppService.listRoles(query, keyword);
    return Result.success(result);
  }

  /**
   * 获取所有角色（下拉选择用）
   *
   * @return 角色列表
   */
  @Operation(summary = "获取所有角色")
  @GetMapping("/all")
  @RequirePermission("sys:role:list")
  public Result<List<RoleDTO>> getAllRoles() {
    List<RoleDTO> roles = roleAppService.getAllRoles();
    return Result.success(roles);
  }

  /**
   * 获取角色详情
   *
   * @param id 角色ID
   * @return 角色详情
   */
  @Operation(summary = "获取角色详情")
  @GetMapping("/{id}")
  @RequirePermission("sys:role:list")
  public Result<RoleDTO> getRole(@PathVariable final Long id) {
    RoleDTO role = roleAppService.getRoleById(id);
    return Result.success(role);
  }

  /**
   * 创建角色
   *
   * @param command 创建角色命令
   * @return 角色信息
   */
  @Operation(summary = "创建角色")
  @PostMapping
  @RequirePermission("sys:role:create")
  @OperationLog(module = "角色管理", action = "创建角色")
  public Result<RoleDTO> createRole(@RequestBody @Valid final CreateRoleCommand command) {
    RoleDTO role = roleAppService.createRole(command);
    return Result.success(role);
  }

  /**
   * 更新角色
   *
   * @param command 更新角色命令
   * @return 角色信息
   */
  @Operation(summary = "更新角色")
  @PutMapping
  @RequirePermission("sys:role:edit")
  @OperationLog(module = "角色管理", action = "更新角色")
  public Result<RoleDTO> updateRole(@RequestBody @Valid final UpdateRoleCommand command) {
    RoleDTO role = roleAppService.updateRole(command);
    return Result.success(role);
  }

  /**
   * 删除角色
   *
   * @param id 角色ID
   * @return 空结果
   */
  @Operation(summary = "删除角色")
  @DeleteMapping("/{id}")
  @RequirePermission("sys:role:delete")
  @OperationLog(module = "角色管理", action = "删除角色")
  public Result<Void> deleteRole(@PathVariable final Long id) {
    roleAppService.deleteRole(id);
    return Result.success();
  }

  /**
   * 修改角色状态
   *
   * @param id 角色ID
   * @param request 状态请求
   * @return 空结果
   */
  @Operation(summary = "修改角色状态")
  @PutMapping("/{id}/status")
  @RequirePermission("sys:role:edit")
  @OperationLog(module = "角色管理", action = "修改角色状态")
  public Result<Void> changeStatus(
      @PathVariable final Long id, @RequestBody @Valid final ChangeStatusRequest request) {
    roleAppService.changeStatus(id, request.getStatus());
    return Result.success();
  }

  /**
   * 分配角色菜单
   *
   * @param id 角色ID
   * @param request 菜单分配请求
   * @return 空结果
   */
  @Operation(summary = "分配角色菜单")
  @PostMapping("/{id}/menus")
  @RequirePermission("sys:role:edit")
  @OperationLog(module = "角色管理", action = "分配角色菜单")
  public Result<Void> assignMenus(
      @PathVariable final Long id, @RequestBody @Valid final AssignMenusRequest request) {
    roleAppService.assignMenus(id, request.getMenuIds());
    return Result.success();
  }

  /**
   * 获取角色菜单ID列表
   *
   * @param id 角色ID
   * @return 菜单ID列表
   */
  @Operation(summary = "获取角色菜单ID列表")
  @GetMapping("/{id}/menus")
  @RequirePermission("sys:role:list")
  public Result<List<Long>> getRoleMenuIds(@PathVariable final Long id) {
    List<Long> menuIds = roleAppService.getRoleMenuIds(id);
    return Result.success(menuIds);
  }

  // ========== Request DTOs ==========

  /** 变更状态请求 */
  @Data
  public static class ChangeStatusRequest {
    /** 状态 */
    private String status;
  }

  /** 分配菜单请求 */
  @Data
  public static class AssignMenusRequest {
    /** 菜单ID列表 */
    private List<Long> menuIds;
  }
}
