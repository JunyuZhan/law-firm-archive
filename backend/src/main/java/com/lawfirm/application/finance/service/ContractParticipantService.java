package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.CreateParticipantCommand;
import com.lawfirm.application.finance.command.UpdateParticipantCommand;
import com.lawfirm.application.finance.dto.ContractParticipantDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合同参与人服务
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractParticipantService {

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 参与人仓储 */
  private final ContractParticipantRepository participantRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 获取合同的参与人列表
   *
   * @param contractId 合同ID
   * @return 参与人列表
   */
  public List<ContractParticipantDTO> getParticipants(final Long contractId) {
    contractRepository.getByIdOrThrow(contractId, "合同不存在");
    List<ContractParticipant> participants = participantRepository.findByContractId(contractId);

    if (participants.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载用户信息，避免N+1查询
    Set<Long> userIds =
        participants.stream()
            .map(ContractParticipant::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, User> userMap =
        userIds.isEmpty()
            ? Collections.emptyMap()
            : userRepository.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

    return participants.stream()
        .map(p -> toParticipantDTO(p, userMap))
        .collect(Collectors.toList());
  }

  /**
   * 创建参与人
   *
   * <p>使用悲观锁（SELECT FOR UPDATE）确保提成比例验证的并发安全
   *
   * @param command 创建参与人命令对象
   * @return 参与人DTO
   */
  @Transactional
  public ContractParticipantDTO createParticipant(final CreateParticipantCommand command) {
    // 使用悲观锁锁定合同记录，防止并发添加参与人时提成比例超过100%
    Contract contract = contractRepository.selectByIdForUpdate(command.getContractId());
    if (contract == null) {
      throw new BusinessException("合同不存在");
    }

    // 检查用户是否已是参与人
    if (participantRepository.existsByContractIdAndUserId(
        command.getContractId(), command.getUserId())) {
      throw new BusinessException("该用户已是合同参与人");
    }

    // 验证提成比例（在锁保护下进行，确保并发安全）
    if (command.getCommissionRate() != null) {
      BigDecimal currentTotal =
          participantRepository.sumCommissionRateByContractId(command.getContractId());
      BigDecimal newTotal = currentTotal.add(command.getCommissionRate());
      if (newTotal.compareTo(new BigDecimal("100")) > 0) {
        throw new BusinessException("提成比例总和不能超过100%，当前已分配: " + currentTotal + "%");
      }
    }

    ContractParticipant participant =
        ContractParticipant.builder()
            .contractId(command.getContractId())
            .userId(command.getUserId())
            .role(command.getRole())
            .commissionRate(command.getCommissionRate())
            .remark(command.getRemark())
            .build();

    participantRepository.save(participant);
    log.info("合同参与人创建成功: {} - userId: {}", contract.getContractNo(), command.getUserId());
    return toParticipantDTO(participant);
  }

  /**
   * 更新参与人
   *
   * @param command 更新参与人命令
   * @return 参与人DTO
   */
  @Transactional
  public ContractParticipantDTO updateParticipant(final UpdateParticipantCommand command) {
    ContractParticipant participant =
        participantRepository.getByIdOrThrow(command.getId(), "参与人不存在");

    // 验证提成比例
    if (command.getCommissionRate() != null) {
      BigDecimal currentTotal =
          participantRepository.sumCommissionRateByContractId(participant.getContractId());
      BigDecimal oldRate =
          participant.getCommissionRate() != null
              ? participant.getCommissionRate()
              : BigDecimal.ZERO;
      BigDecimal newTotal = currentTotal.subtract(oldRate).add(command.getCommissionRate());
      if (newTotal.compareTo(new BigDecimal("100")) > 0) {
        throw new BusinessException("提成比例总和不能超过100%");
      }
    }

    if (command.getRole() != null) {
      participant.setRole(command.getRole());
    }
    if (command.getCommissionRate() != null) {
      participant.setCommissionRate(command.getCommissionRate());
    }
    if (command.getRemark() != null) {
      participant.setRemark(command.getRemark());
    }

    participantRepository.updateById(participant);
    log.info("合同参与人更新成功: {}", participant.getId());
    return toParticipantDTO(participant);
  }

  /**
   * 删除参与人
   *
   * @param id 参与人ID
   */
  @Transactional
  public void deleteParticipant(final Long id) {
    ContractParticipant participant = participantRepository.getByIdOrThrow(id, "参与人不存在");

    // 检查是否是唯一的承办律师
    if ("LEAD".equals(participant.getRole())) {
      List<ContractParticipant> leads =
          participantRepository.findByContractId(participant.getContractId()).stream()
              .filter(p -> "LEAD".equals(p.getRole()) && !p.getId().equals(id))
              .collect(Collectors.toList());
      if (leads.isEmpty()) {
        throw new BusinessException("合同必须至少有一个承办律师");
      }
    }

    participantRepository.removeById(id);
    log.info("合同参与人删除成功: {}", id);
  }

  /**
   * 验证合同是否有承办律师
   *
   * @param contractId 合同ID
   * @return 是否有承办律师
   */
  public boolean hasLeadParticipant(final Long contractId) {
    ContractParticipant lead = participantRepository.findLeadByContractId(contractId);
    return lead != null;
  }

  /**
   * 获取参与人角色名称
   *
   * @param role 参与人角色代码
   * @return 参与人角色名称
   */
  private String getParticipantRoleName(final String role) {
    if (role == null) {
      return null;
    }
    return switch (role) {
      case "LEAD" -> "承办律师";
      case "CO_COUNSEL" -> "协办律师";
      case "ORIGINATOR" -> "案源人";
      case "PARALEGAL" -> "律师助理";
      default -> role;
    };
  }

  /**
   * 参与人 Entity 转 DTO
   *
   * @param participant 参与人实体
   * @return 参与人DTO
   */
  private ContractParticipantDTO toParticipantDTO(final ContractParticipant participant) {
    ContractParticipantDTO dto = new ContractParticipantDTO();
    dto.setId(participant.getId());
    dto.setContractId(participant.getContractId());
    dto.setUserId(participant.getUserId());
    // 填充用户名称
    if (participant.getUserId() != null) {
      try {
        var user = userRepository.getById(participant.getUserId());
        if (user != null) {
          dto.setUserName(user.getRealName());
        }
      } catch (Exception e) {
        log.warn("获取用户名称失败，userId: {}", participant.getUserId(), e);
      }
    }
    dto.setRole(participant.getRole());
    dto.setRoleName(getParticipantRoleName(participant.getRole()));
    dto.setCommissionRate(participant.getCommissionRate());
    dto.setRemark(participant.getRemark());
    dto.setCreatedAt(participant.getCreatedAt());
    dto.setUpdatedAt(participant.getUpdatedAt());
    return dto;
  }

  /**
   * 参与人 Entity 转 DTO（使用预加载的用户数据，避免N+1查询）
   *
   * @param participant 参与人实体
   * @param userMap 用户映射
   * @return 参与人DTO
   */
  private ContractParticipantDTO toParticipantDTO(
      final ContractParticipant participant, final Map<Long, User> userMap) {
    ContractParticipantDTO dto = new ContractParticipantDTO();
    dto.setId(participant.getId());
    dto.setContractId(participant.getContractId());
    dto.setUserId(participant.getUserId());
    // 从预加载的Map获取用户名称
    if (participant.getUserId() != null && userMap != null) {
      User user = userMap.get(participant.getUserId());
      if (user != null) {
        dto.setUserName(user.getRealName());
      }
    }
    dto.setRole(participant.getRole());
    dto.setRoleName(getParticipantRoleName(participant.getRole()));
    dto.setCommissionRate(participant.getCommissionRate());
    dto.setRemark(participant.getRemark());
    dto.setCreatedAt(participant.getCreatedAt());
    dto.setUpdatedAt(participant.getUpdatedAt());
    return dto;
  }
}
