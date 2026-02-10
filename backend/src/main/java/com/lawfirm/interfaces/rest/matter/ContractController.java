package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.finance.command.ContractChangeCommand;
import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.command.CreateParticipantCommand;
import com.lawfirm.application.finance.command.CreatePaymentScheduleCommand;
import com.lawfirm.application.finance.command.UpdateContractCommand;
import com.lawfirm.application.finance.command.UpdateParticipantCommand;
import com.lawfirm.application.finance.command.UpdatePaymentScheduleCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractParticipantDTO;
import com.lawfirm.application.finance.dto.ContractPaymentScheduleDTO;
import com.lawfirm.application.finance.dto.ContractQueryDTO;
import com.lawfirm.application.finance.service.ContractAppService;
import com.lawfirm.application.finance.service.ContractParticipantService;
import com.lawfirm.application.finance.service.ContractPaymentScheduleService;
import com.lawfirm.application.finance.service.ContractPrintService;
import com.lawfirm.application.finance.service.ContractTemplateService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RepeatSubmit;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 项目管理模块 - 合同管理 Controller 提供合同的完整 CRUD 操作，包括付款计划和参与人管理 */
@RestController("matterContractController")
@RequestMapping("/matter/contract")
@RequiredArgsConstructor
public class ContractController {

  /** 统计查询最大记录数：10000条 */
  private static final int MAX_STATISTICS_RECORDS = 10000;

  /** 合同应用服务 */
  private final ContractAppService contractAppService;

  /** 合同付款计划服务 */
  private final ContractPaymentScheduleService contractPaymentScheduleService;

  /** 合同参与人服务 */
  private final ContractParticipantService contractParticipantService;

  /** 合同打印服务 */
  private final ContractPrintService contractPrintService;

  /** 合同模板服务 */
  private final ContractTemplateService contractTemplateService;

  /** 付款仓储 */
  private final com.lawfirm.domain.finance.repository.PaymentRepository paymentRepository;

  // ========== 合同基础操作 ==========

  /**
   * 分页查询合同列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("matter:contract:view")
  public Result<PageResult<ContractDTO>> listContracts(final ContractQueryDTO query) {
    PageResult<ContractDTO> result = contractAppService.listContracts(query);
    return Result.success(result);
  }

  /**
   * 获取我的合同（仅自己创建或签约的合同）
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/my")
  @RequirePermission("matter:contract:view")
  public Result<PageResult<ContractDTO>> getMyContracts(final ContractQueryDTO query) {
    PageResult<ContractDTO> result = contractAppService.getMyContracts(query);
    return Result.success(result);
  }

  /**
   * 获取合同详情
   *
   * @param id 合同ID
   * @return 合同信息
   */
  @GetMapping("/{id}")
  @RequirePermission("matter:contract:view")
  public Result<ContractDTO> getContract(@PathVariable final Long id) {
    ContractDTO contract = contractAppService.getContractById(id);
    return Result.success(contract);
  }

  /**
   * 创建合同
   *
   * @param command 创建合同命令
   * @return 合同信息
   */
  @PostMapping
  @RequirePermission("matter:contract:create")
  @OperationLog(module = "合同管理", action = "创建合同")
  @RepeatSubmit(interval = 5000, message = "请勿重复提交合同信息")
  public Result<ContractDTO> createContract(@Valid @RequestBody final CreateContractCommand command) {
    ContractDTO contract = contractAppService.createContract(command);
    return Result.success(contract);
  }

  /**
   * 更新合同
   *
   * @param id 合同ID
   * @param command 更新合同命令
   * @return 合同信息
   */
  @PutMapping("/{id}")
  @RequirePermission("matter:contract:edit")
  @OperationLog(module = "合同管理", action = "更新合同")
  public Result<ContractDTO> updateContract(
      @PathVariable final Long id, @Valid @RequestBody final UpdateContractCommand command) {
    command.setId(id);
    ContractDTO contract = contractAppService.updateContract(command);
    return Result.success(contract);
  }

  /**
   * 删除合同
   *
   * @param id 合同ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("matter:contract:delete")
  @OperationLog(module = "合同管理", action = "删除合同")
  public Result<Void> deleteContract(@PathVariable final Long id) {
    contractAppService.deleteContract(id);
    return Result.success();
  }

  /**
   * 提交审批
   *
   * @param id 合同ID
   * @param approverId 审批人ID
   * @return 空结果
   */
  @PostMapping("/{id}/submit")
  @RequirePermission("matter:contract:submit")
  @OperationLog(module = "合同管理", action = "提交审批")
  public Result<Void> submitForApproval(
      @PathVariable final Long id, @RequestParam(required = false) final Long approverId) {
    contractAppService.submitForApproval(id, approverId);
    return Result.success();
  }

  /**
   * 获取可选审批人列表（主任和合伙人）
   *
   * @return 审批人列表
   */
  @GetMapping("/approvers")
  @RequirePermission("matter:contract:view")
  public Result<List<Map<String, Object>>> getApprovers() {
    List<Map<String, Object>> approvers = contractAppService.getAvailableApprovers();
    return Result.success(approvers);
  }

  /**
   * 获取已审批的合同列表（用于创建项目时选择）
   *
   * @return 已审批合同列表
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
   *
   * @param id 合同ID
   * @return 付款计划列表
   */
  @GetMapping("/{id}/payment-schedules")
  @RequirePermission("matter:contract:view")
  public Result<List<ContractPaymentScheduleDTO>> getPaymentSchedules(@PathVariable final Long id) {
    List<ContractPaymentScheduleDTO> schedules =
        contractPaymentScheduleService.getPaymentSchedules(id);
    return Result.success(schedules);
  }

  /**
   * 创建付款计划
   *
   * @param id 合同ID
   * @param command 创建付款计划命令
   * @return 付款计划信息
   */
  @PostMapping("/{id}/payment-schedules")
  @RequirePermission("matter:contract:edit")
  @OperationLog(module = "合同管理", action = "创建付款计划")
  public Result<ContractPaymentScheduleDTO> createPaymentSchedule(
      @PathVariable final Long id, @Valid @RequestBody final CreatePaymentScheduleCommand command) {
    command.setContractId(id);
    ContractPaymentScheduleDTO schedule =
        contractPaymentScheduleService.createPaymentSchedule(command);
    return Result.success(schedule);
  }

  /**
   * 更新付款计划
   *
   * @param contractId 合同ID
   * @param scheduleId 付款计划ID
   * @param command 更新付款计划命令
   * @return 付款计划信息
   */
  @PutMapping("/{contractId}/payment-schedules/{scheduleId}")
  @RequirePermission("matter:contract:edit")
  @OperationLog(module = "合同管理", action = "更新付款计划")
  public Result<ContractPaymentScheduleDTO> updatePaymentSchedule(
      @PathVariable final Long contractId,
      @PathVariable final Long scheduleId,
      @Valid @RequestBody final UpdatePaymentScheduleCommand command) {
    command.setId(scheduleId);
    ContractPaymentScheduleDTO schedule =
        contractPaymentScheduleService.updatePaymentSchedule(command);
    return Result.success(schedule);
  }

  /**
   * 删除付款计划
   *
   * @param contractId 合同ID
   * @param scheduleId 付款计划ID
   * @return 空结果
   */
  @DeleteMapping("/{contractId}/payment-schedules/{scheduleId}")
  @RequirePermission("matter:contract:edit")
  @OperationLog(module = "合同管理", action = "删除付款计划")
  public Result<Void> deletePaymentSchedule(
      @PathVariable final Long contractId, @PathVariable final Long scheduleId) {
    contractPaymentScheduleService.deletePaymentSchedule(scheduleId);
    return Result.success();
  }

  // ========== 参与人管理 ==========

  /**
   * 获取合同的参与人列表
   *
   * @param id 合同ID
   * @return 参与人列表
   */
  @GetMapping("/{id}/participants")
  @RequirePermission("matter:contract:view")
  public Result<List<ContractParticipantDTO>> getParticipants(@PathVariable final Long id) {
    List<ContractParticipantDTO> participants = contractParticipantService.getParticipants(id);
    return Result.success(participants);
  }

  /**
   * 创建参与人
   *
   * @param id 合同ID
   * @param command 创建参与人命令
   * @return 参与人信息
   */
  @PostMapping("/{id}/participants")
  @RequirePermission("matter:contract:edit")
  @OperationLog(module = "合同管理", action = "添加参与人")
  public Result<ContractParticipantDTO> createParticipant(
      @PathVariable final Long id, @Valid @RequestBody final CreateParticipantCommand command) {
    command.setContractId(id);
    ContractParticipantDTO participant = contractParticipantService.createParticipant(command);
    return Result.success(participant);
  }

  /**
   * 更新参与人
   *
   * @param contractId 合同ID
   * @param participantId 参与人ID
   * @param command 更新参与人命令
   * @return 参与人信息
   */
  @PutMapping("/{contractId}/participants/{participantId}")
  @RequirePermission("matter:contract:edit")
  @OperationLog(module = "合同管理", action = "更新参与人")
  public Result<ContractParticipantDTO> updateParticipant(
      @PathVariable final Long contractId,
      @PathVariable final Long participantId,
      @Valid @RequestBody final UpdateParticipantCommand command) {
    command.setId(participantId);
    ContractParticipantDTO participant = contractParticipantService.updateParticipant(command);
    return Result.success(participant);
  }

  /**
   * 删除参与人
   *
   * @param contractId 合同ID
   * @param participantId 参与人ID
   * @return 空结果
   */
  @DeleteMapping("/{contractId}/participants/{participantId}")
  @RequirePermission("matter:contract:edit")
  @OperationLog(module = "合同管理", action = "删除参与人")
  public Result<Void> deleteParticipant(
      @PathVariable final Long contractId, @PathVariable final Long participantId) {
    contractParticipantService.deleteParticipant(participantId);
    return Result.success();
  }

  // ========== 统计接口 ==========

  /**
   * 获取合同统计信息
   *
   * @param departmentId 部门ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 统计信息
   */
  @GetMapping("/statistics")
  @RequirePermission(
      value = {"matter:contract:view", "finance:report:view"},
      logical = RequirePermission.Logical.OR)
  public Result<Map<String, Object>> getStatistics(
      @RequestParam(required = false) final Long departmentId,
      @RequestParam(required = false) final LocalDate startDate,
      @RequestParam(required = false) final LocalDate endDate) {
    // 使用查询条件获取合同列表进行统计
    ContractQueryDTO query = new ContractQueryDTO();
    query.setPageNum(1);
    query.setPageSize(MAX_STATISTICS_RECORDS); // 获取所有数据用于统计
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
    long terminatedCount =
        contracts.stream().filter(c -> "TERMINATED".equals(c.getStatus())).count();

    statistics.put("draftCount", draftCount);
    statistics.put("pendingCount", pendingCount);
    statistics.put("activeCount", activeCount);
    statistics.put("completedCount", completedCount);
    statistics.put("terminatedCount", terminatedCount);

    // 金额统计
    BigDecimal totalAmount =
        contracts.stream()
            .filter(c -> c.getTotalAmount() != null)
            .map(ContractDTO::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 基于实际收款记录计算已收款金额（更准确）
    List<Long> contractIds =
        contracts.stream()
            .map(ContractDTO::getId)
            .filter(id -> id != null)
            .collect(java.util.stream.Collectors.toList());

    BigDecimal receivedAmount = BigDecimal.ZERO;
    if (!contractIds.isEmpty()) {
      // 查询这些合同的已确认收款总额
      receivedAmount =
          paymentRepository
              .lambdaQuery()
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
    long contingencyCount =
        contracts.stream().filter(c -> "CONTINGENCY".equals(c.getFeeType())).count();
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
   *
   * @param templateId 模板ID
   * @param command 创建合同命令
   * @return 合同信息
   */
  @PostMapping("/from-template/{templateId}")
  @RequirePermission("matter:contract:create")
  @OperationLog(module = "合同管理", action = "基于模板创建合同")
  public Result<ContractDTO> createFromTemplate(
      @PathVariable final Long templateId, @RequestBody final CreateContractCommand command) {
    ContractDTO contract = contractTemplateService.createFromTemplate(templateId, command);
    return Result.success(contract);
  }

  /**
   * 预览模板内容（变量替换后）
   *
   * @param templateId 模板ID
   * @param command 创建合同命令
   * @return 预览内容
   */
  @PostMapping("/template/{templateId}/preview")
  @RequirePermission("matter:contract:view")
  public Result<String> previewTemplateContent(
      @PathVariable final Long templateId, @RequestBody final CreateContractCommand command) {
    String content = contractTemplateService.previewTemplateContent(templateId, command);
    return Result.success(content);
  }

  /**
   * 获取合同打印数据 用于打印合同和收案审批表
   *
   * @param id 合同ID
   * @return 打印数据
   */
  @GetMapping("/{id}/print-data")
  @RequirePermission("matter:contract:view")
  public Result<com.lawfirm.application.finance.dto.ContractPrintDTO> getContractPrintData(
      @PathVariable final Long id) {
    return Result.success(contractPrintService.getContractPrintData(id));
  }

  /**
   * 申请合同变更 用于已审批通过的合同申请变更，需要重新审批
   *
   * @param command 合同变更命令
   * @return 空结果
   */
  @PostMapping("/change")
  @RequirePermission("matter:contract:edit")
  @OperationLog(module = "合同管理", action = "申请合同变更")
  public Result<Void> applyContractChange(@Valid @RequestBody final ContractChangeCommand command) {
    contractAppService.applyContractChange(command);
    return Result.success();
  }
}
