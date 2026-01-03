package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateCareerLevelCommand;
import com.lawfirm.application.hr.command.CreatePromotionCommand;
import com.lawfirm.application.hr.command.SubmitReviewCommand;
import com.lawfirm.application.hr.dto.CareerLevelDTO;
import com.lawfirm.application.hr.dto.PromotionApplicationDTO;
import com.lawfirm.application.hr.service.PromotionAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 晋升管理控制器
 */
@Tag(name = "晋升管理", description = "职级通道与晋升申请管理接口")
@RestController
@RequestMapping("/api/hr/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionAppService promotionAppService;

    // ==================== 职级管理 ====================

    @Operation(summary = "分页查询职级")
    @GetMapping("/levels")
    @RequirePermission("hr:level:list")
    public Result<PageResult<CareerLevelDTO>> listLevels(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "通道类别") @RequestParam(required = false) String category,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        PageResult<CareerLevelDTO> result = promotionAppService.listLevels(
                pageNum, pageSize, keyword, category, status);
        return Result.success(result);
    }

    @Operation(summary = "获取职级详情")
    @GetMapping("/levels/{id}")
    @RequirePermission("hr:level:view")
    public Result<CareerLevelDTO> getLevelById(@PathVariable Long id) {
        CareerLevelDTO level = promotionAppService.getLevelById(id);
        return Result.success(level);
    }

    @Operation(summary = "按类别获取职级列表")
    @GetMapping("/levels/category/{category}")
    public Result<List<CareerLevelDTO>> getLevelsByCategory(@PathVariable String category) {
        List<CareerLevelDTO> levels = promotionAppService.getLevelsByCategory(category);
        return Result.success(levels);
    }

    @Operation(summary = "创建职级")
    @PostMapping("/levels")
    @RequirePermission("hr:level:create")
    @OperationLog(module = "晋升管理", action = "创建职级")
    public Result<CareerLevelDTO> createLevel(@RequestBody @Valid CreateCareerLevelCommand command) {
        CareerLevelDTO level = promotionAppService.createLevel(command);
        return Result.success(level);
    }

    @Operation(summary = "更新职级")
    @PutMapping("/levels/{id}")
    @RequirePermission("hr:level:edit")
    @OperationLog(module = "晋升管理", action = "更新职级")
    public Result<CareerLevelDTO> updateLevel(@PathVariable Long id,
                                               @RequestBody @Valid CreateCareerLevelCommand command) {
        CareerLevelDTO level = promotionAppService.updateLevel(id, command);
        return Result.success(level);
    }

    @Operation(summary = "删除职级")
    @DeleteMapping("/levels/{id}")
    @RequirePermission("hr:level:delete")
    @OperationLog(module = "晋升管理", action = "删除职级")
    public Result<Void> deleteLevel(@PathVariable Long id) {
        promotionAppService.deleteLevel(id);
        return Result.success();
    }

    @Operation(summary = "启用职级")
    @PostMapping("/levels/{id}/enable")
    @RequirePermission("hr:level:edit")
    @OperationLog(module = "晋升管理", action = "启用职级")
    public Result<Void> enableLevel(@PathVariable Long id) {
        promotionAppService.changeLevelStatus(id, "ACTIVE");
        return Result.success();
    }

    @Operation(summary = "停用职级")
    @PostMapping("/levels/{id}/disable")
    @RequirePermission("hr:level:edit")
    @OperationLog(module = "晋升管理", action = "停用职级")
    public Result<Void> disableLevel(@PathVariable Long id) {
        promotionAppService.changeLevelStatus(id, "INACTIVE");
        return Result.success();
    }

    // ==================== 晋升申请 ====================

    @Operation(summary = "分页查询晋升申请")
    @GetMapping("/applications")
    @RequirePermission("hr:promotion:list")
    public Result<PageResult<PromotionApplicationDTO>> listApplications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "员工ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId) {
        PageResult<PromotionApplicationDTO> result = promotionAppService.listApplications(
                pageNum, pageSize, keyword, status, employeeId, departmentId);
        return Result.success(result);
    }

    @Operation(summary = "获取晋升申请详情")
    @GetMapping("/applications/{id}")
    @RequirePermission("hr:promotion:view")
    public Result<PromotionApplicationDTO> getApplicationById(@PathVariable Long id) {
        PromotionApplicationDTO app = promotionAppService.getApplicationById(id);
        return Result.success(app);
    }

    @Operation(summary = "提交晋升申请")
    @PostMapping("/applications")
    @RequirePermission("hr:promotion:apply")
    @OperationLog(module = "晋升管理", action = "提交晋升申请")
    public Result<PromotionApplicationDTO> submitApplication(@RequestBody @Valid CreatePromotionCommand command) {
        PromotionApplicationDTO app = promotionAppService.submitApplication(command);
        return Result.success(app);
    }

    @Operation(summary = "取消晋升申请")
    @PostMapping("/applications/{id}/cancel")
    @RequirePermission("hr:promotion:apply")
    @OperationLog(module = "晋升管理", action = "取消晋升申请")
    public Result<Void> cancelApplication(@PathVariable Long id) {
        promotionAppService.cancelApplication(id);
        return Result.success();
    }

    @Operation(summary = "提交评审")
    @PostMapping("/applications/review")
    @RequirePermission("hr:promotion:review")
    @OperationLog(module = "晋升管理", action = "提交评审")
    public Result<Void> submitReview(@RequestBody @Valid SubmitReviewCommand command) {
        promotionAppService.submitReview(command);
        return Result.success();
    }

    @Operation(summary = "最终审批-通过")
    @PostMapping("/applications/{id}/approve")
    @RequirePermission("hr:promotion:approve")
    @OperationLog(module = "晋升管理", action = "审批通过")
    public Result<Void> approve(@PathVariable Long id,
                                 @RequestParam(required = false) String comment,
                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate effectiveDate) {
        promotionAppService.approve(id, true, comment, effectiveDate);
        return Result.success();
    }

    @Operation(summary = "最终审批-拒绝")
    @PostMapping("/applications/{id}/reject")
    @RequirePermission("hr:promotion:approve")
    @OperationLog(module = "晋升管理", action = "审批拒绝")
    public Result<Void> reject(@PathVariable Long id,
                                @RequestParam(required = false) String comment) {
        promotionAppService.approve(id, false, comment, null);
        return Result.success();
    }

    @Operation(summary = "统计待审批数量")
    @GetMapping("/applications/pending-count")
    public Result<Integer> countPending() {
        int count = promotionAppService.countPending();
        return Result.success(count);
    }
}
