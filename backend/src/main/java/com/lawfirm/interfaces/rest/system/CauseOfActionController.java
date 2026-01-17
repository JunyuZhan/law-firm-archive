package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateCauseCommand;
import com.lawfirm.application.system.command.UpdateCauseCommand;
import com.lawfirm.application.system.service.CauseOfActionService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.system.entity.CauseOfAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 案由/罪名 API
 */
@Tag(name = "案由管理", description = "案由/罪名数据查询接口")
@RestController
@RequestMapping("/causes")
@RequiredArgsConstructor
public class CauseOfActionController {

    private final CauseOfActionService causeOfActionService;

    @Operation(summary = "获取民事案由树")
    @GetMapping("/civil/tree")
    public Result<List<CauseOfActionService.CauseTreeNode>> getCivilCauseTree() {
        return Result.success(causeOfActionService.getCivilCauseTree());
    }

    @Operation(summary = "获取刑事罪名树")
    @GetMapping("/criminal/tree")
    public Result<List<CauseOfActionService.CauseTreeNode>> getCriminalChargeTree() {
        return Result.success(causeOfActionService.getCriminalChargeTree());
    }

    @Operation(summary = "获取行政案由树")
    @GetMapping("/admin/tree")
    public Result<List<CauseOfActionService.CauseTreeNode>> getAdminCauseTree() {
        return Result.success(causeOfActionService.getAdminCauseTree());
    }

    @Operation(summary = "搜索案由")
    @GetMapping("/search")
    public Result<List<CauseOfAction>> searchCauses(
            @Parameter(description = "案由类型: CIVIL/CRIMINAL/ADMIN") @RequestParam String type,
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        return Result.success(causeOfActionService.searchCauses(type, keyword));
    }

    @Operation(summary = "获取案由名称")
    @GetMapping("/name")
    public Result<String> getCauseName(
            @Parameter(description = "案由代码") @RequestParam String code,
            @Parameter(description = "案由类型: CIVIL/CRIMINAL/ADMIN") @RequestParam(defaultValue = "CIVIL") String type) {
        return Result.success(causeOfActionService.getCauseName(code, type));
    }

    // ==================== CRUD 操作 ====================

    @Operation(summary = "获取案由详情")
    @GetMapping("/{id}")
    public Result<CauseOfAction> getCause(@PathVariable Long id) {
        return Result.success(causeOfActionService.getCauseById(id));
    }

    @Operation(summary = "创建案由/罪名")
    @PostMapping
    @RequirePermission("system:cause:create")
    @OperationLog(module = "案由管理", action = "创建案由")
    public Result<CauseOfAction> createCause(@RequestBody CreateCauseCommand command) {
        return Result.success(causeOfActionService.createCause(command));
    }

    @Operation(summary = "更新案由/罪名")
    @PutMapping("/{id}")
    @RequirePermission("system:cause:update")
    @OperationLog(module = "案由管理", action = "更新案由")
    public Result<CauseOfAction> updateCause(
            @PathVariable Long id,
            @RequestBody UpdateCauseCommand command) {
        return Result.success(causeOfActionService.updateCause(id, command));
    }

    @Operation(summary = "删除案由/罪名")
    @DeleteMapping("/{id}")
    @RequirePermission("system:cause:delete")
    @OperationLog(module = "案由管理", action = "删除案由")
    public Result<Void> deleteCause(@PathVariable Long id) {
        causeOfActionService.deleteCause(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用案由")
    @PostMapping("/{id}/toggle")
    @RequirePermission("system:cause:update")
    @OperationLog(module = "案由管理", action = "切换案由状态")
    public Result<Void> toggleCauseStatus(@PathVariable Long id) {
        causeOfActionService.toggleCauseStatus(id);
        return Result.success();
    }
}
