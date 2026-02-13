package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.AppraisalRecord;
import com.archivesystem.service.AppraisalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 鉴定管理控制器.
 */
@RestController
@RequestMapping("/appraisals")
@RequiredArgsConstructor
@Tag(name = "鉴定管理", description = "档案鉴定申请、审批")
public class AppraisalController {

    private final AppraisalService appraisalService;

    /**
     * 发起鉴定.
     */
    @PostMapping
    @Operation(summary = "发起鉴定")
    public Result<AppraisalRecord> create(@RequestBody Map<String, Object> params) {
        Long archiveId = Long.valueOf(params.get("archiveId").toString());
        String appraisalType = (String) params.get("appraisalType");
        String originalValue = (String) params.get("originalValue");
        String newValue = (String) params.get("newValue");
        String appraisalReason = (String) params.get("appraisalReason");

        AppraisalRecord record = appraisalService.create(archiveId, appraisalType, originalValue, newValue, appraisalReason);
        return Result.success("鉴定申请提交成功", record);
    }

    /**
     * 获取鉴定详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取鉴定详情")
    public Result<AppraisalRecord> getById(@PathVariable Long id) {
        AppraisalRecord record = appraisalService.getById(id);
        return Result.success(record);
    }

    /**
     * 获取鉴定列表.
     */
    @GetMapping
    @Operation(summary = "获取鉴定列表")
    public Result<PageResult<AppraisalRecord>> getList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<AppraisalRecord> result = appraisalService.getList(type, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取待审批列表.
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审批列表")
    public Result<PageResult<AppraisalRecord>> getPendingList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<AppraisalRecord> result = appraisalService.getPendingList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取档案的鉴定历史.
     */
    @GetMapping("/archive/{archiveId}")
    @Operation(summary = "获取档案的鉴定历史")
    public Result<List<AppraisalRecord>> getByArchiveId(@PathVariable Long archiveId) {
        List<AppraisalRecord> records = appraisalService.getByArchiveId(archiveId);
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
        appraisalService.approve(id, comment);
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
        appraisalService.reject(id, comment);
        return Result.success("已拒绝", null);
    }
}
