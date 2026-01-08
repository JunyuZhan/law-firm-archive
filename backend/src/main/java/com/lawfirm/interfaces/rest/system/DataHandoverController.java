package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.ConfirmHandoverCommand;
import com.lawfirm.application.system.command.CreateHandoverCommand;
import com.lawfirm.application.system.dto.DataHandoverDTO;
import com.lawfirm.application.system.dto.DataHandoverPreviewDTO;
import com.lawfirm.application.system.dto.DataHandoverQueryDTO;
import com.lawfirm.application.system.service.DataHandoverService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据交接 Controller
 */
@Tag(name = "数据交接", description = "用户数据交接管理")
@RestController
@RequestMapping("/system/data-handover")
@RequiredArgsConstructor
public class DataHandoverController {

    private final DataHandoverService dataHandoverService;

    /**
     * 预览离职交接数据
     */
    @GetMapping("/preview/{userId}")
    @RequirePermission("sys:handover:view")
    @Operation(summary = "预览交接数据", description = "预览指定用户的待交接数据")
    public Result<DataHandoverPreviewDTO> previewHandover(@PathVariable Long userId) {
        DataHandoverPreviewDTO preview = dataHandoverService.previewResignationHandover(userId);
        return Result.success(preview);
    }

    /**
     * 创建离职交接
     */
    @PostMapping("/resignation")
    @RequirePermission("sys:handover:create")
    @Operation(summary = "创建离职交接", description = "创建离职交接单，一次性移交所有数据")
    public Result<DataHandoverDTO> createResignationHandover(@RequestBody @Valid CreateHandoverCommand command) {
        command.setHandoverType("RESIGNATION");
        DataHandoverDTO result = dataHandoverService.createResignationHandover(command);
        return Result.success(result);
    }

    /**
     * 创建项目移交
     */
    @PostMapping("/project")
    @RequirePermission("sys:handover:create")
    @Operation(summary = "创建项目移交", description = "创建项目移交单，移交指定项目")
    public Result<DataHandoverDTO> createProjectHandover(@RequestBody @Valid CreateHandoverCommand command) {
        command.setHandoverType("PROJECT");
        DataHandoverDTO result = dataHandoverService.createMatterHandover(command);
        return Result.success(result);
    }

    /**
     * 创建客户移交
     */
    @PostMapping("/client")
    @RequirePermission("sys:handover:create")
    @Operation(summary = "创建客户移交", description = "创建客户移交单，移交指定客户")
    public Result<DataHandoverDTO> createClientHandover(@RequestBody @Valid CreateHandoverCommand command) {
        command.setHandoverType("CLIENT");
        DataHandoverDTO result = dataHandoverService.createClientHandover(command);
        return Result.success(result);
    }

    /**
     * 确认交接
     */
    @PostMapping("/{id}/confirm")
    @RequirePermission("sys:handover:confirm")
    @Operation(summary = "确认交接", description = "确认交接单，执行数据迁移")
    public Result<Void> confirmHandover(@PathVariable Long id,
                                         @RequestBody(required = false) ConfirmHandoverCommand command) {
        dataHandoverService.confirmHandover(id);
        return Result.success();
    }

    /**
     * 取消交接
     */
    @PostMapping("/{id}/cancel")
    @RequirePermission("sys:handover:cancel")
    @Operation(summary = "取消交接", description = "取消待确认的交接单")
    public Result<Void> cancelHandover(@PathVariable Long id, @RequestBody CancelRequest request) {
        dataHandoverService.cancelHandover(id, request.getReason());
        return Result.success();
    }

    /**
     * 获取交接单详情
     */
    @GetMapping("/{id}")
    @RequirePermission("sys:handover:view")
    @Operation(summary = "获取交接单详情", description = "获取交接单及其明细")
    public Result<DataHandoverDTO> getHandover(@PathVariable Long id) {
        DataHandoverDTO result = dataHandoverService.getHandoverById(id);
        return Result.success(result);
    }

    /**
     * 分页查询交接单
     */
    @GetMapping
    @RequirePermission("sys:handover:list")
    @Operation(summary = "分页查询交接单", description = "分页查询数据交接记录")
    public Result<PageResult<DataHandoverDTO>> listHandovers(DataHandoverQueryDTO query) {
        PageResult<DataHandoverDTO> result = dataHandoverService.listHandovers(query);
        return Result.success(result);
    }

    @Data
    public static class CancelRequest {
        private String reason;
    }
}

