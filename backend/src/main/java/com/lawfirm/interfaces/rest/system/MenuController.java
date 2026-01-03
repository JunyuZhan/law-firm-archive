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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理接口
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuAppService menuAppService;

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    @RequirePermission("sys:menu:list")
    public Result<List<MenuDTO>> getMenuTree() {
        return Result.success(menuAppService.getMenuTree());
    }

    @Operation(summary = "获取当前用户菜单")
    @GetMapping("/user")
    public Result<List<MenuDTO>> getUserMenus() {
        Long userId = SecurityUtils.getUserId();
        return Result.success(menuAppService.getUserMenuTree(userId));
    }

    @Operation(summary = "获取角色菜单ID")
    @GetMapping("/role/{roleId}")
    @RequirePermission("sys:role:list")
    public Result<List<Long>> getRoleMenuIds(@PathVariable Long roleId) {
        return Result.success(menuAppService.getRoleMenuIds(roleId));
    }

    @Operation(summary = "分配角色菜单")
    @PutMapping("/role/{roleId}")
    @RequirePermission("sys:role:update")
    @OperationLog(module = "菜单管理", action = "分配角色菜单")
    public Result<Void> assignRoleMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        menuAppService.assignRoleMenus(roleId, menuIds);
        return Result.success();
    }

    @Operation(summary = "获取菜单详情")
    @GetMapping("/{id}")
    @RequirePermission("sys:menu:list")
    public Result<MenuDTO> getMenu(@PathVariable Long id) {
        return Result.success(menuAppService.getMenuById(id));
    }

    @Operation(summary = "创建菜单")
    @PostMapping
    @RequirePermission("sys:menu:create")
    @OperationLog(module = "菜单管理", action = "创建菜单")
    public Result<MenuDTO> createMenu(@RequestBody CreateMenuCommand command) {
        return Result.success(menuAppService.createMenu(command));
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    @RequirePermission("sys:menu:update")
    @OperationLog(module = "菜单管理", action = "更新菜单")
    public Result<MenuDTO> updateMenu(@PathVariable Long id, @RequestBody UpdateMenuCommand command) {
        command.setId(id);
        return Result.success(menuAppService.updateMenu(command));
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @RequirePermission("sys:menu:delete")
    @OperationLog(module = "菜单管理", action = "删除菜单")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        menuAppService.deleteMenu(id);
        return Result.success();
    }
}
