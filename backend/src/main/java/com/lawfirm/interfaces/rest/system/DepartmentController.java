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
import org.springframework.web.bind.annotation.RestController;

/** 部门管理 Controller */
@Tag(name = "部门管理", description = "部门管理相关接口")
@RestController
@RequestMapping("/system/department")
@RequiredArgsConstructor
public class DepartmentController {

  /** 部门应用服务 */
  private final DepartmentAppService departmentAppService;

  /**
   * 获取部门树
   *
   * @return 部门树
   */
  @Operation(summary = "获取部门树")
  @GetMapping("/tree")
  @RequirePermission("sys:dept:list")
  public Result<List<DepartmentDTO>> getDepartmentTree() {
    List<DepartmentDTO> tree = departmentAppService.getDepartmentTree();
    return Result.success(tree);
  }

  /**
   * 获取部门树（公共接口，用于用户选择器等场景） 所有登录用户都可以访问，无需特殊权限
   *
   * @return 部门树
   */
  @Operation(summary = "获取部门树（公共）")
  @GetMapping("/tree-public")
  public Result<List<DepartmentDTO>> getDepartmentTreePublic() {
    List<DepartmentDTO> tree = departmentAppService.getDepartmentTree();
    return Result.success(tree);
  }

  /**
   * 获取部门列表（平铺）
   *
   * @return 部门列表
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
   *
   * @param id 部门ID
   * @return 部门详情
   */
  @Operation(summary = "获取部门详情")
  @GetMapping("/{id}")
  @RequirePermission("sys:dept:list")
  public Result<DepartmentDTO> getDepartment(@PathVariable final Long id) {
    DepartmentDTO department = departmentAppService.getDepartmentById(id);
    return Result.success(department);
  }

  /**
   * 创建部门
   *
   * @param command 创建部门命令
   * @return 部门信息
   */
  @Operation(summary = "创建部门")
  @PostMapping
  @RequirePermission("sys:dept:create")
  @OperationLog(module = "部门管理", action = "创建部门")
  public Result<DepartmentDTO> createDepartment(
      @RequestBody @Valid final CreateDepartmentCommand command) {
    DepartmentDTO department = departmentAppService.createDepartment(command);
    return Result.success(department);
  }

  /**
   * 更新部门
   *
   * @param command 更新部门命令
   * @return 部门信息
   */
  @Operation(summary = "更新部门")
  @PutMapping
  @RequirePermission("sys:dept:edit")
  @OperationLog(module = "部门管理", action = "更新部门")
  public Result<DepartmentDTO> updateDepartment(
      @RequestBody @Valid final UpdateDepartmentCommand command) {
    DepartmentDTO department = departmentAppService.updateDepartment(command);
    return Result.success(department);
  }

  /**
   * 删除部门
   *
   * @param id 部门ID
   * @return 空结果
   */
  @Operation(summary = "删除部门")
  @DeleteMapping("/{id}")
  @RequirePermission("sys:dept:delete")
  @OperationLog(module = "部门管理", action = "删除部门")
  public Result<Void> deleteDepartment(@PathVariable final Long id) {
    departmentAppService.deleteDepartment(id);
    return Result.success();
  }

  /**
   * 设置部门负责人
   *
   * @param id 部门ID
   * @param request 设置负责人请求
   * @return 空结果
   */
  @Operation(summary = "设置部门负责人")
  @PutMapping("/{id}/leader")
  @RequirePermission("sys:dept:edit")
  @OperationLog(module = "部门管理", action = "设置部门负责人")
  public Result<Void> setLeader(
      @PathVariable final Long id, @RequestBody @Valid final SetLeaderRequest request) {
    departmentAppService.setLeader(id, request.getLeaderId());
    return Result.success();
  }

  // ========== Request DTOs ==========

  /** 设置负责人请求 */
  @Data
  public static class SetLeaderRequest {
    /** 负责人ID */
    private Long leaderId;
  }
}
