package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.ApproveRegularizationCommand;
import com.lawfirm.application.hr.command.CreateRegularizationCommand;
import com.lawfirm.application.hr.dto.RegularizationDTO;
import com.lawfirm.application.hr.dto.RegularizationQueryDTO;
import com.lawfirm.application.hr.service.RegularizationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 转正申请管理接口
 */
@Tag(name = "转正申请管理", description = "转正申请管理相关接口")
@RestController
@RequestMapping("/hr/regularization")
@RequiredArgsConstructor
public class RegularizationController {

    private final RegularizationAppService regularizationAppService;

    @Operation(summary = "分页查询转正申请")
    @GetMapping
    @RequirePermission("hr:regularization:list")
    public Result<PageResult<RegularizationDTO>> listRegularizations(RegularizationQueryDTO query) {
        return Result.success(regularizationAppService.listRegularizations(query));
    }

    @Operation(summary = "根据ID查询转正申请")
    @GetMapping("/{id}")
    @RequirePermission("hr:regularization:detail")
    public Result<RegularizationDTO> getRegularization(@PathVariable Long id) {
        return Result.success(regularizationAppService.getRegularizationById(id));
    }

    @Operation(summary = "根据员工ID查询转正申请")
    @GetMapping("/employee/{employeeId}")
    @RequirePermission("hr:regularization:list")
    public Result<List<RegularizationDTO>> getRegularizationsByEmployeeId(@PathVariable Long employeeId) {
        return Result.success(regularizationAppService.getRegularizationsByEmployeeId(employeeId));
    }

    @Operation(summary = "创建转正申请")
    @PostMapping
    @RequirePermission("hr:regularization:create")
    @OperationLog(module = "转正申请管理", action = "创建转正申请")
    public Result<RegularizationDTO> createRegularization(@Valid @RequestBody CreateRegularizationCommand command) {
        return Result.success(regularizationAppService.createRegularization(command));
    }

    @Operation(summary = "审批转正申请")
    @PostMapping("/{id}/approve")
    @RequirePermission("hr:regularization:approve")
    @OperationLog(module = "转正申请管理", action = "审批转正申请")
    public Result<RegularizationDTO> approveRegularization(@PathVariable Long id, @Valid @RequestBody ApproveRegularizationCommand command) {
        return Result.success(regularizationAppService.approveRegularization(id, command));
    }

    @Operation(summary = "删除转正申请")
    @DeleteMapping("/{id}")
    @RequirePermission("hr:regularization:delete")
    @OperationLog(module = "转正申请管理", action = "删除转正申请")
    public Result<Void> deleteRegularization(@PathVariable Long id) {
        regularizationAppService.deleteRegularization(id);
        return Result.success();
    }
}

