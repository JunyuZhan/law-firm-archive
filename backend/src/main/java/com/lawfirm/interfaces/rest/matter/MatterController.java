package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CloseMatterCommand;
import com.lawfirm.application.matter.command.CreateMatterCommand;
import com.lawfirm.application.matter.command.UpdateMatterCommand;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.application.matter.dto.MatterQueryDTO;
import com.lawfirm.application.matter.dto.MatterTimelineDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.application.matter.service.MatterTimelineAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 案件管理 Controller
 */
@RestController
@RequestMapping("/matter")
@RequiredArgsConstructor
public class MatterController {

    private final MatterAppService matterAppService;
    private final MatterTimelineAppService matterTimelineAppService;

    /**
     * 分页查询案件列表
     */
    @GetMapping("/list")
    @RequirePermission("matter:list")
    public Result<PageResult<MatterDTO>> listMatters(MatterQueryDTO query) {
        PageResult<MatterDTO> result = matterAppService.listMatters(query);
        return Result.success(result);
    }

    /**
     * 查询我的案件
     */
    @GetMapping("/my")
    public Result<PageResult<MatterDTO>> myMatters(MatterQueryDTO query) {
        query.setMyMatters(true);
        PageResult<MatterDTO> result = matterAppService.listMatters(query);
        return Result.success(result);
    }

    /**
     * 获取案件详情
     */
    @GetMapping("/{id}")
    @RequirePermission("matter:list")
    public Result<MatterDTO> getMatter(@PathVariable Long id) {
        MatterDTO matter = matterAppService.getMatterById(id);
        return Result.success(matter);
    }

    /**
     * 创建案件
     */
    @PostMapping
    @RequirePermission("matter:create")
    @OperationLog(module = "案件管理", action = "创建案件")
    public Result<MatterDTO> createMatter(@RequestBody @Valid CreateMatterCommand command) {
        MatterDTO matter = matterAppService.createMatter(command);
        return Result.success(matter);
    }

    /**
     * 更新案件
     */
    @PutMapping
    @RequirePermission("matter:update")
    @OperationLog(module = "案件管理", action = "更新案件")
    public Result<MatterDTO> updateMatter(@RequestBody @Valid UpdateMatterCommand command) {
        MatterDTO matter = matterAppService.updateMatter(command);
        return Result.success(matter);
    }

    /**
     * 删除案件
     */
    @DeleteMapping("/{id}")
    @RequirePermission("matter:delete")
    @OperationLog(module = "案件管理", action = "删除案件")
    public Result<Void> deleteMatter(@PathVariable Long id) {
        matterAppService.deleteMatter(id);
        return Result.success();
    }

    /**
     * 修改案件状态
     */
    @PutMapping("/{id}/status")
    @RequirePermission("matter:update")
    @OperationLog(module = "案件管理", action = "修改案件状态")
    public Result<Void> changeStatus(@PathVariable Long id,
                                      @RequestBody @Valid ChangeStatusRequest request) {
        matterAppService.changeStatus(id, request.getStatus());
        return Result.success();
    }

    /**
     * 添加团队成员
     */
    @PostMapping("/{id}/participant")
    @RequirePermission("matter:update")
    @OperationLog(module = "案件管理", action = "添加团队成员")
    public Result<Void> addParticipant(@PathVariable Long id,
                                        @RequestBody @Valid AddParticipantRequest request) {
        matterAppService.addParticipant(id, request.getUserId(), request.getRole(),
                request.getCommissionRate(), request.getIsOriginator());
        return Result.success();
    }

    /**
     * 移除团队成员
     */
    @DeleteMapping("/{id}/participant/{userId}")
    @RequirePermission("matter:update")
    @OperationLog(module = "案件管理", action = "移除团队成员")
    public Result<Void> removeParticipant(@PathVariable Long id, @PathVariable Long userId) {
        matterAppService.removeParticipant(id, userId);
        return Result.success();
    }

    /**
     * 申请项目结案
     */
    @PostMapping("/{id}/close/apply")
    @RequirePermission("matter:close")
    @OperationLog(module = "案件管理", action = "申请项目结案")
    public Result<MatterDTO> applyCloseMatter(@PathVariable Long id, 
                                              @RequestBody @Valid CloseMatterCommand command) {
        command.setMatterId(id);
        MatterDTO matter = matterAppService.applyCloseMatter(command);
        return Result.success(matter);
    }

    /**
     * 审批项目结案
     */
    @PostMapping("/{id}/close/approve")
    @RequirePermission("matter:approve")
    @OperationLog(module = "案件管理", action = "审批项目结案")
    public Result<MatterDTO> approveCloseMatter(@PathVariable Long id,
                                                 @RequestBody @Valid ApproveCloseRequest request) {
        MatterDTO matter = matterAppService.approveCloseMatter(id, request.getApproved(), request.getComment());
        return Result.success(matter);
    }

    /**
     * 生成结案报告
     */
    @GetMapping("/{id}/close/report")
    @RequirePermission("matter:view")
    @OperationLog(module = "案件管理", action = "生成结案报告")
    public Result<String> generateCloseReport(@PathVariable Long id) {
        String report = matterAppService.generateCloseReport(id);
        return Result.success(report);
    }

    /**
     * 获取项目时间线（M3-024，P2）
     */
    @GetMapping("/{id}/timeline")
    @RequirePermission("matter:view")
    @Operation(summary = "获取项目时间线", description = "获取项目的所有关键事件时间线")
    public Result<List<MatterTimelineDTO>> getMatterTimeline(@PathVariable Long id) {
        List<MatterTimelineDTO> timeline = matterTimelineAppService.getMatterTimeline(id);
        return Result.success(timeline);
    }

    // ========== Request DTOs ==========

    @Data
    public static class ChangeStatusRequest {
        @NotBlank(message = "状态不能为空")
        private String status;
    }

    @Data
    public static class AddParticipantRequest {
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        @NotBlank(message = "角色不能为空")
        private String role;

        private BigDecimal commissionRate;
        private Boolean isOriginator;
    }

    @Data
    public static class ApproveCloseRequest {
        @NotNull(message = "审批结果不能为空")
        private Boolean approved;

        private String comment;
    }
}

