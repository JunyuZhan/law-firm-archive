package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.destruction.DestructionApplyRequest;
import com.archivesystem.dto.destruction.DestructionBatchApplyRequest;
import com.archivesystem.dto.destruction.DestructionBatchExecuteRequest;
import com.archivesystem.dto.destruction.DestructionRejectRequest;
import com.archivesystem.entity.DestructionRecord;
import com.archivesystem.service.DestructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 销毁管理控制器.
 */
@RestController
@RequestMapping("/destructions")
@RequiredArgsConstructor
@Validated
@Tag(name = "销毁管理", description = "档案销毁申请、审批、执行")
public class DestructionController {

    private final DestructionService destructionService;

    /**
     * 申请销毁.
     */
    @PostMapping("/apply")
    @Operation(summary = "申请销毁")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<DestructionRecord> apply(@Valid @RequestBody DestructionApplyRequest request) {
        DestructionRecord record = destructionService.apply(
            request.getArchiveId(), 
            request.getDestructionReason(), 
            request.getDestructionMethod()
        );
        return Result.success("销毁申请已提交", record);
    }

    /**
     * 批量申请销毁.
     */
    @PostMapping("/batch-apply")
    @Operation(summary = "批量申请销毁")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<List<DestructionRecord>> batchApply(@Valid @RequestBody DestructionBatchApplyRequest request) {
        List<DestructionRecord> records = destructionService.batchApply(
            request.getArchiveIds(), 
            request.getDestructionReason(), 
            request.getDestructionMethod()
        );
        return Result.success("批量销毁申请已提交", records);
    }

    /**
     * 获取销毁记录详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取销毁记录详情")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<DestructionRecord> getById(@PathVariable @Parameter(description = "销毁记录ID") Long id) {
        DestructionRecord record = destructionService.getById(id);
        return Result.success(record);
    }

    /**
     * 获取销毁记录列表.
     */
    @GetMapping
    @Operation(summary = "获取销毁记录列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<PageResult<DestructionRecord>> getList(
            @RequestParam(required = false) @Parameter(description = "状态") String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        PageResult<DestructionRecord> result = destructionService.getList(status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取待审批列表.
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审批列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<PageResult<DestructionRecord>> getPendingList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        PageResult<DestructionRecord> result = destructionService.getPendingList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取待执行列表（已审批）.
     */
    @GetMapping("/approved")
    @Operation(summary = "获取待执行列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<PageResult<DestructionRecord>> getApprovedList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        PageResult<DestructionRecord> result = destructionService.getApprovedList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取档案的销毁记录.
     */
    @GetMapping("/archive/{archiveId}")
    @Operation(summary = "获取档案的销毁记录")
    @PreAuthorize("isAuthenticated()")
    public Result<List<DestructionRecord>> getByArchiveId(
            @PathVariable @Parameter(description = "档案ID") Long archiveId) {
        List<DestructionRecord> records = destructionService.getByArchiveId(archiveId);
        return Result.success(records);
    }

    /**
     * 审批通过.
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> approve(
            @PathVariable @Parameter(description = "销毁记录ID") Long id,
            @RequestParam(required = false) @Parameter(description = "审批意见") String comment) {
        destructionService.approve(id, comment);
        return Result.success("审批通过", null);
    }

    /**
     * 审批拒绝.
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> reject(
            @PathVariable @Parameter(description = "销毁记录ID") Long id,
            @Valid @RequestBody DestructionRejectRequest request) {
        destructionService.reject(id, request.getComment());
        return Result.success("已拒绝", null);
    }

    /**
     * 执行销毁.
     */
    @PutMapping("/{id}/execute")
    @Operation(summary = "执行销毁")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> execute(
            @PathVariable @Parameter(description = "销毁记录ID") Long id,
            @RequestParam(required = false) @Parameter(description = "执行备注") String remarks) {
        destructionService.execute(id, remarks);
        return Result.success("销毁已执行", null);
    }

    /**
     * 批量执行销毁.
     */
    @PutMapping("/batch-execute")
    @Operation(summary = "批量执行销毁")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> batchExecute(@Valid @RequestBody DestructionBatchExecuteRequest request) {
        destructionService.batchExecute(request.getIds(), request.getRemarks());
        return Result.success("批量销毁已执行", null);
    }
}
