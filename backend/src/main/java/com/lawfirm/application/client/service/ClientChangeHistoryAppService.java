package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.CreateClientChangeHistoryCommand;
import com.lawfirm.application.client.dto.ClientChangeHistoryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientChangeHistory;
import com.lawfirm.domain.client.repository.ClientChangeHistoryRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.infrastructure.persistence.mapper.ClientChangeHistoryMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业变更历史应用服务（M2-014）. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientChangeHistoryAppService {

  /** 客户变更历史仓储. */
  private final ClientChangeHistoryRepository changeHistoryRepository;

  /** 客户变更历史Mapper. */
  private final ClientChangeHistoryMapper changeHistoryMapper;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /**
   * 创建变更记录
   *
   * @param command 创建变更记录命令
   * @return 变更记录DTO
   */
  @Transactional
  public ClientChangeHistoryDTO createChangeHistory(
      final CreateClientChangeHistoryCommand command) {
    Client client = clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

    // 验证客户类型必须是企业
    if (!"ENTERPRISE".equals(client.getClientType())) {
      throw new BusinessException("只有企业客户才能记录变更历史");
    }

    ClientChangeHistory history =
        ClientChangeHistory.builder()
            .clientId(command.getClientId())
            .changeType(command.getChangeType())
            .changeDate(command.getChangeDate())
            .beforeValue(command.getBeforeValue())
            .afterValue(command.getAfterValue())
            .changeDescription(command.getChangeDescription())
            .registrationAuthority(command.getRegistrationAuthority())
            .registrationNumber(command.getRegistrationNumber())
            .attachmentUrl(command.getAttachmentUrl())
            .build();

    changeHistoryRepository.save(history);
    log.info(
        "创建企业变更记录: clientId={}, changeType={}", command.getClientId(), command.getChangeType());
    return toDTO(history);
  }

  /**
   * 获取客户的所有变更记录
   *
   * @param clientId 客户ID
   * @return 变更记录列表
   */
  public List<ClientChangeHistoryDTO> getClientChangeHistories(final Long clientId) {
    List<ClientChangeHistory> histories = changeHistoryMapper.selectByClientId(clientId);
    return histories.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取指定类型的变更记录
   *
   * @param clientId 客户ID
   * @param changeType 变更类型
   * @return 变更记录列表
   */
  public List<ClientChangeHistoryDTO> getChangeHistoriesByType(
      final Long clientId, final String changeType) {
    List<ClientChangeHistory> histories =
        changeHistoryMapper.selectByClientIdAndType(clientId, changeType);
    return histories.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 删除变更记录
   *
   * @param id 变更记录ID
   */
  @Transactional
  public void deleteChangeHistory(final Long id) {
    changeHistoryRepository.getByIdOrThrow(id, "变更记录不存在");
    changeHistoryRepository.removeById(id);
    log.info("删除企业变更记录: id={}", id);
  }

  private String getChangeTypeName(final String changeType) {
    if (changeType == null) {
      return null;
    }
    return switch (changeType) {
      case ClientChangeHistory.TYPE_NAME -> "名称变更";
      case ClientChangeHistory.TYPE_REGISTERED_CAPITAL -> "注册资本变更";
      case ClientChangeHistory.TYPE_LEGAL_REPRESENTATIVE -> "法定代表人变更";
      case ClientChangeHistory.TYPE_ADDRESS -> "地址变更";
      case ClientChangeHistory.TYPE_BUSINESS_SCOPE -> "经营范围变更";
      case ClientChangeHistory.TYPE_SHAREHOLDER -> "股东变更";
      case ClientChangeHistory.TYPE_OTHER -> "其他变更";
      default -> changeType;
    };
  }

  private ClientChangeHistoryDTO toDTO(final ClientChangeHistory history) {
    ClientChangeHistoryDTO dto = new ClientChangeHistoryDTO();
    dto.setId(history.getId());
    dto.setClientId(history.getClientId());
    dto.setChangeType(history.getChangeType());
    dto.setChangeTypeName(getChangeTypeName(history.getChangeType()));
    dto.setChangeDate(history.getChangeDate());
    dto.setBeforeValue(history.getBeforeValue());
    dto.setAfterValue(history.getAfterValue());
    dto.setChangeDescription(history.getChangeDescription());
    dto.setRegistrationAuthority(history.getRegistrationAuthority());
    dto.setRegistrationNumber(history.getRegistrationNumber());
    dto.setAttachmentUrl(history.getAttachmentUrl());
    dto.setCreatedAt(history.getCreatedAt());
    dto.setUpdatedAt(history.getUpdatedAt());

    // 获取客户名称
    Client client = clientRepository.getById(history.getClientId());
    if (client != null) {
      dto.setClientName(client.getName());
    }

    return dto;
  }
}
