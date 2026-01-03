package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateCaseLibraryCommand;
import com.lawfirm.application.knowledge.dto.*;
import com.lawfirm.application.knowledge.service.CaseLibraryAppService;
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
 * 案例库接口
 */
@Tag(name = "案例库", description = "案例查询、收藏相关接口")
@RestController
@RequestMapping("/knowledge/case")
@RequiredArgsConstructor
public class CaseLibraryController {

    private final CaseLibraryAppService caseLibraryAppService;

    @Operation(summary = "获取案例分类树")
    @GetMapping("/categories")
    public Result<List<CaseCategoryDTO>> getCategoryTree() {
        return Result.success(caseLibraryAppService.getCategoryTree());
    }

    @Operation(summary = "分页查询案例")
    @GetMapping
    public Result<PageResult<CaseLibraryDTO>> listCases(CaseLibraryQueryDTO query) {
        return Result.success(caseLibraryAppService.listCases(query));
    }

    @Operation(summary = "获取案例详情")
    @GetMapping("/{id}")
    public Result<CaseLibraryDTO> getCaseById(@PathVariable Long id) {
        return Result.success(caseLibraryAppService.getCaseById(id));
    }

    @Operation(summary = "创建案例")
    @PostMapping
    @RequirePermission("knowledge:case:create")
    @OperationLog(module = "案例库", action = "创建案例")
    public Result<CaseLibraryDTO> createCase(@RequestBody CreateCaseLibraryCommand command) {
        return Result.success(caseLibraryAppService.createCase(command));
    }

    @Operation(summary = "更新案例")
    @PutMapping("/{id}")
    @RequirePermission("knowledge:case:edit")
    @OperationLog(module = "案例库", action = "更新案例")
    public Result<CaseLibraryDTO> updateCase(@PathVariable Long id, @RequestBody CreateCaseLibraryCommand command) {
        return Result.success(caseLibraryAppService.updateCase(id, command));
    }

    @Operation(summary = "删除案例")
    @DeleteMapping("/{id}")
    @RequirePermission("knowledge:case:delete")
    @OperationLog(module = "案例库", action = "删除案例")
    public Result<Void> deleteCase(@PathVariable Long id) {
        caseLibraryAppService.deleteCase(id);
        return Result.success();
    }

    @Operation(summary = "收藏案例")
    @PostMapping("/{id}/collect")
    public Result<Void> collectCase(@PathVariable Long id) {
        caseLibraryAppService.collectCase(id);
        return Result.success();
    }

    @Operation(summary = "取消收藏案例")
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollectCase(@PathVariable Long id) {
        caseLibraryAppService.uncollectCase(id);
        return Result.success();
    }

    @Operation(summary = "获取我的收藏案例")
    @GetMapping("/collected")
    public Result<List<CaseLibraryDTO>> getMyCollectedCases() {
        return Result.success(caseLibraryAppService.getMyCollectedCases());
    }
}
