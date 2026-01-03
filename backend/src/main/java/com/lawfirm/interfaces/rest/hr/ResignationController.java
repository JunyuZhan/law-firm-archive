package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.ApproveResignationCommand;
import com.lawfirm.application.hr.command.CreateResignationCommand;
import com.lawfirm.application.hr.dto.ResignationDTO;
import com.lawfirm.application.hr.dto.ResignationQueryDTO;
import com.lawfirm.application.hr.service.ResignationAppService;
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
 * 离职申请管理接口
 */
@Tag(name = "离职申请管理", description = "离职申请管理相关接口")
@RestController
@RequestMapping("/hr/resignation")
@RequiredArgsConstructor
public class ResignationController {

    private final ResignationAppService resignationAppService;

    @Operation(summary = "分页查询离职申请")
    @GetMapping
    @RequirePermission("hr:resignation:list")
    public Result<PageResult<ResignationDTO>> listResignations(ResignationQueryDTO query) {
        return Result.success(resignationAppService.listResignations(query));
    }

    @Operation(summary = "根据ID查询离职申请")
    @GetMapping("/{id}")
    @RequirePermission("hr:resignation:detail")
    public Result<ResignationDTO> getResignation(@PathVariable Long id) {
        return Result.success(resignationAppService.getResignationById(id));
    }

    @Operation(summary = "根据员工ID查询离职申请")
    @GetMapping("/employee/{employeeId}")
    @RequirePermission("hr:resignation:list")
    public Result<List<ResignationDTO>> getResignationsByEmployeeId(@PathVariable Long employeeId) {
        return Result.success(resignationAppService.getResignationsByEmployeeId(employeeId));
    }

    @Operation(summary = "创建离职申请")
    @PostMapping
    @RequirePermission("hr:resignation:create")
    @OperationLog(module = "离职申请管理", action = "创建离职申请")
    public Result<ResignationDTO> createResignation(@Valid @RequestBody CreateResignationCommand command) {
        return Result.success(resignationAppService.createResignation(command));
    }

    @Operation(summary = "审批离职申请")
    @PostMapping("/{id}/approve")
    @RequirePermission("hr:resignation:approve")
    @OperationLog(module = "离职申请管理", action = "审批离职申请")
    public Result<ResignationDTO> approveResignation(@PathVariable Long id, @Valid @RequestBody ApproveResignationCommand command) {
        return Result.success(resignationAppService.approveResignation(id, command));
    }

    @Operation(summary = "完成交接")
    @PostMapping("/{id}/complete-handover")
    @RequirePermission("hr:resignation:handover")
    @OperationLog(module = "离职申请管理", action = "完成离职交接")
    public Result<ResignationDTO> completeHandover(@PathVariable Long id, @RequestParam(required = false) String handoverNote) {
        return Result.success(resignationAppService.completeHandover(id, handoverNote));
    }

    @Operation(summary = "删除离职申请")
    @DeleteMapping("/{id}")
    @RequirePermission("hr:resignation:delete")
    @OperationLog(module = "离职申请管理", action = "删除离职申请")
    public Result<Void> deleteResignation(@PathVariable Long id) {
        resignationAppService.deleteResignation(id);
        return Result.success();
    }
}

