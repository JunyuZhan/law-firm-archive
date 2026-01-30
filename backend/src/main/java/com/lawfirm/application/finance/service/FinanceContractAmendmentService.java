package com.lawfirm.application.finance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.entity.ContractPaymentSchedule;
import com.lawfirm.domain.finance.entity.FinanceContractAmendment;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.finance.event.ContractAmendedEvent;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractPaymentScheduleRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.FinanceContractAmendmentRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 财务合同变更处理服务
 *
 * <p>业务流程： 1. 律师变更合同 → 触发 ContractAmendedEvent 2. 本服务监听事件 → 创建变更待处理记录 3. 财务人员查看待处理变更 → 决定同步/忽略 4.
 * 如果同步 → 更新财务模块的数据副本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceContractAmendmentService {

  /** 合同变更仓储. */
  private final FinanceContractAmendmentRepository amendmentRepository;

  /** 合同仓储. */
  private final ContractRepository contractRepository;

  /** 合同参与人仓储. */
  private final ContractParticipantRepository participantRepository;

  /** 合同收款计划仓储. */
  private final ContractPaymentScheduleRepository scheduleRepository;

  /** 收款仓储. */
  private final PaymentRepository paymentRepository;

  /** JSON对象映射器. */
  private final ObjectMapper objectMapper;

  /** 变更编号随机数范围 */
  private static final int AMENDMENT_NO_RANDOM_RANGE = 10000;

  /**
   * 监听合同变更事件
   *
   * @param event 合同变更事件
   */
  @EventListener
  @Async
  public void onContractAmended(final ContractAmendedEvent event) {
    log.info("收到合同变更事件: contractId={}, type={}", event.getContractId(), event.getAmendmentType());

    try {
      createAmendmentRecord(event);
    } catch (Exception e) {
      log.error("处理合同变更事件失败: contractId={}", event.getContractId(), e);
    }
  }

  /**
   * 创建变更待处理记录
   *
   * @param event 合同变更事件
   */
  @Transactional
  public void createAmendmentRecord(final ContractAmendedEvent event) {
    // 检查是否有已确认的收款
    List<Payment> confirmedPayments =
        paymentRepository.findByContractIdAndStatus(event.getContractId(), "CONFIRMED");

    boolean affectsPayments = !confirmedPayments.isEmpty();
    String affectedPaymentIds = null;

    if (affectsPayments) {
      List<Long> paymentIds =
          confirmedPayments.stream().map(Payment::getId).collect(Collectors.toList());
      try {
        affectedPaymentIds = objectMapper.writeValueAsString(paymentIds);
      } catch (Exception e) {
        log.warn("序列化受影响收款ID失败", e);
      }
    }

    String amendmentNo = generateAmendmentNo();

    FinanceContractAmendment amendment =
        FinanceContractAmendment.builder()
            .amendmentNo(amendmentNo)
            .contractId(event.getContractId())
            .amendmentType(event.getAmendmentType())
            .beforeSnapshot(event.getBeforeSnapshot())
            .afterSnapshot(event.getAfterSnapshot())
            .amendmentReason(event.getAmendmentReason())
            .lawyerAmendedBy(event.getAmendedBy())
            .lawyerAmendedAt(event.getAmendedAt())
            .status("PENDING")
            .affectsPayments(affectsPayments)
            .affectedPaymentIds(affectedPaymentIds)
            .build();

    amendmentRepository.save(amendment);

    log.info(
        "创建合同变更待处理记录: amendmentNo={}, contractId={}, affectsPayments={}",
        amendmentNo,
        event.getContractId(),
        affectsPayments);
  }

  /**
   * 查询待处理的变更记录
   *
   * @return 待处理的变更记录列表
   */
  public List<FinanceContractAmendment> getPendingAmendments() {
    return amendmentRepository.findPendingAmendments();
  }

  /**
   * 查询合同的变更记录
   *
   * @param contractId 合同ID
   * @return 变更记录列表
   */
  public List<FinanceContractAmendment> getAmendmentsByContractId(final Long contractId) {
    return amendmentRepository.findByContractId(contractId);
  }

  /**
   * 同步变更到财务数据
   *
   * @param amendmentId 变更记录ID
   * @param remark 处理备注
   */
  @Transactional
  public void syncAmendment(final Long amendmentId, final String remark) {
    FinanceContractAmendment amendment = amendmentRepository.findById(amendmentId);
    if (amendment == null) {
      throw new BusinessException("变更记录不存在");
    }

    if (!"PENDING".equals(amendment.getStatus())) {
      throw new BusinessException("该变更记录已处理");
    }

    // 根据变更类型执行同步
    switch (amendment.getAmendmentType()) {
      case "AMOUNT":
        syncAmountChange(amendment);
        break;
      case "PARTICIPANT":
        syncParticipantChange(amendment);
        break;
      case "SCHEDULE":
        syncScheduleChange(amendment);
        break;
      default:
        syncOtherChange(amendment);
    }

    // 更新变更记录状态
    amendment.setStatus("SYNCED");
    amendment.setFinanceHandledBy(SecurityUtils.getCurrentUserId());
    amendment.setFinanceHandledAt(LocalDateTime.now());
    amendment.setFinanceRemark(remark);
    amendmentRepository.updateById(amendment);

    log.info("合同变更已同步: amendmentId={}, contractId={}", amendmentId, amendment.getContractId());
  }

  /**
   * 忽略变更（不同步）
   *
   * @param amendmentId 变更记录ID
   * @param remark 忽略原因
   */
  @Transactional
  public void ignoreAmendment(final Long amendmentId, final String remark) {
    FinanceContractAmendment amendment = amendmentRepository.findById(amendmentId);
    if (amendment == null) {
      throw new BusinessException("变更记录不存在");
    }

    if (!"PENDING".equals(amendment.getStatus())) {
      throw new BusinessException("该变更记录已处理");
    }

    amendment.setStatus("IGNORED");
    amendment.setFinanceHandledBy(SecurityUtils.getCurrentUserId());
    amendment.setFinanceHandledAt(LocalDateTime.now());
    amendment.setFinanceRemark(remark);
    amendmentRepository.updateById(amendment);

    log.info(
        "合同变更已忽略: amendmentId={}, contractId={}, reason={}",
        amendmentId,
        amendment.getContractId(),
        remark);
  }

  /**
   * 同步金额变更
   *
   * @param amendment 合同变更记录
   */
  private void syncAmountChange(final FinanceContractAmendment amendment) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> afterData =
          objectMapper.readValue(amendment.getAfterSnapshot(), Map.class);

      Contract contract = contractRepository.findById(amendment.getContractId());
      if (contract == null) {
        throw new BusinessException("合同不存在");
      }

      // 更新合同金额
      if (afterData.containsKey("totalAmount")) {
        Object totalAmountObj = afterData.get("totalAmount");
        if (totalAmountObj != null) {
          contract.setTotalAmount(new java.math.BigDecimal(totalAmountObj.toString()));
        }
      }

      contractRepository.updateById(contract);
      log.info("同步合同金额变更: contractId={}", amendment.getContractId());

    } catch (Exception e) {
      log.error("同步金额变更失败", e);
      throw new BusinessException("同步金额变更失败: " + e.getMessage());
    }
  }

  /**
   * 同步参与人变更
   *
   * @param amendment 合同变更记录
   */
  private void syncParticipantChange(final FinanceContractAmendment amendment) {
    try {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> afterParticipants =
          objectMapper.readValue(amendment.getAfterSnapshot(), List.class);

      // 删除原有参与人
      participantRepository.deleteByContractId(amendment.getContractId());

      // 创建新参与人
      for (Map<String, Object> pData : afterParticipants) {
        ContractParticipant participant =
            ContractParticipant.builder()
                .contractId(amendment.getContractId())
                .userId(Long.valueOf(pData.get("userId").toString()))
                .role((String) pData.get("role"))
                .commissionRate(
                    pData.get("commissionRate") != null
                        ? new java.math.BigDecimal(pData.get("commissionRate").toString())
                        : null)
                .remark((String) pData.get("remark"))
                .build();
        participantRepository.save(participant);
      }

      log.info(
          "同步合同参与人变更: contractId={}, participantCount={}",
          amendment.getContractId(),
          afterParticipants.size());

    } catch (Exception e) {
      log.error("同步参与人变更失败", e);
      throw new BusinessException("同步参与人变更失败: " + e.getMessage());
    }
  }

  /**
   * 同步付款计划变更
   *
   * @param amendment 合同变更记录
   */
  private void syncScheduleChange(final FinanceContractAmendment amendment) {
    try {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> afterSchedules =
          objectMapper.readValue(amendment.getAfterSnapshot(), List.class);

      // 获取现有付款计划
      List<ContractPaymentSchedule> existingSchedules =
          scheduleRepository.findByContractId(amendment.getContractId());

      // 检查是否有已收款的计划
      boolean hasReceivedPayment =
          existingSchedules.stream()
              .anyMatch(s -> "PAID".equals(s.getStatus()) || "PARTIAL".equals(s.getStatus()));

      if (hasReceivedPayment) {
        log.warn("合同存在已收款的付款计划，仅同步未收款部分: contractId={}", amendment.getContractId());
        // 实现部分同步逻辑：保留已收款计划，更新未收款计划
        syncPaymentPlansPartially(amendment, afterSchedules, existingSchedules);
      } else {
        // 全部替换
        scheduleRepository.deleteByContractId(amendment.getContractId());

        for (Map<String, Object> sData : afterSchedules) {
          ContractPaymentSchedule schedule =
              ContractPaymentSchedule.builder()
                  .contractId(amendment.getContractId())
                  .phaseName((String) sData.get("phaseName"))
                  .amount(new java.math.BigDecimal(sData.get("amount").toString()))
                  .status("PENDING")
                  .build();
          scheduleRepository.save(schedule);
        }
      }

      log.info("同步合同付款计划变更: contractId={}", amendment.getContractId());

    } catch (Exception e) {
      log.error("同步付款计划变更失败", e);
      throw new BusinessException("同步付款计划变更失败: " + e.getMessage());
    }
  }

  /**
   * 部分同步付款计划（保留已收款计划，更新未收款计划）
   *
   * @param amendment 合同变更记录
   * @param afterSchedules 变更后的付款计划
   * @param existingSchedules 现有的付款计划
   */
  private void syncPaymentPlansPartially(
      final FinanceContractAmendment amendment,
      final List<Map<String, Object>> afterSchedules,
      final List<ContractPaymentSchedule> existingSchedules) {
    // 找出已收款的计划
    List<ContractPaymentSchedule> paidSchedules =
        existingSchedules.stream()
            .filter(s -> "PAID".equals(s.getStatus()) || "PARTIAL".equals(s.getStatus()))
            .collect(java.util.stream.Collectors.toList());

    // 找出未收款的计划
    List<ContractPaymentSchedule> unpaidSchedules =
        existingSchedules.stream()
            .filter(s -> "PENDING".equals(s.getStatus()) || "OVERDUE".equals(s.getStatus()))
            .collect(java.util.stream.Collectors.toList());

    // 删除所有未收款的计划
    unpaidSchedules.forEach(s -> scheduleRepository.removeById(s.getId()));

    // 创建新的付款计划
    for (Map<String, Object> sData : afterSchedules) {
      ContractPaymentSchedule schedule =
          ContractPaymentSchedule.builder()
              .contractId(amendment.getContractId())
              .phaseName((String) sData.get("phaseName"))
              .amount(new java.math.BigDecimal(sData.get("amount").toString()))
              .status("PENDING")
              .build();
      scheduleRepository.save(schedule);
    }

    log.info(
        "部分同步付款计划完成: contractId={}, 保留已收款={}, 新增未收款={}",
        amendment.getContractId(),
        paidSchedules.size(),
        afterSchedules.size());
  }

  /**
   * 同步其他变更
   *
   * @param amendment 合同变更记录
   */
  private void syncOtherChange(final FinanceContractAmendment amendment) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> afterData =
          objectMapper.readValue(amendment.getAfterSnapshot(), Map.class);

      Contract contract = contractRepository.findById(amendment.getContractId());
      if (contract == null) {
        throw new BusinessException("合同不存在");
      }

      // 更新可同步的字段
      if (afterData.containsKey("name")) {
        contract.setName((String) afterData.get("name"));
      }
      if (afterData.containsKey("opposingParty")) {
        contract.setOpposingParty((String) afterData.get("opposingParty"));
      }
      if (afterData.containsKey("caseType")) {
        contract.setCaseType((String) afterData.get("caseType"));
      }
      if (afterData.containsKey("causeOfAction")) {
        contract.setCauseOfAction((String) afterData.get("causeOfAction"));
      }

      contractRepository.updateById(contract);
      log.info("同步合同其他变更: contractId={}", amendment.getContractId());

    } catch (Exception e) {
      log.error("同步其他变更失败", e);
      throw new BusinessException("同步其他变更失败: " + e.getMessage());
    }
  }

  private String generateAmendmentNo() {
    return "AMD"
        + System.currentTimeMillis()
        + String.format("%04d", (int) (Math.random() * AMENDMENT_NO_RANDOM_RANGE));
  }
}
