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
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 档案借阅 Controller */
@RestController
@RequestMapping("/archive/borrow")
@RequiredArgsConstructor
public class ArchiveBorrowController {

  /** 档案借阅应用服务 */
  private final ArchiveBorrowAppService borrowAppService;

  /**
   * 分页查询借阅记录
   *
   * @param query 分页查询条件
   * @param archiveId 档案ID（可选）
   * @param status 状态（可选）
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("archive:borrow")
  public Result<PageResult<ArchiveBorrowDTO>> listBorrows(
      final PageQuery query,
      @RequestParam(required = false) final Long archiveId,
      @RequestParam(required = false) final String status) {
    PageResult<ArchiveBorrowDTO> result = borrowAppService.listBorrows(query, archiveId, status);
    return Result.success(result);
  }

  /**
   * 创建借阅申请
   *
   * @param command 创建借阅命令
   * @return 借阅记录
   */
  @PostMapping
  @RequirePermission("archive:borrow:create")
  @OperationLog(module = "档案借阅", action = "申请借阅")
  public Result<ArchiveBorrowDTO> createBorrow(
      @RequestBody @Valid final CreateBorrowCommand command) {
    ArchiveBorrowDTO borrow = borrowAppService.createBorrow(command);
    return Result.success(borrow);
  }

  /**
   * 审批通过
   *
   * @param id 借阅记录ID
   * @return 空结果
   */
  @PostMapping("/{id}/approve")
  @RequirePermission("archive:borrow:approve")
  @OperationLog(module = "档案借阅", action = "审批通过")
  public Result<Void> approveBorrow(@PathVariable final Long id) {
    borrowAppService.approveBorrow(id);
    return Result.success();
  }

  /**
   * 审批拒绝
   *
   * @param id 借阅记录ID
   * @param request 拒绝请求
   * @return 空结果
   */
  @PostMapping("/{id}/reject")
  @RequirePermission("archive:borrow:approve")
  @OperationLog(module = "档案借阅", action = "审批拒绝")
  public Result<Void> rejectBorrow(
      @PathVariable final Long id, @RequestBody final RejectRequest request) {
    borrowAppService.rejectBorrow(id, request.getReason());
    return Result.success();
  }

  /**
   * 确认借出
   *
   * @param id 借阅记录ID
   * @return 空结果
   */
  @PostMapping("/{id}/confirm")
  @RequirePermission("archive:borrow:confirm")
  @OperationLog(module = "档案借阅", action = "确认借出")
  public Result<Void> confirmBorrow(@PathVariable final Long id) {
    borrowAppService.confirmBorrow(id);
    return Result.success();
  }

  /**
   * 归还档案
   *
   * @param command 归还档案命令
   * @return 空结果
   */
  @PostMapping("/return")
  @RequirePermission("archive:borrow:return")
  @OperationLog(module = "档案借阅", action = "归还档案")
  public Result<Void> returnArchive(@RequestBody @Valid final ReturnArchiveCommand command) {
    borrowAppService.returnArchive(command);
    return Result.success();
  }

  /**
   * 获取逾期借阅列表
   *
   * @return 逾期借阅列表
   */
  @GetMapping("/overdue")
  @RequirePermission("archive:borrow")
  public Result<List<ArchiveBorrowDTO>> getOverdueBorrows() {
    List<ArchiveBorrowDTO> borrows = borrowAppService.getOverdueBorrows();
    return Result.success(borrows);
  }

  // ========== Request DTOs ==========

  /** 拒绝请求 */
  @Data
  public static class RejectRequest {
    /** 拒绝原因 */
    private String reason;
  }
}
