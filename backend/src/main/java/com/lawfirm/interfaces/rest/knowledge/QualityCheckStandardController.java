package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateQualityCheckStandardCommand;
import com.lawfirm.application.knowledge.dto.QualityCheckStandardDTO;
import com.lawfirm.application.knowledge.service.QualityCheckStandardAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 质量检查标准接口（M10-030）
 */
@Tag(name = "质量检查标准", description = "质量检查标准管理相关接口")
@RestController
@RequestMapping("/knowledge/quality-standard")
@RequiredArgsConstructor
public class QualityCheckStandardController {

    private final QualityCheckStandardAppService standardAppService;

    @Operation(summary = "分页查询检查标准")
    @GetMapping
    @RequirePermission("knowledge:quality:list")
    public Result<List<QualityCheckStandardDTO>> listStandards() {
        return Result.success(standardAppService.getEnabledStandards());
    }

    @Operation(summary = "获取所有启用的检查标准")
    @GetMapping("/enabled")
    @RequirePermission("knowledge:quality:list")
    public Result<List<QualityCheckStandardDTO>> getEnabledStandards() {
        return Result.success(standardAppService.getEnabledStandards());
    }

    @Operation(summary = "按分类查询检查标准")
    @GetMapping("/category/{category}")
    @RequirePermission("knowledge:quality:list")
    public Result<List<QualityCheckStandardDTO>> getStandardsByCategory(@PathVariable String category) {
        return Result.success(standardAppService.getStandardsByCategory(category));
    }

    @Operation(summary = "获取检查标准详情")
    @GetMapping("/{id}")
    @RequirePermission("knowledge:quality:detail")
    public Result<QualityCheckStandardDTO> getStandardById(@PathVariable Long id) {
        return Result.success(standardAppService.getStandardById(id));
    }

    @Operation(summary = "创建检查标准")
    @PostMapping
    @RequirePermission("knowledge:quality:create")
    @OperationLog(module = "质量管理", action = "创建检查标准")
    public Result<QualityCheckStandardDTO> createStandard(@RequestBody CreateQualityCheckStandardCommand command) {
        return Result.success(standardAppService.createStandard(command));
    }

    @Operation(summary = "更新检查标准")
    @PutMapping("/{id}")
    @RequirePermission("knowledge:quality:edit")
    @OperationLog(module = "质量管理", action = "更新检查标准")
    public Result<QualityCheckStandardDTO> updateStandard(@PathVariable Long id, @RequestBody CreateQualityCheckStandardCommand command) {
        return Result.success(standardAppService.updateStandard(id, command));
    }

    @Operation(summary = "删除检查标准")
    @DeleteMapping("/{id}")
    @RequirePermission("knowledge:quality:delete")
    @OperationLog(module = "质量管理", action = "删除检查标准")
    public Result<Void> deleteStandard(@PathVariable Long id) {
        standardAppService.deleteStandard(id);
        return Result.success();
    }
}

