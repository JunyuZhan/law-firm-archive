package com.lawfirm.application.finance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.entity.ContractPaymentSchedule;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.event.ContractApprovedEvent;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractPaymentScheduleRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.system.entity.DataSyncLog;
import com.lawfirm.domain.system.repository.DataSyncLogRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合同数据同步服务 监听合同审批通过事件，同步数据到财务和行政模块
 *
 * <p>Requirements: 1.3, 1.4, 1.5, 1.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractSyncService {

  /** 最大重试次数 */
  private static final int MAX_RETRY_COUNT = 3;

  /** 源数据表名 */
  private static final String SOURCE_TABLE = "finance_contract";

  /** 合同仓库 */
  private final ContractRepository contractRepository;

  /** 合同参与人仓库 */
  private final ContractParticipantRepository participantRepository;

  /** 合同付款计划仓库 */
  private final ContractPaymentScheduleRepository paymentScheduleRepository;

  /** 客户仓库 */
  private final ClientRepository clientRepository;

  /** 数据同步日志仓库 */
  private final DataSyncLogRepository syncLogRepository;

  /** 收费记录仓库 */
  private final FeeRepository feeRepository;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /**
   * 监听合同审批通过事件
   *
   * @param event 合同审批通过事件
   *     <p>Requirements: 1.1, 1.2
   */
  @EventListener
  @Async
  public void onContractApproved(final ContractApprovedEvent event) {
    log.info(
        "收到合同审批通过事件: contractId={}, approverId={}", event.getContractId(), event.getApproverId());

    // 同步到财务模块
    syncToFinanceModule(event.getContractId());

    // 同步到行政模块
    syncToAdminModule(event.getContractId());
  }

  /**
   * 同步数据到财务模块
   *
   * @param contractId 合同ID
   *     <p>Requirements: 1.1, 1.3
   */
  @Transactional
  public void syncToFinanceModule(final Long contractId) {
    int retryCount = 0;
    Exception lastException = null;

    while (retryCount < MAX_RETRY_COUNT) {
      try {
        doSyncToFinance(contractId);
        logSyncSuccess(contractId, "FINANCE", "CREATE");
        return;
      } catch (Exception e) {
        lastException = e;
        retryCount++;
        log.warn("同步到财务模块失败，重试次数: {}", retryCount, e);

        try {
          Thread.sleep(1000L * retryCount);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    // 重试失败，记录错误日志
    logSyncError(contractId, "FINANCE", "CREATE", lastException, retryCount);
    log.error("合同数据同步到财务模块失败: contractId={}", contractId, lastException);
  }

  /**
   * 同步数据到行政模块
   *
   * @param contractId 合同ID
   *     <p>Requirements: 1.2, 1.4
   */
  @Transactional
  public void syncToAdminModule(final Long contractId) {
    int retryCount = 0;
    Exception lastException = null;

    while (retryCount < MAX_RETRY_COUNT) {
      try {
        doSyncToAdmin(contractId);
        logSyncSuccess(contractId, "ADMIN", "CREATE");
        return;
      } catch (Exception e) {
        lastException = e;
        retryCount++;
        log.warn("同步到行政模块失败，重试次数: {}", retryCount, e);

        try {
          Thread.sleep(1000L * retryCount);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    // 重试失败，记录错误日志
    logSyncError(contractId, "ADMIN", "CREATE", lastException, retryCount);
    log.error("合同数据同步到行政模块失败: contractId={}", contractId, lastException);
  }

  /**
   * 执行财务模块同步 同步字段：合同编号、委托人、对方当事人、案件类型、案由、承办律师、律师费金额、签约日期、付款计划 自动创建收费记录
   *
   * @param contractId 合同ID
   */
  private void doSyncToFinance(final Long contractId) {
    Contract contract = contractRepository.getByIdOrThrow(contractId, "合同不存在");

    // 检查是否已存在收费记录
    List<Fee> existingFees = feeRepository.findByContractId(contractId);
    if (existingFees != null && !existingFees.isEmpty()) {
      log.info("合同已存在收费记录，跳过创建: contractId={}", contractId);
      return;
    }

    // 自动创建收费记录
    createFeeFromContract(contract);

    // 构建同步数据（保留方法调用以保持代码结构）
    buildFinanceSyncData(contract);

    log.info("合同数据同步到财务模块成功: contractId={}, contractNo={}", contractId, contract.getContractNo());
  }

  /**
   * 根据合同自动创建收费记录 如果有付款计划，按付款计划创建多条 Fee；否则创建一条总额 Fee
   *
   * @param contract 合同实体
   *     <p>Requirements: 1.3 - 合同审批后自动同步给财务模块
   */
  private void createFeeFromContract(final Contract contract) {
    // 查询付款计划
    List<ContractPaymentSchedule> schedules =
        paymentScheduleRepository.findByContractId(contract.getId());

    if (schedules != null && !schedules.isEmpty()) {
      // 有付款计划，按计划创建多条 Fee（分期收款）
      int index = 1;
      for (ContractPaymentSchedule schedule : schedules) {
        String feeNo = "FEE" + System.currentTimeMillis() + "-" + index;

        Fee fee =
            Fee.builder()
                .feeNo(feeNo)
                .contractId(contract.getId())
                .matterId(contract.getMatterId())
                .clientId(contract.getClientId())
                .feeType(contract.getFeeType() != null ? contract.getFeeType() : "LAWYER_FEE")
                .feeName(schedule.getPhaseName()) // 使用付款阶段名称
                .amount(schedule.getAmount())
                .paidAmount(BigDecimal.ZERO)
                .currency("CNY")
                .plannedDate(
                    schedule.getPlannedDate() != null ? schedule.getPlannedDate() : LocalDate.now())
                .status("PENDING")
                .build();

        feeRepository.save(fee);
        index++;

        log.info(
            "自动创建收费记录(分期): feeNo={}, phaseName={}, amount={}",
            feeNo,
            schedule.getPhaseName(),
            schedule.getAmount());
      }
    } else {
      // 无付款计划，创建一条总额 Fee
      String feeNo = "FEE" + System.currentTimeMillis();

      Fee fee =
          Fee.builder()
              .feeNo(feeNo)
              .contractId(contract.getId())
              .matterId(contract.getMatterId())
              .clientId(contract.getClientId())
              .feeType(contract.getFeeType() != null ? contract.getFeeType() : "LAWYER_FEE")
              .feeName(contract.getName() + " - 律师费")
              .amount(contract.getTotalAmount())
              .paidAmount(BigDecimal.ZERO)
              .currency("CNY")
              .plannedDate(
                  contract.getSignDate() != null ? contract.getSignDate() : LocalDate.now())
              .status("PENDING")
              .build();

      feeRepository.save(fee);

      log.info(
          "自动创建收费记录(全额): feeNo={}, contractId={}, amount={}",
          feeNo,
          contract.getId(),
          contract.getTotalAmount());
    }
  }

  /**
   * 执行行政模块同步 同步字段：合同编号、委托人、对方当事人、案件类型、案由、承办律师、律师费金额、签约日期 诉讼类型额外同步：案号、管辖法院、代理权限
   *
   * @param contractId 合同ID
   */
  private void doSyncToAdmin(final Long contractId) {
    Contract contract = contractRepository.getByIdOrThrow(contractId, "合同不存在");

    // 构建同步数据（保留方法调用以保持代码结构）
    buildAdminSyncData(contract);

    log.info("合同数据同步到行政模块成功: contractId={}, contractNo={}", contractId, contract.getContractNo());
  }

  /**
   * 构建财务模块同步数据
   *
   * @param contract 合同实体
   * @return 财务模块同步数据
   *     <p>Requirements: 1.3
   */
  private Map<String, Object> buildFinanceSyncData(final Contract contract) {
    Map<String, Object> data = new HashMap<>();

    // 基本信息
    data.put("contractId", contract.getId());
    data.put("contractNo", contract.getContractNo());
    data.put("name", contract.getName());
    data.put("totalAmount", contract.getTotalAmount());
    data.put("paidAmount", contract.getPaidAmount());
    data.put("feeType", contract.getFeeType());
    data.put("signDate", contract.getSignDate());
    data.put("status", contract.getStatus());

    // 客户信息
    if (contract.getClientId() != null) {
      Client client = clientRepository.findById(contract.getClientId());
      if (client != null) {
        data.put("clientName", client.getName());
        data.put("clientId", client.getId());
      }
    }

    // 对方当事人
    data.put("opposingParty", contract.getOpposingParty());

    // 案件类型和案由
    data.put("caseType", contract.getCaseType());
    data.put("causeOfAction", contract.getCauseOfAction());

    // 承办律师信息
    List<ContractParticipant> participants =
        participantRepository.findByContractId(contract.getId());
    ContractParticipant leadLawyer =
        participants.stream().filter(p -> "LEAD".equals(p.getRole())).findFirst().orElse(null);
    if (leadLawyer != null) {
      data.put("leadLawyerId", leadLawyer.getUserId());
      // 需要从用户表获取律师姓名
    }

    return data;
  }

  /**
   * 构建行政模块同步数据
   *
   * @param contract 合同实体
   * @return 行政模块同步数据
   *     <p>Requirements: 1.4
   */
  private Map<String, Object> buildAdminSyncData(final Contract contract) {
    Map<String, Object> data = new HashMap<>();

    // 基本信息
    data.put("contractId", contract.getId());
    data.put("contractNo", contract.getContractNo());
    data.put("name", contract.getName());
    data.put("totalAmount", contract.getTotalAmount());
    data.put("signDate", contract.getSignDate());

    // 客户信息
    if (contract.getClientId() != null) {
      Client client = clientRepository.findById(contract.getClientId());
      if (client != null) {
        data.put("clientName", client.getName());
      }
    }

    // 对方当事人
    data.put("opposingParty", contract.getOpposingParty());

    // 案件类型和案由
    data.put("caseType", contract.getCaseType());
    data.put("causeOfAction", contract.getCauseOfAction());

    // 承办律师信息
    List<ContractParticipant> participants =
        participantRepository.findByContractId(contract.getId());
    ContractParticipant leadLawyer =
        participants.stream().filter(p -> "LEAD".equals(p.getRole())).findFirst().orElse(null);
    if (leadLawyer != null) {
      data.put("leadLawyerId", leadLawyer.getUserId());
      // 需要从用户表获取律师姓名
    }

    // 诉讼类型额外字段
    if (isLitigationType(contract.getCaseType())) {
      data.put("jurisdictionCourt", contract.getJurisdictionCourt());
      data.put("trialStage", contract.getTrialStage());
    }

    return data;
  }

  /**
   * 判断是否为诉讼类型
   *
   * @param caseType 案件类型
   * @return 是否为诉讼类型
   */
  private boolean isLitigationType(final String caseType) {
    if (caseType == null) {
      return false;
    }
    return caseType.contains("诉讼")
        || caseType.equals("CIVIL")
        || caseType.equals("CRIMINAL")
        || caseType.equals("ADMINISTRATIVE");
  }

  /**
   * 记录同步成功日志
   *
   * @param contractId 合同ID
   * @param targetModule 目标模块
   * @param operationType 操作类型
   *     <p>Requirements: 1.5
   */
  private void logSyncSuccess(
      final Long contractId, final String targetModule, final String operationType) {
    try {
      Contract contract = contractRepository.findById(contractId);
      Map<String, Object> syncData =
          "FINANCE".equals(targetModule)
              ? buildFinanceSyncData(contract)
              : buildAdminSyncData(contract);

      DataSyncLog syncLog =
          DataSyncLog.builder()
              .sourceTable(SOURCE_TABLE)
              .sourceId(contractId)
              .targetModule(targetModule)
              .operationType(operationType)
              .syncData(objectMapper.writeValueAsString(syncData))
              .syncStatus("SUCCESS")
              .retryCount(0)
              .syncedAt(LocalDateTime.now())
              .build();

      syncLogRepository.save(syncLog);
    } catch (Exception e) {
      log.error("记录同步成功日志失败", e);
    }
  }

  /**
   * 记录同步失败日志
   *
   * @param contractId 合同ID
   * @param targetModule 目标模块
   * @param operationType 操作类型
   * @param exception 异常信息
   * @param retryCount 重试次数
   *     <p>Requirements: 1.5, 1.6
   */
  private void logSyncError(
      final Long contractId,
      final String targetModule,
      final String operationType,
      final Exception exception,
      final int retryCount) {
    try {
      DataSyncLog syncLog =
          DataSyncLog.builder()
              .sourceTable(SOURCE_TABLE)
              .sourceId(contractId)
              .targetModule(targetModule)
              .operationType(operationType)
              .syncStatus("FAILED")
              .errorMessage(exception != null ? exception.getMessage() : "Unknown error")
              .retryCount(retryCount)
              .syncedAt(LocalDateTime.now())
              .build();

      syncLogRepository.save(syncLog);
    } catch (Exception e) {
      log.error("记录同步失败日志失败", e);
    }
  }
}
