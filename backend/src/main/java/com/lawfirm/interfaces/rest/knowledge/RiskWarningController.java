package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateRiskWarningCommand;
import com.lawfirm.application.knowledge.dto.RiskWarningDTO;
import com.lawfirm.application.knowledge.service.RiskWarningAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风险预警接口（M10-033）
 */
@Tag(name = "风险预警", description = "项目风险预警相关接口")
@RestController
@RequestMapping("/knowledge/risk-warning")
@RequiredArgsConstructor
public class RiskWarningController {

    private final RiskWarningAppService warningAppService;

    @Operation(summary = "创建风险预警")
    @PostMapping
    @RequirePermission("knowledge:quality:create")
    @OperationLog(module = "质量管理", action = "创建风险预警")
    public Result<RiskWarningDTO> createWarning(@RequestBody CreateRiskWarningCommand command) {
        return Result.success(warningAppService.createWarning(command));
    }

    @Operation(summary = "确认预警")
    @PostMapping("/{id}/acknowledge")
    @RequirePermission("knowledge:quality:edit")
    @OperationLog(module = "质量管理", action = "确认风险预警")
    public Result<RiskWarningDTO> acknowledgeWarning(@PathVariable Long id) {
        return Result.success(warningAppService.acknowledgeWarning(id));
    }

    @Operation(summary = "解决预警")
    @PostMapping("/{id}/resolve")
    @RequirePermission("knowledge:quality:edit")
    @OperationLog(module = "质量管理", action = "解决风险预警")
    public Result<RiskWarningDTO> resolveWarning(@PathVariable Long id) {
        return Result.success(warningAppService.resolveWarning(id));
    }

    @Operation(summary = "关闭预警")
    @PostMapping("/{id}/close")
    @RequirePermission("knowledge:quality:edit")
    @OperationLog(module = "质量管理", action = "关闭风险预警")
    public Result<RiskWarningDTO> closeWarning(@PathVariable Long id) {
        return Result.success(warningAppService.closeWarning(id));
    }

    @Operation(summary = "获取预警详情")
    @GetMapping("/{id}")
    @RequirePermission("knowledge:quality:detail")
    public Result<RiskWarningDTO> getWarningById(@PathVariable Long id) {
        return Result.success(warningAppService.getWarningById(id));
    }

    @Operation(summary = "获取项目的所有预警")
    @GetMapping("/matter/{matterId}")
    @RequirePermission("knowledge:quality:list")
    public Result<List<RiskWarningDTO>> getWarningsByMatterId(@PathVariable Long matterId) {
        return Result.success(warningAppService.getWarningsByMatterId(matterId));
    }

    @Operation(summary = "获取活跃的预警")
    @GetMapping("/active")
    @RequirePermission("knowledge:quality:list")
    public Result<List<RiskWarningDTO>> getActiveWarnings() {
        return Result.success(warningAppService.getActiveWarnings());
    }

    @Operation(summary = "获取高风险预警")
    @GetMapping("/high-risk")
    @RequirePermission("knowledge:quality:list")
    public Result<List<RiskWarningDTO>> getHighRiskWarnings() {
        return Result.success(warningAppService.getHighRiskWarnings());
    }
}

