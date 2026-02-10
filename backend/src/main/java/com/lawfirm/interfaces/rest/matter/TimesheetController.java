package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateTimesheetCommand;
import com.lawfirm.application.matter.dto.TimesheetDTO;
import com.lawfirm.application.matter.dto.TimesheetQueryDTO;
import com.lawfirm.application.matter.dto.TimesheetSummaryDTO;
import com.lawfirm.application.matter.service.TimesheetAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 工时管理接口 */
@RestController
@RequestMapping("/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

  /** 工时应用服务 */
  private final TimesheetAppService timesheetAppService;

  /**
   * 分页查询工时
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("timesheet:list")
  public Result<PageResult<TimesheetDTO>> list(final TimesheetQueryDTO query) {
    return Result.success(timesheetAppService.listTimesheets(query));
  }

  /**
   * 获取工时详情
   *
   * @param id 工时ID
   * @return 工时信息
   */
  @GetMapping("/{id}")
  @RequirePermission("timesheet:view")
  public Result<TimesheetDTO> getById(@PathVariable final Long id) {
    return Result.success(timesheetAppService.getTimesheetById(id));
  }

  /**
   * 创建工时记录
   *
   * @param command 创建工时命令
   * @return 工时信息
   */
  @PostMapping
  @RequirePermission("timesheet:record")
  @OperationLog(module = "工时管理", action = "记录工时")
  public Result<TimesheetDTO> create(@Valid @RequestBody final CreateTimesheetCommand command) {
    return Result.success(timesheetAppService.createTimesheet(command));
  }

  /**
   * 更新工时记录
   *
   * @param id 工时ID
   * @param workDate 工作日期
   * @param hours 工时数
   * @param workType 工作类型
   * @param workContent 工作内容
   * @param billable 是否可计费
   * @return 工时信息
   */
  @PutMapping("/{id}")
  @RequirePermission("timesheet:record")
  @OperationLog(module = "工时管理", action = "更新工时")
  public Result<TimesheetDTO> update(
      @PathVariable final Long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          final LocalDate workDate,
      @RequestParam(required = false) final BigDecimal hours,
      @RequestParam(required = false) final String workType,
      @RequestParam(required = false) final String workContent,
      @RequestParam(required = false) final Boolean billable) {
    return Result.success(
        timesheetAppService.updateTimesheet(id, workDate, hours, workType, workContent, billable));
  }

  /**
   * 删除工时记录
   *
   * @param id 工时ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("timesheet:record")
  @OperationLog(module = "工时管理", action = "删除工时")
  public Result<Void> delete(@PathVariable final Long id) {
    timesheetAppService.deleteTimesheet(id);
    return Result.success();
  }

  /**
   * 提交工时
   *
   * @param id 工时ID
   * @return 工时信息
   */
  @PostMapping("/{id}/submit")
  @RequirePermission("timesheet:record")
  @OperationLog(module = "工时管理", action = "提交工时")
  public Result<TimesheetDTO> submit(@PathVariable final Long id) {
    return Result.success(timesheetAppService.submitTimesheet(id));
  }

  /**
   * 批量提交工时
   *
   * @param ids 工时ID列表
   * @return 空结果
   */
  @PostMapping("/batch-submit")
  @RequirePermission("timesheet:record")
  @OperationLog(module = "工时管理", action = "批量提交工时")
  public Result<Void> batchSubmit(
      @RequestBody @jakarta.validation.constraints.Size(min = 1, max = 100, message = "批量操作数量需在1-100之间")
          final List<Long> ids) {
    timesheetAppService.batchSubmit(ids);
    return Result.success();
  }

  /**
   * 审批通过
   *
   * @param id 工时ID
   * @param comment 审批意见
   * @return 工时信息
   */
  @PostMapping("/{id}/approve")
  @RequirePermission("timesheet:approve")
  @OperationLog(module = "工时管理", action = "审批通过")
  public Result<TimesheetDTO> approve(
      @PathVariable final Long id, @RequestParam(required = false) final String comment) {
    return Result.success(timesheetAppService.approveTimesheet(id, comment));
  }

  /**
   * 审批拒绝
   *
   * @param id 工时ID
   * @param comment 拒绝原因
   * @return 工时信息
   */
  @PostMapping("/{id}/reject")
  @RequirePermission("timesheet:approve")
  @OperationLog(module = "工时管理", action = "审批拒绝")
  public Result<TimesheetDTO> reject(
      @PathVariable final Long id, @RequestParam(required = false) final String comment) {
    return Result.success(timesheetAppService.rejectTimesheet(id, comment));
  }

  /**
   * 获取待审批列表
   *
   * @return 待审批工时列表
   */
  @GetMapping("/pending")
  @RequirePermission("timesheet:approve")
  public Result<List<TimesheetDTO>> getPending() {
    return Result.success(timesheetAppService.getPendingApproval());
  }

  /**
   * 获取我的工时
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 工时列表
   */
  @GetMapping("/my")
  @RequirePermission("timesheet:record")
  public Result<List<TimesheetDTO>> getMyTimesheets(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate) {
    return Result.success(timesheetAppService.getMyTimesheets(startDate, endDate));
  }

  /**
   * 获取用户月度汇总
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return 工时汇总信息
   */
  @GetMapping("/summary/user/{userId}")
  @RequirePermission("timesheet:list")
  public Result<TimesheetSummaryDTO> getUserSummary(
      @PathVariable final Long userId,
      @RequestParam final int year,
      @RequestParam final int month) {
    return Result.success(timesheetAppService.getUserMonthlySummary(userId, year, month));
  }

  /**
   * 获取案件工时汇总
   *
   * @param matterId 案件ID
   * @return 工时汇总信息
   */
  @GetMapping("/summary/matter/{matterId}")
  @RequirePermission("timesheet:list")
  public Result<TimesheetSummaryDTO> getMatterSummary(@PathVariable final Long matterId) {
    return Result.success(timesheetAppService.getMatterSummary(matterId));
  }
}
