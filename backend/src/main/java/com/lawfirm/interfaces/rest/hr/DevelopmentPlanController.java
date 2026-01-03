package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateDevelopmentPlanCommand;
import com.lawfirm.application.hr.dto.DevelopmentPlanDTO;
import com.lawfirm.application.hr.service.DevelopmentPlanAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 发展规划控制器
 */
@Tag(name = "发展规划", description = "个人发展规划管理接口")
@RestController
@RequestMapping("/api/hr/development-plans")
@RequiredArgsConstructor
public class DevelopmentPlanController {

    private final DevelopmentPlanAppService developmentPlanAppService;

    @Operation(summary = "分页查询发展规划")
    @GetMapping
    @RequirePermission("hr:plan:list")
    public Result<PageResult<DevelopmentPlanDTO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "员工ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "规划年度") @RequestParam(required = false) Integer planYear) {
        PageResult<DevelopmentPlanDTO> result = developmentPlanAppService.listPlans(
                pageNum, pageSize, keyword, status, employeeId, planYear);
        return Result.success(result);
    }

    @Operation(summary = "获取规划详情")
    @GetMapping("/{id}")
    @RequirePermission("hr:plan:view")
    public Result<DevelopmentPlanDTO> getById(@PathVariable Long id) {
        DevelopmentPlanDTO plan = developmentPlanAppService.getPlanById(id);
        return Result.success(plan);
    }

    @Operation(summary = "获取我的当年规划")
    @GetMapping("/my-current")
    public Result<DevelopmentPlanDTO> getMyCurrentPlan() {
        DevelopmentPlanDTO plan = developmentPlanAppService.getMyCurrentPlan();
        return Result.success(plan);
    }

    @Operation(summary = "创建发展规划")
    @PostMapping
    @RequirePermission("hr:plan:create")
    @OperationLog(module = "发展规划", action = "创建规划")
    public Result<DevelopmentPlanDTO> create(@RequestBody @Valid CreateDevelopmentPlanCommand command) {
        DevelopmentPlanDTO plan = developmentPlanAppService.createPlan(command);
        return Result.success(plan);
    }

    @Operation(summary = "更新发展规划")
    @PutMapping("/{id}")
    @RequirePermission("hr:plan:edit")
    @OperationLog(module = "发展规划", action = "更新规划")
    public Result<DevelopmentPlanDTO> update(@PathVariable Long id,
                                              @RequestBody @Valid CreateDevelopmentPlanCommand command) {
        DevelopmentPlanDTO plan = developmentPlanAppService.updatePlan(id, command);
        return Result.success(plan);
    }

    @Operation(summary = "删除发展规划")
    @DeleteMapping("/{id}")
    @RequirePermission("hr:plan:delete")
    @OperationLog(module = "发展规划", action = "删除规划")
    public Result<Void> delete(@PathVariable Long id) {
        developmentPlanAppService.deletePlan(id);
        return Result.success();
    }

    @Operation(summary = "提交规划")
    @PostMapping("/{id}/submit")
    @RequirePermission("hr:plan:edit")
    @OperationLog(module = "发展规划", action = "提交规划")
    public Result<Void> submit(@PathVariable Long id) {
        developmentPlanAppService.submitPlan(id);
        return Result.success();
    }

    @Operation(summary = "审核规划")
    @PostMapping("/{id}/review")
    @RequirePermission("hr:plan:review")
    @OperationLog(module = "发展规划", action = "审核规划")
    public Result<Void> review(@PathVariable Long id,
                                @RequestParam(required = false) String comment) {
        developmentPlanAppService.reviewPlan(id, comment);
        return Result.success();
    }

    @Operation(summary = "更新里程碑状态")
    @PostMapping("/milestones/{milestoneId}/status")
    @RequirePermission("hr:plan:edit")
    @OperationLog(module = "发展规划", action = "更新里程碑")
    public Result<Void> updateMilestoneStatus(
            @PathVariable Long milestoneId,
            @RequestParam String status,
            @RequestParam(required = false) String completionNote) {
        developmentPlanAppService.updateMilestoneStatus(milestoneId, status, completionNote);
        return Result.success();
    }
}
