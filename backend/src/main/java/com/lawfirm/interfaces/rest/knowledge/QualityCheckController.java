package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateQualityCheckCommand;
import com.lawfirm.application.knowledge.dto.QualityCheckDTO;
import com.lawfirm.application.knowledge.service.QualityCheckAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 质量检查接口（M10-031）
 */
@Tag(name = "质量检查", description = "项目质量检查相关接口")
@RestController
@RequestMapping("/knowledge/quality-check")
@RequiredArgsConstructor
public class QualityCheckController {

    private final QualityCheckAppService checkAppService;

    @Operation(summary = "创建质量检查")
    @PostMapping
    @RequirePermission("knowledge:quality:create")
    @OperationLog(module = "质量管理", action = "创建质量检查")
    public Result<QualityCheckDTO> createCheck(@RequestBody CreateQualityCheckCommand command) {
        return Result.success(checkAppService.createCheck(command));
    }

    @Operation(summary = "获取检查详情")
    @GetMapping("/{id}")
    @RequirePermission("knowledge:quality:detail")
    public Result<QualityCheckDTO> getCheckById(@PathVariable Long id) {
        return Result.success(checkAppService.getCheckById(id));
    }

    @Operation(summary = "获取项目的所有检查")
    @GetMapping("/matter/{matterId}")
    @RequirePermission("knowledge:quality:list")
    public Result<List<QualityCheckDTO>> getChecksByMatterId(@PathVariable Long matterId) {
        return Result.success(checkAppService.getChecksByMatterId(matterId));
    }

    @Operation(summary = "获取进行中的检查")
    @GetMapping("/in-progress")
    @RequirePermission("knowledge:quality:list")
    public Result<List<QualityCheckDTO>> getInProgressChecks() {
        return Result.success(checkAppService.getInProgressChecks());
    }
}

