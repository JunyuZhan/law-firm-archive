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
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * 工资管理 Controller
 */
@RestController
@RequestMapping("/hr/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollAppService payrollAppService;

    /**
     * 创建工资表（仅财务角色）
     */
    @PostMapping
    @RequirePermission("payroll:create")
    public Result<PayrollSheetDTO> createPayrollSheet(@RequestBody CreatePayrollSheetCommand command) {
        // 权限检查在Service层进行
        PayrollSheetDTO dto = payrollAppService.createPayrollSheet(command);
        return Result.success(dto);
    }

    /**
     * 分页查询工资表
     */
    @GetMapping
    @RequirePermission("payroll:list")
    public Result<PageResult<PayrollSheetDTO>> listPayrollSheets(PayrollSheetQueryDTO query) {
        PageResult<PayrollSheetDTO> result = payrollAppService.listPayrollSheets(query);
        return Result.success(result);
    }

    /**
     * 查询工资表详情
     */
    @GetMapping("/{id}")
    @RequirePermission("payroll:view")
    public Result<PayrollSheetDTO> getPayrollSheetById(@PathVariable Long id) {
        PayrollSheetDTO dto = payrollAppService.getPayrollSheetById(id);
        return Result.success(dto);
    }

    /**
     * 查询工资表的所有员工工资明细列表（用于列表展示）
     */
    @GetMapping("/{id}/items")
    @RequirePermission("payroll:view")
    public Result<List<PayrollItemDTO>> getPayrollItemsBySheetId(@PathVariable Long id) {
        List<PayrollItemDTO> items = payrollAppService.getPayrollItemsBySheetId(id);
        return Result.success(items);
    }

    /**
     * 根据年月查询员工工资明细列表
     */
    @GetMapping("/items")
    @RequirePermission("payroll:view")
    public Result<List<PayrollItemDTO>> getPayrollItemsByYearMonth(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        List<PayrollItemDTO> items = payrollAppService.getPayrollItemsByYearMonth(year, month);
        return Result.success(items);
    }

    /**
     * 为工资表添加员工工资明细
     */
    @PostMapping("/{id}/add-item")
    @RequirePermission("payroll:edit")
    public Result<PayrollItemDTO> addPayrollItem(
            @PathVariable Long id,
            @RequestBody AddPayrollItemCommand command) {
        PayrollItemDTO dto = payrollAppService.addPayrollItemForEmployee(id, command);
        return Result.success(dto);
    }

    /**
     * 更新工资明细
     */
    @PutMapping("/item/{itemId}")
    @RequirePermission("payroll:edit")
    public Result<PayrollItemDTO> updatePayrollItem(
            @PathVariable Long itemId,
            @RequestBody UpdatePayrollItemCommand command) {
        command.setPayrollItemId(itemId);
        PayrollItemDTO dto = payrollAppService.updatePayrollItem(command);
        return Result.success(dto);
    }

    /**
     * 根据年月和员工ID更新或创建工资明细（用于没有工资表时也能编辑）
     */
    @PutMapping("/item/by-employee")
    @RequirePermission("payroll:edit")
    public Result<PayrollItemDTO> updatePayrollItemByEmployee(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Long employeeId,
            @RequestBody UpdatePayrollItemCommand command) {
        PayrollItemDTO dto = payrollAppService.updateOrCreatePayrollItemByEmployee(year, month, employeeId, command);
        return Result.success(dto);
    }

    /**
     * 提交工资表（待确认）
     */
    @PostMapping("/{id}/submit")
    @RequirePermission("payroll:submit")
    public Result<Void> submitPayrollSheet(@PathVariable Long id) {
        payrollAppService.submitPayrollSheet(id);
        return Result.success();
    }

    /**
     * 员工确认工资表
     */
    @PostMapping("/item/confirm")
    @RequirePermission("payroll:confirm")
    public Result<Void> confirmPayrollItem(@RequestBody ConfirmPayrollCommand command) {
        payrollAppService.confirmPayrollItem(command);
        return Result.success();
    }

    /**
     * 财务确认工资表
     * 确认所有员工都已确认后，更新工资表状态为财务已确认
     */
    @PostMapping("/{id}/finance-confirm")
    @RequirePermission("payroll:finance:confirm")
    public Result<Void> financeConfirmPayrollSheet(@PathVariable Long id) {
        payrollAppService.financeConfirmPayrollSheet(id);
        return Result.success();
    }

    /**
     * 提交审批（财务确认所有员工已确认后，提交给主任或合伙人审批）
     */
    @PostMapping("/submit-approval")
    @RequirePermission("payroll:submit")
    public Result<Void> submitApproval(@RequestBody SubmitApprovalCommand command) {
        payrollAppService.submitApproval(command);
        return Result.success();
    }

    /**
     * 审批工资表（主任或合伙人审批）
     */
    @PostMapping("/approve")
    @RequirePermission("payroll:approve")
    public Result<Void> approvePayrollSheet(@RequestBody ApprovePayrollCommand command) {
        payrollAppService.approvePayrollSheet(command);
        return Result.success();
    }

    /**
     * 发放工资
     */
    @PostMapping("/{id}/issue")
    @RequirePermission("payroll:issue")
    public Result<Void> issuePayroll(
            @PathVariable Long id,
            @RequestBody IssuePayrollCommand command) {
        command.setPayrollSheetId(id);
        payrollAppService.issuePayroll(command);
        return Result.success();
    }

    /**
     * 查询我的工资表
     */
    @GetMapping("/my")
    @RequirePermission("payroll:my:view")
    public Result<List<PayrollSheetDTO>> getMyPayrollSheets(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        List<PayrollSheetDTO> result = payrollAppService.getMyPayrollSheets(year, month);
        return Result.success(result);
    }

    /**
     * 查询我的工资明细
     */
    @GetMapping("/my/{id}")
    @RequirePermission("payroll:my:view")
    public Result<PayrollSheetDTO> getMyPayrollSheetById(@PathVariable Long id) {
        PayrollSheetDTO dto = payrollAppService.getPayrollSheetById(id);
        // 权限检查：只能查看自己的工资
        // TODO: 在Service层添加权限检查
        return Result.success(dto);
    }

    /**
     * 导出工资表为Excel（仅已审批通过的工资表可以导出）
     */
    @GetMapping("/{id}/export")
    @RequirePermission("payroll:export")
    public ResponseEntity<InputStreamResource> exportPayrollSheet(@PathVariable Long id) {
        try {
            ByteArrayInputStream excelStream = payrollAppService.exportPayrollSheet(id);
            PayrollSheetDTO sheet = payrollAppService.getPayrollSheetById(id);
            String filename = String.format("%d年%d月工资表.xlsx", sheet.getPayrollYear(), sheet.getPayrollMonth());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + new String(filename.getBytes("UTF-8"), "ISO-8859-1"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(excelStream));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

