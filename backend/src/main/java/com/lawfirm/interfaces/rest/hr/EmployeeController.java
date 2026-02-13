package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateEmployeeCommand;
import com.lawfirm.application.hr.command.UpdateEmployeeCommand;
import com.lawfirm.application.hr.dto.EmployeeDTO;
import com.lawfirm.application.hr.dto.EmployeeQueryDTO;
import com.lawfirm.application.hr.service.EmployeeAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 员工档案管理接口 */
@Tag(name = "员工档案管理", description = "员工档案信息管理相关接口")
@RestController
@RequestMapping("/hr/employee")
@RequiredArgsConstructor
public class EmployeeController {

  /** 员工档案服务. */
  private final EmployeeAppService employeeAppService;

  /**
   * 分页查询员工档案
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询员工档案")
  @GetMapping
  @RequirePermission("hr:employee:list")
  public Result<PageResult<EmployeeDTO>> listEmployees(final EmployeeQueryDTO query) {
    return Result.success(employeeAppService.listEmployees(query));
  }

  /**
   * 根据ID查询员工档案
   *
   * @param id 员工ID
   * @return 员工档案详情
   */
  @Operation(summary = "根据ID查询员工档案")
  @GetMapping("/{id}")
  @RequirePermission("hr:employee:detail")
  public Result<EmployeeDTO> getEmployee(@PathVariable final Long id) {
    return Result.success(employeeAppService.getEmployeeById(id));
  }

  /**
   * 根据用户ID查询员工档案
   *
   * @param userId 用户ID
   * @return 员工档案详情
   */
  @Operation(summary = "根据用户ID查询员工档案")
  @GetMapping("/user/{userId}")
  @RequirePermission("hr:employee:detail")
  public Result<EmployeeDTO> getEmployeeByUserId(@PathVariable final Long userId) {
    return Result.success(employeeAppService.getEmployeeByUserId(userId));
  }

  /**
   * 创建员工档案
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建员工档案")
  @PostMapping
  @RequirePermission("hr:employee:create")
  @OperationLog(module = "员工档案管理", action = "创建员工档案")
  public Result<EmployeeDTO> createEmployee(
      @Valid @RequestBody final CreateEmployeeCommand command) {
    return Result.success(employeeAppService.createEmployee(command));
  }

  /**
   * 更新员工档案
   *
   * @param id 员工ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新员工档案")
  @PutMapping("/{id}")
  @RequirePermission("hr:employee:update")
  @OperationLog(module = "员工档案管理", action = "更新员工档案")
  public Result<EmployeeDTO> updateEmployee(
      @PathVariable final Long id, @Valid @RequestBody final UpdateEmployeeCommand command) {
    return Result.success(employeeAppService.updateEmployee(id, command));
  }

  /**
   * 删除员工档案
   *
   * @param id 员工ID
   * @return 无返回
   */
  @Operation(summary = "删除员工档案")
  @DeleteMapping("/{id}")
  @RequirePermission("hr:employee:delete")
  @OperationLog(module = "员工档案管理", action = "删除员工档案")
  public Result<Void> deleteEmployee(@PathVariable final Long id) {
    employeeAppService.deleteEmployee(id);
    return Result.success();
  }
}
