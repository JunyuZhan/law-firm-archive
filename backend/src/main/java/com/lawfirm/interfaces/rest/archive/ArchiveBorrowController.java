package com.lawfirm.interfaces.rest.archive;

import com.lawfirm.application.archive.command.CreateBorrowCommand;
import com.lawfirm.application.archive.command.ReturnArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveBorrowDTO;
import com.lawfirm.application.archive.service.ArchiveBorrowAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 档案借阅 Controller
 */
@RestController
@RequestMapping("/archive/borrow")
@RequiredArgsConstructor
public class ArchiveBorrowController {

    private final ArchiveBorrowAppService borrowAppService;

    /**
     * 分页查询借阅记录
     */
    @GetMapping("/list")
    @RequirePermission("archive:borrow:list")
    public Result<PageResult<ArchiveBorrowDTO>> listBorrows(PageQuery query,
                                                            @RequestParam(required = false) Long archiveId,
                                                            @RequestParam(required = false) String status) {
        PageResult<ArchiveBorrowDTO> result = borrowAppService.listBorrows(query, archiveId, status);
        return Result.success(result);
    }

    /**
     * 创建借阅申请
     */
    @PostMapping
    @RequirePermission("archive:borrow:create")
    @OperationLog(module = "档案借阅", action = "申请借阅")
    public Result<ArchiveBorrowDTO> createBorrow(@RequestBody @Valid CreateBorrowCommand command) {
        ArchiveBorrowDTO borrow = borrowAppService.createBorrow(command);
        return Result.success(borrow);
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @RequirePermission("archive:borrow:approve")
    @OperationLog(module = "档案借阅", action = "审批通过")
    public Result<Void> approveBorrow(@PathVariable Long id) {
        borrowAppService.approveBorrow(id);
        return Result.success();
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/{id}/reject")
    @RequirePermission("archive:borrow:approve")
    @OperationLog(module = "档案借阅", action = "审批拒绝")
    public Result<Void> rejectBorrow(@PathVariable Long id, @RequestBody RejectRequest request) {
        borrowAppService.rejectBorrow(id, request.getReason());
        return Result.success();
    }

    /**
     * 确认借出
     */
    @PostMapping("/{id}/confirm")
    @RequirePermission("archive:borrow:confirm")
    @OperationLog(module = "档案借阅", action = "确认借出")
    public Result<Void> confirmBorrow(@PathVariable Long id) {
        borrowAppService.confirmBorrow(id);
        return Result.success();
    }

    /**
     * 归还档案
     */
    @PostMapping("/return")
    @RequirePermission("archive:borrow:return")
    @OperationLog(module = "档案借阅", action = "归还档案")
    public Result<Void> returnArchive(@RequestBody @Valid ReturnArchiveCommand command) {
        borrowAppService.returnArchive(command);
        return Result.success();
    }

    /**
     * 获取逾期借阅列表
     */
    @GetMapping("/overdue")
    @RequirePermission("archive:borrow:list")
    public Result<List<ArchiveBorrowDTO>> getOverdueBorrows() {
        List<ArchiveBorrowDTO> borrows = borrowAppService.getOverdueBorrows();
        return Result.success(borrows);
    }

    // ========== Request DTOs ==========

    @Data
    public static class RejectRequest {
        private String reason;
    }
}

