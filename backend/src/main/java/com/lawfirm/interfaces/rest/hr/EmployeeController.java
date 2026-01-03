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
import org.springframework.web.bind.annotation.*;

/**
 * 员工档案管理接口
 */
@Tag(name = "员工档案管理", description = "员工档案信息管理相关接口")
@RestController
@RequestMapping("/hr/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeAppService employeeAppService;

    @Operation(summary = "分页查询员工档案")
    @GetMapping
    @RequirePermission("hr:employee:list")
    public Result<PageResult<EmployeeDTO>> listEmployees(EmployeeQueryDTO query) {
        return Result.success(employeeAppService.listEmployees(query));
    }

    @Operation(summary = "根据ID查询员工档案")
    @GetMapping("/{id}")
    @RequirePermission("hr:employee:detail")
    public Result<EmployeeDTO> getEmployee(@PathVariable Long id) {
        return Result.success(employeeAppService.getEmployeeById(id));
    }

    @Operation(summary = "根据用户ID查询员工档案")
    @GetMapping("/user/{userId}")
    @RequirePermission("hr:employee:detail")
    public Result<EmployeeDTO> getEmployeeByUserId(@PathVariable Long userId) {
        return Result.success(employeeAppService.getEmployeeByUserId(userId));
    }

    @Operation(summary = "创建员工档案")
    @PostMapping
    @RequirePermission("hr:employee:create")
    @OperationLog(module = "员工档案管理", action = "创建员工档案")
    public Result<EmployeeDTO> createEmployee(@Valid @RequestBody CreateEmployeeCommand command) {
        return Result.success(employeeAppService.createEmployee(command));
    }

    @Operation(summary = "更新员工档案")
    @PutMapping("/{id}")
    @RequirePermission("hr:employee:update")
    @OperationLog(module = "员工档案管理", action = "更新员工档案")
    public Result<EmployeeDTO> updateEmployee(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeCommand command) {
        return Result.success(employeeAppService.updateEmployee(id, command));
    }

    @Operation(summary = "删除员工档案")
    @DeleteMapping("/{id}")
    @RequirePermission("hr:employee:delete")
    @OperationLog(module = "员工档案管理", action = "删除员工档案")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        employeeAppService.deleteEmployee(id);
        return Result.success();
    }
}

