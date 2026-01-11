package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateDepartmentCommand;
import com.lawfirm.application.system.command.UpdateDepartmentCommand;
import com.lawfirm.application.system.dto.DepartmentDTO;
import com.lawfirm.application.system.service.DepartmentAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理 Controller
 */
@Tag(name = "部门管理", description = "部门管理相关接口")
@RestController
@RequestMapping("/system/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentAppService departmentAppService;

    /**
     * 获取部门树
     */
    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    @RequirePermission("sys:dept:list")
    public Result<List<DepartmentDTO>> getDepartmentTree() {
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree();
        return Result.success(tree);
    }

    /**
     * 获取部门树（公共接口，用于用户选择器等场景）
     * 所有登录用户都可以访问，无需特殊权限
     */
    @Operation(summary = "获取部门树（公共）")
    @GetMapping("/tree-public")
    public Result<List<DepartmentDTO>> getDepartmentTreePublic() {
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree();
        return Result.success(tree);
    }

    /**
     * 获取部门列表（平铺）
     */
    @Operation(summary = "获取部门列表")
    @GetMapping("/list")
    @RequirePermission("sys:dept:list")
    public Result<List<DepartmentDTO>> getAllDepartments() {
        List<DepartmentDTO> departments = departmentAppService.getAllDepartments();
        return Result.success(departments);
    }

    /**
     * 获取部门详情
     */
    @Operation(summary = "获取部门详情")
    @GetMapping("/{id}")
    @RequirePermission("sys:dept:list")
    public Result<DepartmentDTO> getDepartment(@PathVariable Long id) {
        DepartmentDTO department = departmentAppService.getDepartmentById(id);
        return Result.success(department);
    }

    /**
     * 创建部门
     */
    @Operation(summary = "创建部门")
    @PostMapping
    @RequirePermission("sys:dept:create")
    @OperationLog(module = "部门管理", action = "创建部门")
    public Result<DepartmentDTO> createDepartment(@RequestBody @Valid CreateDepartmentCommand command) {
        DepartmentDTO department = departmentAppService.createDepartment(command);
        return Result.success(department);
    }

    /**
     * 更新部门
     */
    @Operation(summary = "更新部门")
    @PutMapping
    @RequirePermission("sys:dept:edit")
    @OperationLog(module = "部门管理", action = "更新部门")
    public Result<DepartmentDTO> updateDepartment(@RequestBody @Valid UpdateDepartmentCommand command) {
        DepartmentDTO department = departmentAppService.updateDepartment(command);
        return Result.success(department);
    }

    /**
     * 删除部门
     */
    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @RequirePermission("sys:dept:delete")
    @OperationLog(module = "部门管理", action = "删除部门")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        departmentAppService.deleteDepartment(id);
        return Result.success();
    }

    /**
     * 设置部门负责人
     */
    @Operation(summary = "设置部门负责人")
    @PutMapping("/{id}/leader")
    @RequirePermission("sys:dept:edit")
    @OperationLog(module = "部门管理", action = "设置部门负责人")
    public Result<Void> setLeader(@PathVariable Long id,
                                   @RequestBody @Valid SetLeaderRequest request) {
        departmentAppService.setLeader(id, request.getLeaderId());
        return Result.success();
    }

    // ========== Request DTOs ==========

    @Data
    public static class SetLeaderRequest {
        private Long leaderId;
    }
}
