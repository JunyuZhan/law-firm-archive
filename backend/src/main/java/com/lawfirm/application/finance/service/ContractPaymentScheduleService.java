package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.CreatePaymentScheduleCommand;
import com.lawfirm.application.finance.command.UpdatePaymentScheduleCommand;
import com.lawfirm.application.finance.dto.ContractPaymentScheduleDTO;
import com.lawfirm.common.constant.PaymentStatus;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractPaymentSchedule;
import com.lawfirm.domain.finance.repository.ContractPaymentScheduleRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合同付款计划服务
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractPaymentScheduleService {

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 付款计划仓储 */
  private final ContractPaymentScheduleRepository paymentScheduleRepository;

  /**
   * 获取付款计划状态名称
   *
   * @param status 付款计划状态代码
   * @return 付款计划状态名称
   */
  private String getPaymentScheduleStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "PENDING" -> "待收";
      case "PARTIAL" -> "部分收款";
      case "PAID" -> "已收清";
      case "CANCELLED" -> "已取消";
      default -> status;
    };
  }

  /**
   * 获取合同的付款计划列表
   *
   * @param contractId 合同ID
   * @return 付款计划列表
   */
  public List<ContractPaymentScheduleDTO> getPaymentSchedules(final Long contractId) {
    contractRepository.getByIdOrThrow(contractId, "合同不存在");
    List<ContractPaymentSchedule> schedules =
        paymentScheduleRepository.findByContractId(contractId);
    return schedules.stream().map(this::toPaymentScheduleDTO).collect(Collectors.toList());
  }

  /**
   * 创建付款计划
   *
   * @param command 创建付款计划命令对象
   * @return 付款计划DTO
   */
  @Transactional
  public ContractPaymentScheduleDTO createPaymentSchedule(
      final CreatePaymentScheduleCommand command) {
    Contract contract = contractRepository.getByIdOrThrow(command.getContractId(), "合同不存在");

    ContractPaymentSchedule schedule =
        ContractPaymentSchedule.builder()
            .contractId(command.getContractId())
            .phaseName(command.getPhaseName())
            .amount(command.getAmount())
            .percentage(command.getPercentage())
            .plannedDate(command.getPlannedDate())
            .status(PaymentStatus.PENDING)
            .remark(command.getRemark())
            .build();

    paymentScheduleRepository.save(schedule);
    log.info("付款计划创建成功: {} - {}", contract.getContractNo(), command.getPhaseName());
    return toPaymentScheduleDTO(schedule);
  }

  /**
   * 更新付款计划
   *
   * @param command 更新付款计划命令对象
   * @return 更新后的付款计划DTO
   */
  @Transactional
  public ContractPaymentScheduleDTO updatePaymentSchedule(
      final UpdatePaymentScheduleCommand command) {
    ContractPaymentSchedule schedule =
        paymentScheduleRepository.getByIdOrThrow(command.getId(), "付款计划不存在");

    if (command.getPhaseName() != null) {
      schedule.setPhaseName(command.getPhaseName());
    }
    if (command.getAmount() != null) {
      schedule.setAmount(command.getAmount());
    }
    if (command.getPercentage() != null) {
      schedule.setPercentage(command.getPercentage());
    }
    if (command.getPlannedDate() != null) {
      schedule.setPlannedDate(command.getPlannedDate());
    }
    if (command.getActualDate() != null) {
      schedule.setActualDate(command.getActualDate());
    }
    if (command.getStatus() != null) {
      schedule.setStatus(command.getStatus());
    }
    if (command.getRemark() != null) {
      schedule.setRemark(command.getRemark());
    }

    paymentScheduleRepository.updateById(schedule);
    log.info("付款计划更新成功: {}", schedule.getId());
    return toPaymentScheduleDTO(schedule);
  }

  /**
   * 删除付款计划
   *
   * @param id 付款计划ID
   */
  @Transactional
  public void deletePaymentSchedule(final Long id) {
    paymentScheduleRepository.getByIdOrThrow(id, "付款计划不存在");
    paymentScheduleRepository.removeById(id);
    log.info("付款计划删除成功: {}", id);
  }

  /**
   * 付款计划 Entity 转 DTO
   *
   * @param schedule 付款计划实体
   * @return 付款计划DTO
   */
  private ContractPaymentScheduleDTO toPaymentScheduleDTO(final ContractPaymentSchedule schedule) {
    ContractPaymentScheduleDTO dto = new ContractPaymentScheduleDTO();
    dto.setId(schedule.getId());
    dto.setContractId(schedule.getContractId());
    dto.setPhaseName(schedule.getPhaseName());
    dto.setAmount(schedule.getAmount());
    dto.setPercentage(schedule.getPercentage());
    dto.setPlannedDate(schedule.getPlannedDate());
    dto.setActualDate(schedule.getActualDate());
    dto.setStatus(schedule.getStatus());
    dto.setStatusName(getPaymentScheduleStatusName(schedule.getStatus()));
    dto.setRemark(schedule.getRemark());
    dto.setCreatedAt(schedule.getCreatedAt());
    dto.setUpdatedAt(schedule.getUpdatedAt());
    return dto;
  }
}
