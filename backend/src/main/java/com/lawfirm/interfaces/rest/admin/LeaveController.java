package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.ApplyLeaveCommand;
import com.lawfirm.application.admin.command.ApproveLeaveCommand;
import com.lawfirm.application.admin.dto.LeaveApplicationDTO;
import com.lawfirm.application.admin.dto.LeaveApplicationQueryDTO;
import com.lawfirm.application.admin.dto.LeaveBalanceDTO;
import com.lawfirm.application.admin.dto.LeaveTypeDTO;
import com.lawfirm.application.admin.service.LeaveAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 请假管理接口 */
@Tag(name = "请假管理", description = "请假管理相关接口")
@RestController
@RequestMapping("/admin/leave")
@RequiredArgsConstructor
public class LeaveController {

  /** 请假应用服务 */
  private final LeaveAppService leaveAppService;

  /**
   * 获取请假类型列表
   *
   * @return 请假类型列表
   */
  @Operation(summary = "获取请假类型列表")
  @GetMapping("/types")
  public Result<List<LeaveTypeDTO>> listTypes() {
    return Result.success(leaveAppService.listLeaveTypes());
  }

  /**
   * 分页查询请假申请
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询请假申请")
  @GetMapping("/applications")
  @RequirePermission("admin:leave:list")
  public Result<PageResult<LeaveApplicationDTO>> listApplications(
      final LeaveApplicationQueryDTO query) {
    return Result.success(leaveAppService.listApplications(query));
  }

  /**
   * 提交请假申请
   *
   * @param command 申请命令
   * @return 申请记录
   */
  @Operation(summary = "提交请假申请")
  @PostMapping("/applications")
  @OperationLog(module = "请假管理", action = "提交请假申请")
  public Result<LeaveApplicationDTO> applyLeave(@RequestBody final ApplyLeaveCommand command) {
    return Result.success(leaveAppService.applyLeave(command));
  }

  /**
   * 审批请假申请
   *
   * @param command 审批命令
   * @return 申请记录
   */
  @Operation(summary = "审批请假申请")
  @PostMapping("/applications/approve")
  @RequirePermission("admin:leave:approve")
  @OperationLog(module = "请假管理", action = "审批请假申请")
  public Result<LeaveApplicationDTO> approveLeave(@RequestBody final ApproveLeaveCommand command) {
    return Result.success(leaveAppService.approveLeave(command));
  }

  /**
   * 取消请假申请
   *
   * @param id 申请ID
   * @return 无返回
   */
  @Operation(summary = "取消请假申请")
  @PostMapping("/applications/{id}/cancel")
  @OperationLog(module = "请假管理", action = "取消请假申请")
  public Result<Void> cancelApplication(@PathVariable final Long id) {
    leaveAppService.cancelApplication(id);
    return Result.success();
  }

  /**
   * 获取待审批列表
   *
   * @return 待审批列表
   */
  @Operation(summary = "获取待审批列表")
  @GetMapping("/applications/pending")
  @RequirePermission("admin:leave:approve")
  public Result<List<LeaveApplicationDTO>> getPendingApplications() {
    return Result.success(leaveAppService.getPendingApplications());
  }

  /**
   * 获取假期余额
   *
   * @param userId 用户ID
   * @param year 年份
   * @return 余额列表
   */
  @Operation(summary = "获取假期余额")
  @GetMapping("/balance")
  public Result<List<LeaveBalanceDTO>> getUserBalance(
      @RequestParam(required = false) final Long userId,
      @RequestParam(required = false) final Integer year) {
    return Result.success(leaveAppService.getUserBalance(userId, year));
  }

  /**
   * 初始化用户年度假期余额
   *
   * @param userId 用户ID
   * @param year 年份
   * @return 无返回
   */
  @Operation(summary = "初始化用户年度假期余额")
  @PostMapping("/balance/init")
  @RequirePermission("admin:leave:manage")
  @OperationLog(module = "请假管理", action = "初始化用户年度假期余额")
  public Result<Void> initUserBalance(
      @RequestParam final Long userId, @RequestParam final Integer year) {
    leaveAppService.initUserBalance(userId, year);
    return Result.success();
  }
}
