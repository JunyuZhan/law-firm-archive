package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.AddPayrollItemCommand;
import com.lawfirm.application.hr.command.ApprovePayrollCommand;
import com.lawfirm.application.hr.command.ConfirmPayrollCommand;
import com.lawfirm.application.hr.command.CreatePayrollSheetCommand;
import com.lawfirm.application.hr.command.IssuePayrollCommand;
import com.lawfirm.application.hr.command.SubmitApprovalCommand;
import com.lawfirm.application.hr.command.UpdatePayrollItemCommand;
import com.lawfirm.application.hr.dto.PayrollItemDTO;
import com.lawfirm.application.hr.dto.PayrollSheetDTO;
import com.lawfirm.application.hr.dto.PayrollSheetQueryDTO;
import com.lawfirm.application.hr.service.PayrollAppService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import java.io.ByteArrayInputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 工资管理 Controller */
@RestController
@RequestMapping("/hr/payroll")
@RequiredArgsConstructor
public class PayrollController {

  /** 工资服务. */
  private final PayrollAppService payrollAppService;

  /**
   * 创建工资表（仅财务角色）
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping
  @RequirePermission("payroll:create")
  public Result<PayrollSheetDTO> createPayrollSheet(
      @RequestBody final CreatePayrollSheetCommand command) {
    // 权限检查在Service层进行
    PayrollSheetDTO dto = payrollAppService.createPayrollSheet(command);
    return Result.success(dto);
  }

  /**
   * 分页查询工资表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("payroll:list")
  public Result<PageResult<PayrollSheetDTO>> listPayrollSheets(final PayrollSheetQueryDTO query) {
    PageResult<PayrollSheetDTO> result = payrollAppService.listPayrollSheets(query);
    return Result.success(result);
  }

  /**
   * 查询工资表详情
   *
   * @param id 工资表ID
   * @return 工资表详情
   */
  @GetMapping("/{id}")
  @RequirePermission("payroll:view")
  public Result<PayrollSheetDTO> getPayrollSheetById(@PathVariable final Long id) {
    PayrollSheetDTO dto = payrollAppService.getPayrollSheetById(id);
    return Result.success(dto);
  }

  /**
   * 查询工资表的所有员工工资明细列表（用于列表展示）
   *
   * @param id 工资表ID
   * @return 工资明细列表
   */
  @GetMapping("/{id}/items")
  @RequirePermission("payroll:view")
  public Result<List<PayrollItemDTO>> getPayrollItemsBySheetId(@PathVariable final Long id) {
    List<PayrollItemDTO> items = payrollAppService.getPayrollItemsBySheetId(id);
    return Result.success(items);
  }

  /**
   * 根据年月查询员工工资明细列表
   *
   * @param year 年份
   * @param month 月份
   * @return 工资明细列表
   */
  @GetMapping("/items")
  @RequirePermission("payroll:view")
  public Result<List<PayrollItemDTO>> getPayrollItemsByYearMonth(
      @RequestParam final Integer year, @RequestParam final Integer month) {
    List<PayrollItemDTO> items = payrollAppService.getPayrollItemsByYearMonth(year, month);
    return Result.success(items);
  }

  /**
   * 为工资表添加员工工资明细
   *
   * @param id 工资表ID
   * @param command 添加命令
   * @return 工资明细
   */
  @PostMapping("/{id}/add-item")
  @RequirePermission("payroll:edit")
  public Result<PayrollItemDTO> addPayrollItem(
      @PathVariable final Long id, @RequestBody final AddPayrollItemCommand command) {
    PayrollItemDTO dto = payrollAppService.addPayrollItemForEmployee(id, command);
    return Result.success(dto);
  }

  /**
   * 更新工资明细
   *
   * @param itemId 明细ID
   * @param command 更新命令
   * @return 更新结果
   */
  @PutMapping("/item/{itemId}")
  @RequirePermission("payroll:edit")
  public Result<PayrollItemDTO> updatePayrollItem(
      @PathVariable final Long itemId, @RequestBody final UpdatePayrollItemCommand command) {
    command.setPayrollItemId(itemId);
    PayrollItemDTO dto = payrollAppService.updatePayrollItem(command);
    return Result.success(dto);
  }

  /**
   * 根据年月和员工ID更新或创建工资明细（用于没有工资表时也能编辑）
   *
   * @param year 年份
   * @param month 月份
   * @param employeeId 员工ID
   * @param command 更新命令
   * @return 更新结果
   */
  @PutMapping("/item/by-employee")
  @RequirePermission("payroll:edit")
  public Result<PayrollItemDTO> updatePayrollItemByEmployee(
      @RequestParam final Integer year,
      @RequestParam final Integer month,
      @RequestParam final Long employeeId,
      @RequestBody final UpdatePayrollItemCommand command) {
    PayrollItemDTO dto =
        payrollAppService.updateOrCreatePayrollItemByEmployee(year, month, employeeId, command);
    return Result.success(dto);
  }

  /**
   * 提交工资表（待确认）
   *
   * @param id 工资表ID
   * @return 无返回
   */
  @PostMapping("/{id}/submit")
  @RequirePermission("payroll:submit")
  public Result<Void> submitPayrollSheet(@PathVariable final Long id) {
    payrollAppService.submitPayrollSheet(id);
    return Result.success();
  }

  /**
   * 员工确认工资表
   *
   * @param command 确认命令
   * @return 无返回
   */
  @PostMapping("/item/confirm")
  @RequirePermission("payroll:confirm")
  public Result<Void> confirmPayrollItem(@RequestBody final ConfirmPayrollCommand command) {
    payrollAppService.confirmPayrollItem(command);
    return Result.success();
  }

  /**
   * 财务确认工资表 确认所有员工都已确认后，更新工资表状态为财务已确认
   *
   * @param id 工资表ID
   * @return 无返回
   */
  @PostMapping("/{id}/finance-confirm")
  @RequirePermission("payroll:finance:confirm")
  public Result<Void> financeConfirmPayrollSheet(@PathVariable final Long id) {
    payrollAppService.financeConfirmPayrollSheet(id);
    return Result.success();
  }

  /**
   * 提交审批（财务确认所有员工已确认后，提交给主任或合伙人审批）
   *
   * @param command 提交审批命令
   * @return 无返回
   */
  @PostMapping("/submit-approval")
  @RequirePermission("payroll:submit")
  public Result<Void> submitApproval(@RequestBody final SubmitApprovalCommand command) {
    payrollAppService.submitApproval(command);
    return Result.success();
  }

  /**
   * 审批工资表（主任或合伙人审批）
   *
   * @param command 审批命令
   * @return 无返回
   */
  @PostMapping("/approve")
  @RequirePermission("payroll:approve")
  public Result<Void> approvePayrollSheet(@RequestBody final ApprovePayrollCommand command) {
    payrollAppService.approvePayrollSheet(command);
    return Result.success();
  }

  /**
   * 发放工资
   *
   * @param id 工资表ID
   * @param command 发放命令
   * @return 无返回
   */
  @PostMapping("/{id}/issue")
  @RequirePermission("payroll:issue")
  public Result<Void> issuePayroll(
      @PathVariable final Long id, @RequestBody final IssuePayrollCommand command) {
    command.setPayrollSheetId(id);
    payrollAppService.issuePayroll(command);
    return Result.success();
  }

  /**
   * 查询我的工资表
   *
   * @param year 年份
   * @param month 月份
   * @return 我的工资表列表
   */
  @GetMapping("/my")
  @RequirePermission("payroll:my:view")
  public Result<List<PayrollSheetDTO>> getMyPayrollSheets(
      @RequestParam(required = false) final Integer year,
      @RequestParam(required = false) final Integer month) {
    List<PayrollSheetDTO> result = payrollAppService.getMyPayrollSheets(year, month);
    return Result.success(result);
  }

  /**
   * 查询我的工资明细
   *
   * @param id 工资表ID
   * @return 我的工资明细
   */
  @GetMapping("/my/{id}")
  @RequirePermission("payroll:my:view")
  public Result<PayrollSheetDTO> getMyPayrollSheetById(@PathVariable final Long id) {
    // 使用带权限检查的方法，确保员工只能查看自己的工资
    PayrollSheetDTO dto = payrollAppService.getMyPayrollSheetById(id);
    return Result.success(dto);
  }

  /**
   * 导出工资表为Excel（仅已审批通过的工资表可以导出）
   *
   * @param id 工资表ID
   * @return Excel文件流
   */
  @GetMapping("/{id}/export")
  @RequirePermission("payroll:export")
  public ResponseEntity<InputStreamResource> exportPayrollSheet(@PathVariable final Long id) {
    try {
      ByteArrayInputStream excelStream = payrollAppService.exportPayrollSheet(id);
      PayrollSheetDTO sheet = payrollAppService.getPayrollSheetById(id);
      String filename =
          String.format("%d年%d月工资表.xlsx", sheet.getPayrollYear(), sheet.getPayrollMonth());

      HttpHeaders headers = new HttpHeaders();
      headers.add(
          "Content-Disposition",
          "attachment; filename=" + new String(filename.getBytes("UTF-8"), "ISO-8859-1"));

      return ResponseEntity.ok()
          .headers(headers)
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(new InputStreamResource(excelStream));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
