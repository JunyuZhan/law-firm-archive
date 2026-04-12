package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.appraisal.AppraisalCreateRequest;
import com.archivesystem.dto.appraisal.AppraisalRejectRequest;
import com.archivesystem.entity.AppraisalRecord;
import com.archivesystem.service.AppraisalService;
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
 * 鉴定管理控制器.
 * @author junyuzhan
 */
@RestController
@RequestMapping("/appraisals")
@RequiredArgsConstructor
@Validated
@Tag(name = "鉴定管理", description = "档案鉴定申请、审批")
public class AppraisalController {

    private final AppraisalService appraisalService;

    /**
     * 发起鉴定.
     */
    @PostMapping
    @Operation(summary = "发起鉴定")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<AppraisalRecord> create(@Valid @RequestBody AppraisalCreateRequest request) {
        AppraisalRecord record = appraisalService.create(
            request.getArchiveId(), 
            request.getAppraisalType(), 
            request.getOriginalValue(), 
            request.getNewValue(), 
            request.getAppraisalReason()
        );
        return Result.success("鉴定申请提交成功", record);
    }

    /**
     * 获取鉴定详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取鉴定详情")
    @PreAuthorize("isAuthenticated()")
    public Result<AppraisalRecord> getById(@PathVariable Long id) {
        AppraisalRecord record = appraisalService.getById(id);
        return Result.success(record);
    }

    /**
     * 获取鉴定列表.
     */
    @GetMapping
    @Operation(summary = "获取鉴定列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<PageResult<AppraisalRecord>> getList(
            @RequestParam(required = false) @Parameter(description = "鉴定类型") String type,
            @RequestParam(required = false) @Parameter(description = "状态") String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        PageResult<AppraisalRecord> result = appraisalService.getList(type, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取待审批列表.
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审批列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<PageResult<AppraisalRecord>> getPendingList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        PageResult<AppraisalRecord> result = appraisalService.getPendingList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取档案的鉴定历史.
     */
    @GetMapping("/archive/{archiveId}")
    @Operation(summary = "获取档案的鉴定历史")
    @PreAuthorize("isAuthenticated()")
    public Result<List<AppraisalRecord>> getByArchiveId(
            @PathVariable @Parameter(description = "档案ID") Long archiveId) {
        List<AppraisalRecord> records = appraisalService.getByArchiveId(archiveId);
        return Result.success(records);
    }

    /**
     * 审批通过.
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> approve(
            @PathVariable @Parameter(description = "鉴定记录ID") Long id,
            @RequestParam(required = false) @Parameter(description = "审批意见") String comment) {
        appraisalService.approve(id, comment);
        return Result.success("审批通过", null);
    }

    /**
     * 审批拒绝.
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> reject(
            @PathVariable @Parameter(description = "鉴定记录ID") Long id,
            @Valid @RequestBody AppraisalRejectRequest request) {
        appraisalService.reject(id, request.getComment());
        return Result.success("已拒绝", null);
    }
}
