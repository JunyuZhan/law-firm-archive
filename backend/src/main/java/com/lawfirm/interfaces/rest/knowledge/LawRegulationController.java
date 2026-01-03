package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateLawRegulationCommand;
import com.lawfirm.application.knowledge.dto.*;
import com.lawfirm.application.knowledge.service.LawRegulationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 法规库接口
 */
@Tag(name = "法规库", description = "法规查询、收藏相关接口")
@RestController
@RequestMapping("/knowledge/law")
@RequiredArgsConstructor
public class LawRegulationController {

    private final LawRegulationAppService lawRegulationAppService;

    @Operation(summary = "获取法规分类树")
    @GetMapping("/categories")
    public Result<List<LawCategoryDTO>> getCategoryTree() {
        return Result.success(lawRegulationAppService.getCategoryTree());
    }

    @Operation(summary = "分页查询法规")
    @GetMapping
    public Result<PageResult<LawRegulationDTO>> listRegulations(LawRegulationQueryDTO query) {
        return Result.success(lawRegulationAppService.listRegulations(query));
    }

    @Operation(summary = "获取法规详情")
    @GetMapping("/{id}")
    public Result<LawRegulationDTO> getRegulationById(@PathVariable Long id) {
        return Result.success(lawRegulationAppService.getRegulationById(id));
    }

    @Operation(summary = "创建法规")
    @PostMapping
    @RequirePermission("knowledge:law:create")
    @OperationLog(module = "法规库", action = "创建法规")
    public Result<LawRegulationDTO> createRegulation(@RequestBody CreateLawRegulationCommand command) {
        return Result.success(lawRegulationAppService.createRegulation(command));
    }

    @Operation(summary = "更新法规")
    @PutMapping("/{id}")
    @RequirePermission("knowledge:law:edit")
    @OperationLog(module = "法规库", action = "更新法规")
    public Result<LawRegulationDTO> updateRegulation(@PathVariable Long id, @RequestBody CreateLawRegulationCommand command) {
        return Result.success(lawRegulationAppService.updateRegulation(id, command));
    }

    @Operation(summary = "删除法规")
    @DeleteMapping("/{id}")
    @RequirePermission("knowledge:law:delete")
    @OperationLog(module = "法规库", action = "删除法规")
    public Result<Void> deleteRegulation(@PathVariable Long id) {
        lawRegulationAppService.deleteRegulation(id);
        return Result.success();
    }

    @Operation(summary = "收藏法规")
    @PostMapping("/{id}/collect")
    public Result<Void> collectRegulation(@PathVariable Long id) {
        lawRegulationAppService.collectRegulation(id);
        return Result.success();
    }

    @Operation(summary = "取消收藏法规")
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollectRegulation(@PathVariable Long id) {
        lawRegulationAppService.uncollectRegulation(id);
        return Result.success();
    }

    @Operation(summary = "获取我的收藏法规")
    @GetMapping("/collected")
    public Result<List<LawRegulationDTO>> getMyCollectedRegulations() {
        return Result.success(lawRegulationAppService.getMyCollectedRegulations());
    }

    @Operation(summary = "标注法规失效")
    @PostMapping("/{id}/mark-repealed")
    @RequirePermission("knowledge:law:edit")
    @OperationLog(module = "法规库", action = "标注法规失效")
    public Result<LawRegulationDTO> markAsRepealed(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.success(lawRegulationAppService.markAsRepealed(id, reason));
    }
}
