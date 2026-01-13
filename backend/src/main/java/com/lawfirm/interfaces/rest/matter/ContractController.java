package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.command.UpdateContractCommand;
import com.lawfirm.application.finance.command.ContractChangeCommand;
import com.lawfirm.application.finance.command.CreatePaymentScheduleCommand;
import com.lawfirm.application.finance.command.UpdatePaymentScheduleCommand;
import com.lawfirm.application.finance.command.CreateParticipantCommand;
import com.lawfirm.application.finance.command.UpdateParticipantCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractQueryDTO;
import com.lawfirm.application.finance.dto.ContractPaymentScheduleDTO;
import com.lawfirm.application.finance.dto.ContractParticipantDTO;
import com.lawfirm.application.finance.service.ContractAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RepeatSubmit;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 项目管理模块 - 合同管理 Controller
 * 提供合同的完整 CRUD 操作，包括付款计划和参与人管理
 */
@RestController("matterContractController")
@RequestMapping("/matter/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractAppService contractAppService;
    private final com.lawfirm.domain.finance.repository.PaymentRepository paymentRepository;

    // ========== 合同基础操作 ==========

    /**
     * 分页查询合同列表
     */
    @GetMapping("/list")
    @RequirePermission("matter:contract:view")
    public Result<PageResult<ContractDTO>> listContracts(ContractQueryDTO query) {
        PageResult<ContractDTO> result = contractAppService.listContracts(query);
        return Result.success(result);
    }

    /**
     * 获取我的合同（仅自己创建或签约的合同）
     */
    @GetMapping("/my")
    @RequirePermission("matter:contract:view")
    public Result<PageResult<ContractDTO>> getMyContracts(ContractQueryDTO query) {
        PageResult<ContractDTO> result = contractAppService.getMyContracts(query);
        return Result.success(result);
    }

    /**
     * 获取合同详情
     */
    @GetMapping("/{id}")
    @RequirePermission("matter:contract:view")
    public Result<ContractDTO> getContract(@PathVariable Long id) {
        ContractDTO contract = contractAppService.getContractById(id);
        return Result.success(contract);
    }

    /**
     * 创建合同
     */
    @PostMapping
    @RequirePermission("matter:contract:create")
    @OperationLog(module = "合同管理", action = "创建合同")
    @RepeatSubmit(interval = 5000, message = "请勿重复提交合同信息")
    public Result<ContractDTO> createContract(@RequestBody CreateContractCommand command) {
        // 调试日志：打印接收到的提成方案数据
        System.out.println("=== 后端接收到的提成方案数据 ===");
        System.out.println("commissionRuleId: " + command.getCommissionRuleId());
        System.out.println("firmRate: " + command.getFirmRate());
        System.out.println("leadLawyerRate: " + command.getLeadLawyerRate());
        System.out.println("assistLawyerRate: " + command.getAssistLawyerRate());
        System.out.println("supportStaffRate: " + command.getSupportStaffRate());
        System.out.println("================================");
        
        ContractDTO contract = contractAppService.createContract(command);
        return Result.success(contract);
    }

    /**
     * 更新合同
     */
    @PutMapping("/{id}")
    @RequirePermission("matter:contract:edit")
    @OperationLog(module = "合同管理", action = "更新合同")
    public Result<ContractDTO> updateContract(@PathVariable Long id, @RequestBody UpdateContractCommand command) {
        command.setId(id);
        ContractDTO contract = contractAppService.updateContract(command);
        return Result.success(contract);
    }

    /**
     * 删除合同
     */
    @DeleteMapping("/{id}")
    @RequirePermission("matter:contract:delete")
    @OperationLog(module = "合同管理", action = "删除合同")
    public Result<Void> deleteContract(@PathVariable Long id) {
        contractAppService.deleteContract(id);
        return Result.success();
    }

    /**
     * 提交审批
     */
    @PostMapping("/{id}/submit")
    @RequirePermission("matter:contract:submit")
    @OperationLog(module = "合同管理", action = "提交审批")
    public Result<Void> submitForApproval(
            @PathVariable Long id,
            @RequestParam(required = false) Long approverId) {
        contractAppService.submitForApproval(id, approverId);
        return Result.success();
    }

    /**
     * 获取可选审批人列表（主任和合伙人）
     */
    @GetMapping("/approvers")
    @RequirePermission("matter:contract:view")
    public Result<List<Map<String, Object>>> getApprovers() {
        List<Map<String, Object>> approvers = contractAppService.getAvailableApprovers();
        return Result.success(approvers);
    }

    /**
     * 获取已审批的合同列表（用于创建项目时选择）
     */
    @GetMapping("/approved")
    @RequirePermission("matter:contract:view")
    public Result<List<ContractDTO>> getApprovedContracts() {
        List<ContractDTO> contracts = contractAppService.getApprovedContracts();
        return Result.success(contracts);
    }

    // ========== 付款计划管理 ==========

    /**
     * 获取合同的付款计划列表
     */
    @GetMapping("/{id}/payment-schedules")
    @RequirePermission("matter:contract:view")
    public Result<List<ContractPaymentScheduleDTO>> getPaymentSchedules(@PathVariable Long id) {
        List<ContractPaymentScheduleDTO> schedules = contractAppService.getPaymentSchedules(id);
        return Result.success(schedules);
    }

    /**
     * 创建付款计划
     */
    @PostMapping("/{id}/payment-schedules")
    @RequirePermission("matter:contract:edit")
    @OperationLog(module = "合同管理", action = "创建付款计划")
    public Result<ContractPaymentScheduleDTO> createPaymentSchedule(
            @PathVariable Long id,
            @RequestBody CreatePaymentScheduleCommand command) {
        command.setContractId(id);
        ContractPaymentScheduleDTO schedule = contractAppService.createPaymentSchedule(command);
        return Result.success(schedule);
    }

    /**
     * 更新付款计划
     */
    @PutMapping("/{contractId}/payment-schedules/{scheduleId}")
    @RequirePermission("matter:contract:edit")
    @OperationLog(module = "合同管理", action = "更新付款计划")
    public Result<ContractPaymentScheduleDTO> updatePaymentSchedule(
            @PathVariable Long contractId,
            @PathVariable Long scheduleId,
            @RequestBody UpdatePaymentScheduleCommand command) {
        command.setId(scheduleId);
        ContractPaymentScheduleDTO schedule = contractAppService.updatePaymentSchedule(command);
        return Result.success(schedule);
    }

    /**
     * 删除付款计划
     */
    @DeleteMapping("/{contractId}/payment-schedules/{scheduleId}")
    @RequirePermission("matter:contract:edit")
    @OperationLog(module = "合同管理", action = "删除付款计划")
    public Result<Void> deletePaymentSchedule(
            @PathVariable Long contractId,
            @PathVariable Long scheduleId) {
        contractAppService.deletePaymentSchedule(scheduleId);
        return Result.success();
    }

    // ========== 参与人管理 ==========

    /**
     * 获取合同的参与人列表
     */
    @GetMapping("/{id}/participants")
    @RequirePermission("matter:contract:view")
    public Result<List<ContractParticipantDTO>> getParticipants(@PathVariable Long id) {
        List<ContractParticipantDTO> participants = contractAppService.getParticipants(id);
        return Result.success(participants);
    }

    /**
     * 创建参与人
     */
    @PostMapping("/{id}/participants")
    @RequirePermission("matter:contract:edit")
    @OperationLog(module = "合同管理", action = "添加参与人")
    public Result<ContractParticipantDTO> createParticipant(
            @PathVariable Long id,
            @RequestBody CreateParticipantCommand command) {
        command.setContractId(id);
        ContractParticipantDTO participant = contractAppService.createParticipant(command);
        return Result.success(participant);
    }

    /**
     * 更新参与人
     */
    @PutMapping("/{contractId}/participants/{participantId}")
    @RequirePermission("matter:contract:edit")
    @OperationLog(module = "合同管理", action = "更新参与人")
    public Result<ContractParticipantDTO> updateParticipant(
            @PathVariable Long contractId,
            @PathVariable Long participantId,
            @RequestBody UpdateParticipantCommand command) {
        command.setId(participantId);
        ContractParticipantDTO participant = contractAppService.updateParticipant(command);
        return Result.success(participant);
    }

    /**
     * 删除参与人
     */
    @DeleteMapping("/{contractId}/participants/{participantId}")
    @RequirePermission("matter:contract:edit")
    @OperationLog(module = "合同管理", action = "删除参与人")
    public Result<Void> deleteParticipant(
            @PathVariable Long contractId,
            @PathVariable Long participantId) {
        contractAppService.deleteParticipant(participantId);
        return Result.success();
    }

    // ========== 统计接口 ==========

    /**
     * 获取合同统计信息
     */
    @GetMapping("/statistics")
    @RequirePermission(value = {"matter:contract:view", "finance:report:view"}, logical = RequirePermission.Logical.OR)
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        // 使用查询条件获取合同列表进行统计
        ContractQueryDTO query = new ContractQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10000); // 获取所有数据用于统计
        if (startDate != null) {
            query.setSignDateFrom(startDate);
        }
        if (endDate != null) {
            query.setSignDateTo(endDate);
        }
        
        PageResult<ContractDTO> result = contractAppService.listContracts(query);
        List<ContractDTO> contracts = result.getRecords();
        
        // 计算统计数据
        Map<String, Object> statistics = new HashMap<>();
        
        // 总合同数
        statistics.put("totalCount", contracts.size());
        
        // 按状态统计
        long draftCount = contracts.stream().filter(c -> "DRAFT".equals(c.getStatus())).count();
        long pendingCount = contracts.stream().filter(c -> "PENDING".equals(c.getStatus())).count();
        long activeCount = contracts.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count();
        long completedCount = contracts.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count();
        long terminatedCount = contracts.stream().filter(c -> "TERMINATED".equals(c.getStatus())).count();
        
        statistics.put("draftCount", draftCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("activeCount", activeCount);
        statistics.put("completedCount", completedCount);
        statistics.put("terminatedCount", terminatedCount);
        
        // 金额统计
        BigDecimal totalAmount = contracts.stream()
                .filter(c -> c.getTotalAmount() != null)
                .map(ContractDTO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 基于实际收款记录计算已收款金额（更准确）
        List<Long> contractIds = contracts.stream()
                .map(ContractDTO::getId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toList());
        
        BigDecimal receivedAmount = BigDecimal.ZERO;
        if (!contractIds.isEmpty()) {
            // 查询这些合同的已确认收款总额
            receivedAmount = paymentRepository.lambdaQuery()
                    .in(com.lawfirm.domain.finance.entity.Payment::getContractId, contractIds)
                    .eq(com.lawfirm.domain.finance.entity.Payment::getStatus, "CONFIRMED")
                    .eq(com.lawfirm.domain.finance.entity.Payment::getDeleted, false)
                    .list()
                    .stream()
                    .map(com.lawfirm.domain.finance.entity.Payment::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        BigDecimal unpaidAmount = totalAmount.subtract(receivedAmount);
        
        statistics.put("totalAmount", totalAmount);
        statistics.put("paidAmount", receivedAmount); // 保持向后兼容
        statistics.put("receivedAmount", receivedAmount); // 前端期望的字段名
        statistics.put("unpaidAmount", unpaidAmount);
        
        // 按收费方式统计
        long fixedCount = contracts.stream().filter(c -> "FIXED".equals(c.getFeeType())).count();
        long hourlyCount = contracts.stream().filter(c -> "HOURLY".equals(c.getFeeType())).count();
        long contingencyCount = contracts.stream().filter(c -> "CONTINGENCY".equals(c.getFeeType())).count();
        long mixedCount = contracts.stream().filter(c -> "MIXED".equals(c.getFeeType())).count();
        
        statistics.put("fixedFeeCount", fixedCount);
        statistics.put("hourlyFeeCount", hourlyCount);
        statistics.put("contingencyFeeCount", contingencyCount);
        statistics.put("mixedFeeCount", mixedCount);
        
        return Result.success(statistics);
    }

    // ========== 模板相关接口 ==========

    /**
     * 基于模板创建合同
     */
    @PostMapping("/from-template/{templateId}")
    @RequirePermission("matter:contract:create")
    @OperationLog(module = "合同管理", action = "基于模板创建合同")
    public Result<ContractDTO> createFromTemplate(
            @PathVariable Long templateId,
            @RequestBody CreateContractCommand command) {
        // 调试日志：打印接收到的提成方案数据
        System.out.println("=== 后端接收到的提成方案数据（基于模板创建） ===");
        System.out.println("templateId: " + templateId);
        System.out.println("commissionRuleId: " + command.getCommissionRuleId());
        System.out.println("firmRate: " + command.getFirmRate());
        System.out.println("leadLawyerRate: " + command.getLeadLawyerRate());
        System.out.println("assistLawyerRate: " + command.getAssistLawyerRate());
        System.out.println("supportStaffRate: " + command.getSupportStaffRate());
        System.out.println("================================================");
        
        ContractDTO contract = contractAppService.createFromTemplate(templateId, command);
        return Result.success(contract);
    }

    /**
     * 预览模板内容（变量替换后）
     */
    @PostMapping("/template/{templateId}/preview")
    @RequirePermission("matter:contract:view")
    public Result<String> previewTemplateContent(
            @PathVariable Long templateId,
            @RequestBody CreateContractCommand command) {
        String content = contractAppService.previewTemplateContent(templateId, command);
        return Result.success(content);
    }
    
    /**
     * 获取合同打印数据
     * 用于打印合同和收案审批表
     */
    @GetMapping("/{id}/print-data")
    @RequirePermission("matter:contract:view")
    public Result<com.lawfirm.application.finance.dto.ContractPrintDTO> getContractPrintData(@PathVariable Long id) {
        return Result.success(contractAppService.getContractPrintData(id));
    }

    /**
     * 申请合同变更
     * 用于已审批通过的合同申请变更，需要重新审批
     */
    @PostMapping("/change")
    @RequirePermission("matter:contract:edit")
    @OperationLog(module = "合同管理", action = "申请合同变更")
    public Result<Void> applyContractChange(@RequestBody ContractChangeCommand command) {
        contractAppService.applyContractChange(command);
        return Result.success();
    }
}
