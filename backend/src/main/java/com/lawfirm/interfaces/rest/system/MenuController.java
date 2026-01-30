package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateMenuCommand;
import com.lawfirm.application.system.command.UpdateMenuCommand;
import com.lawfirm.application.system.dto.MenuDTO;
import com.lawfirm.application.system.service.MenuAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 菜单管理接口 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class MenuController {

  /** 菜单应用服务 */
  private final MenuAppService menuAppService;

  /**
   * 获取菜单树
   *
   * @return 菜单树
   */
  @Operation(summary = "获取菜单树")
  @GetMapping("/tree")
  @RequirePermission("sys:menu:list")
  public Result<List<MenuDTO>> getMenuTree() {
    return Result.success(menuAppService.getMenuTree());
  }

  /**
   * 获取当前用户菜单
   *
   * @return 用户菜单树
   */
  @Operation(summary = "获取当前用户菜单")
  @GetMapping("/user")
  public Result<List<MenuDTO>> getUserMenus() {
    Long userId = SecurityUtils.getUserId();
    return Result.success(menuAppService.getUserMenuTree(userId));
  }

  /**
   * 获取角色菜单ID
   *
   * @param roleId 角色ID
   * @return 菜单ID列表
   */
  @Operation(summary = "获取角色菜单ID")
  @GetMapping("/role/{roleId}")
  @RequirePermission("sys:role:list")
  public Result<List<Long>> getRoleMenuIds(@PathVariable final Long roleId) {
    return Result.success(menuAppService.getRoleMenuIds(roleId));
  }

  /**
   * 分配角色菜单
   *
   * @param roleId 角色ID
   * @param menuIds 菜单ID列表
   * @return 空结果
   */
  @Operation(summary = "分配角色菜单")
  @PutMapping("/role/{roleId}")
  @RequirePermission("sys:role:update")
  @OperationLog(module = "菜单管理", action = "分配角色菜单")
  public Result<Void> assignRoleMenus(
      @PathVariable final Long roleId, @RequestBody final List<Long> menuIds) {
    menuAppService.assignRoleMenus(roleId, menuIds);
    return Result.success();
  }

  /**
   * 获取菜单详情
   *
   * @param id 菜单ID
   * @return 菜单详情
   */
  @Operation(summary = "获取菜单详情")
  @GetMapping("/{id}")
  @RequirePermission("sys:menu:list")
  public Result<MenuDTO> getMenu(@PathVariable final Long id) {
    return Result.success(menuAppService.getMenuById(id));
  }

  /**
   * 创建菜单
   *
   * @param command 创建菜单命令
   * @return 菜单信息
   */
  @Operation(summary = "创建菜单")
  @PostMapping
  @RequirePermission("sys:menu:create")
  @OperationLog(module = "菜单管理", action = "创建菜单")
  public Result<MenuDTO> createMenu(@RequestBody final CreateMenuCommand command) {
    return Result.success(menuAppService.createMenu(command));
  }

  /**
   * 更新菜单
   *
   * @param id 菜单ID
   * @param command 更新菜单命令
   * @return 菜单信息
   */
  @Operation(summary = "更新菜单")
  @PutMapping("/{id}")
  @RequirePermission("sys:menu:update")
  @OperationLog(module = "菜单管理", action = "更新菜单")
  public Result<MenuDTO> updateMenu(
      @PathVariable final Long id, @RequestBody final UpdateMenuCommand command) {
    command.setId(id);
    return Result.success(menuAppService.updateMenu(command));
  }

  /**
   * 删除菜单
   *
   * @param id 菜单ID
   * @return 空结果
   */
  @Operation(summary = "删除菜单")
  @DeleteMapping("/{id}")
  @RequirePermission("sys:menu:delete")
  @OperationLog(module = "菜单管理", action = "删除菜单")
  public Result<Void> deleteMenu(@PathVariable final Long id) {
    menuAppService.deleteMenu(id);
    return Result.success();
  }
}
