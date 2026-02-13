package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.DestructionRecord;
import com.archivesystem.service.DestructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 销毁管理控制器.
 */
@RestController
@RequestMapping("/destructions")
@RequiredArgsConstructor
@Tag(name = "销毁管理", description = "档案销毁申请、审批、执行")
public class DestructionController {

    private final DestructionService destructionService;

    /**
     * 申请销毁.
     */
    @PostMapping("/apply")
    @Operation(summary = "申请销毁")
    public Result<DestructionRecord> apply(@RequestBody Map<String, Object> params) {
        Long archiveId = Long.valueOf(params.get("archiveId").toString());
        String destructionReason = (String) params.get("destructionReason");
        String destructionMethod = (String) params.get("destructionMethod");

        DestructionRecord record = destructionService.apply(archiveId, destructionReason, destructionMethod);
        return Result.success("销毁申请已提交", record);
    }

    /**
     * 批量申请销毁.
     */
    @PostMapping("/batch-apply")
    @Operation(summary = "批量申请销毁")
    @SuppressWarnings("unchecked")
    public Result<List<DestructionRecord>> batchApply(@RequestBody Map<String, Object> params) {
        List<Number> archiveIdList = (List<Number>) params.get("archiveIds");
        List<Long> archiveIds = archiveIdList.stream().map(Number::longValue).toList();
        String destructionReason = (String) params.get("destructionReason");
        String destructionMethod = (String) params.get("destructionMethod");

        List<DestructionRecord> records = destructionService.batchApply(archiveIds, destructionReason, destructionMethod);
        return Result.success("批量销毁申请已提交", records);
    }

    /**
     * 获取销毁记录详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取销毁记录详情")
    public Result<DestructionRecord> getById(@PathVariable Long id) {
        DestructionRecord record = destructionService.getById(id);
        return Result.success(record);
    }

    /**
     * 获取销毁记录列表.
     */
    @GetMapping
    @Operation(summary = "获取销毁记录列表")
    public Result<PageResult<DestructionRecord>> getList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<DestructionRecord> result = destructionService.getList(status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取待审批列表.
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审批列表")
    public Result<PageResult<DestructionRecord>> getPendingList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<DestructionRecord> result = destructionService.getPendingList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取待执行列表（已审批）.
     */
    @GetMapping("/approved")
    @Operation(summary = "获取待执行列表")
    public Result<PageResult<DestructionRecord>> getApprovedList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<DestructionRecord> result = destructionService.getApprovedList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取档案的销毁记录.
     */
    @GetMapping("/archive/{archiveId}")
    @Operation(summary = "获取档案的销毁记录")
    public Result<List<DestructionRecord>> getByArchiveId(@PathVariable Long archiveId) {
        List<DestructionRecord> records = destructionService.getByArchiveId(archiveId);
        return Result.success(records);
    }

    /**
     * 审批通过.
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        destructionService.approve(id, comment);
        return Result.success("审批通过", null);
    }

    /**
     * 审批拒绝.
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<Void> reject(
            @PathVariable Long id,
            @RequestParam String comment) {
        destructionService.reject(id, comment);
        return Result.success("已拒绝", null);
    }

    /**
     * 执行销毁.
     */
    @PutMapping("/{id}/execute")
    @Operation(summary = "执行销毁")
    public Result<Void> execute(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        destructionService.execute(id, remarks);
        return Result.success("销毁已执行", null);
    }

    /**
     * 批量执行销毁.
     */
    @PutMapping("/batch-execute")
    @Operation(summary = "批量执行销毁")
    @SuppressWarnings("unchecked")
    public Result<Void> batchExecute(@RequestBody Map<String, Object> params) {
        List<Number> idList = (List<Number>) params.get("ids");
        List<Long> ids = idList.stream().map(Number::longValue).toList();
        String remarks = (String) params.get("remarks");

        destructionService.batchExecute(ids, remarks);
        return Result.success("批量销毁已执行", null);
    }
}
