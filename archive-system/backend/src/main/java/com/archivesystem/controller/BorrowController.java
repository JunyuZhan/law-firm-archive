package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@Tag(name = "借阅管理", description = "借阅申请、审批、归还")
public class BorrowController {

    private final BorrowService borrowService;

    /**
     * 提交借阅申请.
     */
    @PostMapping("/apply")
    @Operation(summary = "提交借阅申请")
    public Result<BorrowApplication> apply(@RequestBody Map<String, Object> params) {
        Long archiveId = Long.valueOf(params.get("archiveId").toString());
        String borrowPurpose = (String) params.get("borrowPurpose");
        LocalDate expectedReturnDate = LocalDate.parse(params.get("expectedReturnDate").toString());
        String remarks = (String) params.get("remarks");

        BorrowApplication application = borrowService.apply(archiveId, borrowPurpose, expectedReturnDate, remarks);
        return Result.success("申请提交成功", application);
    }

    /**
     * 获取借阅申请详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取借阅申请详情")
    public Result<BorrowApplication> getById(@PathVariable Long id) {
        BorrowApplication application = borrowService.getById(id);
        return Result.success(application);
    }

    /**
     * 获取我的申请列表.
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的申请列表")
    public Result<PageResult<BorrowApplication>> getMyApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        PageResult<BorrowApplication> result = borrowService.getMyApplications(userId, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 取消申请.
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消申请")
    public Result<Void> cancel(@PathVariable Long id) {
        borrowService.cancel(id);
        return Result.success("取消成功", null);
    }

    /**
     * 获取待审批列表.
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审批列表")
    public Result<PageResult<BorrowApplication>> getPendingList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<BorrowApplication> result = borrowService.getPendingList(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 审批通过.
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        borrowService.approve(id, remarks);
        return Result.success("审批通过", null);
    }

    /**
     * 审批拒绝.
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<Void> reject(
            @PathVariable Long id,
            @RequestParam String reason) {
        borrowService.reject(id, reason);
        return Result.success("已拒绝", null);
    }

    /**
     * 借出档案.
     */
    @PutMapping("/{id}/lend")
    @Operation(summary = "借出档案")
    public Result<Void> lend(@PathVariable Long id) {
        borrowService.lend(id);
        return Result.success("借出成功", null);
    }

    /**
     * 归还档案.
     */
    @PutMapping("/{id}/return")
    @Operation(summary = "归还档案")
    public Result<Void> returnArchive(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        borrowService.returnArchive(id, remarks);
        return Result.success("归还成功", null);
    }

    /**
     * 续借.
     */
    @PutMapping("/{id}/renew")
    @Operation(summary = "续借")
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
    public Result<List<BorrowApplication>> getOverdueList() {
        List<BorrowApplication> list = borrowService.getOverdueList();
        return Result.success(list);
    }

    /**
     * 检查档案是否可借阅.
     */
    @GetMapping("/check/{archiveId}")
    @Operation(summary = "检查档案是否可借阅")
    public Result<Map<String, Object>> checkAvailable(@PathVariable Long archiveId) {
        BorrowApplication current = borrowService.getCurrentByArchiveId(archiveId);
        boolean available = current == null;
        
        return Result.success(Map.of(
                "available", available,
                "currentApplication", current
        ));
    }
}
