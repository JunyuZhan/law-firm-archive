package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.borrow.BorrowApplyRequest;
import com.archivesystem.dto.borrow.BorrowRejectRequest;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 借阅管理控制器.
 */
@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
@Validated
@Tag(name = "借阅管理", description = "借阅申请、审批、归还")
public class BorrowController {

    private final BorrowService borrowService;

    /**
     * 提交借阅申请.
     */
    @PostMapping("/apply")
    @Operation(summary = "提交借阅申请")
    @PreAuthorize("isAuthenticated()")
    public Result<BorrowApplication> apply(@Valid @RequestBody BorrowApplyRequest request) {
        BorrowApplication application = borrowService.apply(
            request.getArchiveId(), 
            request.getBorrowPurpose(), 
            request.getExpectedReturnDate(), 
            request.getRemarks()
        );
        return Result.success("申请提交成功", application);
    }

    /**
     * 获取借阅申请详情.
     * 申请人可查看自己的申请，管理员可查看所有申请
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取借阅申请详情")
    @PreAuthorize("isAuthenticated()")
    public Result<BorrowApplication> getById(@PathVariable Long id) {
        BorrowApplication application = borrowService.getById(id);
        return Result.success(application);
    }

    /**
     * 获取我的申请列表.
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的申请列表")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<BorrowApplication>> getMyApplications(
            @RequestParam(required = false) @Parameter(description = "申请状态") String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        PageResult<BorrowApplication> result = borrowService.getMyApplications(userId, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 取消申请.
     * 仅申请人本人可取消
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消申请")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> cancel(@PathVariable Long id) {
        borrowService.cancel(id);
        return Result.success("取消成功", null);
    }

    /**
     * 获取待审批列表.
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审批列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<PageResult<BorrowApplication>> getPendingList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        PageResult<BorrowApplication> result = borrowService.getPendingList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取待借出列表（已审批通过，待借出）.
     */
    @GetMapping("/approved")
    @Operation(summary = "获取待借出列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<PageResult<BorrowApplication>> getApprovedList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        PageResult<BorrowApplication> result = borrowService.getApprovedList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 审批通过.
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> approve(
            @PathVariable @Parameter(description = "借阅申请ID") Long id,
            @RequestParam(required = false) @Parameter(description = "审批备注") String remarks) {
        borrowService.approve(id, remarks);
        return Result.success("审批通过", null);
    }

    /**
     * 审批拒绝.
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> reject(
            @PathVariable @Parameter(description = "借阅申请ID") Long id,
            @Valid @RequestBody BorrowRejectRequest request) {
        borrowService.reject(id, request.getReason());
        return Result.success("已拒绝", null);
    }

    /**
     * 借出档案.
     */
    @PutMapping("/{id}/lend")
    @Operation(summary = "借出档案")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> lend(@PathVariable @Parameter(description = "借阅申请ID") Long id) {
        borrowService.lend(id);
        return Result.success("借出成功", null);
    }

    /**
     * 归还档案.
     */
    @PutMapping("/{id}/return")
    @Operation(summary = "归还档案")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Void> returnArchive(
            @PathVariable @Parameter(description = "借阅申请ID") Long id,
            @RequestParam(required = false) @Parameter(description = "归还备注") String remarks) {
        borrowService.returnArchive(id, remarks);
        return Result.success("归还成功", null);
    }

    /**
     * 续借.
     * 申请人或管理员可续借
     */
    @PutMapping("/{id}/renew")
    @Operation(summary = "续借")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> renew(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newReturnDate) {
        borrowService.renew(id, newReturnDate);
        return Result.success("续借成功", null);
    }

    /**
     * 获取逾期列表.
     */
    @GetMapping("/overdue")
    @Operation(summary = "获取逾期列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<List<BorrowApplication>> getOverdueList() {
        List<BorrowApplication> list = borrowService.getOverdueList();
        return Result.success(list);
    }

    /**
     * 检查档案是否可借阅.
     */
    @GetMapping("/check/{archiveId}")
    @Operation(summary = "检查档案是否可借阅")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> checkAvailable(@PathVariable Long archiveId) {
        BorrowApplication current = borrowService.getCurrentByArchiveId(archiveId);
        boolean available = current == null;
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("available", available);
        if (current != null) {
            result.put("currentApplication", current);
        }
        return Result.success(result);
    }
}
