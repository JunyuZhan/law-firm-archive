package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.dto.OperationLogDTO;
import com.lawfirm.application.system.dto.OperationLogQueryDTO;
import com.lawfirm.application.system.service.OperationLogAppService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志接口
 */
@Tag(name = "操作日志", description = "操作日志查询相关接口")
@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogAppService operationLogAppService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping
    @RequirePermission("sys:log:list")
    public Result<PageResult<OperationLogDTO>> listLogs(OperationLogQueryDTO query) {
        return Result.success(operationLogAppService.listLogs(query));
    }

    @Operation(summary = "获取日志详情")
    @GetMapping("/{id}")
    @RequirePermission("sys:log:list")
    public Result<OperationLogDTO> getLogById(@PathVariable Long id) {
        return Result.success(operationLogAppService.getLogById(id));
    }

    @Operation(summary = "清理历史日志")
    @PostMapping("/clean")
    @RequirePermission("sys:log:clean")
    public Result<Void> cleanOldLogs(@RequestParam(defaultValue = "90") int keepDays) {
        operationLogAppService.cleanOldLogs(keepDays);
        return Result.success();
    }
}
