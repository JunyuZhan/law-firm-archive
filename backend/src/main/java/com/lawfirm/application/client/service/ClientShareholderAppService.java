package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.CreateShareholderCommand;
import com.lawfirm.application.client.command.UpdateShareholderCommand;
import com.lawfirm.application.client.dto.ClientShareholderDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientShareholder;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ClientShareholderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户股东信息应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientShareholderAppService {

    private final ClientShareholderRepository shareholderRepository;
    private final ClientRepository clientRepository;

    /**
     * 获取客户的股东列表
     */
    public List<ClientShareholderDTO> getShareholdersByClientId(Long clientId) {
        clientRepository.getByIdOrThrow(clientId, "客户不存在");
        List<ClientShareholder> shareholders = shareholderRepository.findByClientId(clientId);
        return shareholders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建股东信息
     */
    @Transactional
    public ClientShareholderDTO createShareholder(CreateShareholderCommand command) {
        Client client = clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

        // 验证：企业客户才能添加股东
        if (!"ENTERPRISE".equals(client.getClientType())) {
            throw new BusinessException("只有企业客户才能添加股东信息");
        }

        ClientShareholder shareholder = ClientShareholder.builder()
                .clientId(command.getClientId())
                .shareholderName(command.getShareholderName())
                .shareholderType(command.getShareholderType())
                .idCard(command.getIdCard())
                .creditCode(command.getCreditCode())
                .shareholdingRatio(command.getShareholdingRatio())
                .investmentAmount(command.getInvestmentAmount())
                .investmentDate(command.getInvestmentDate())
                .position(command.getPosition())
                .remark(command.getRemark())
                .build();

        shareholderRepository.save(shareholder);
        log.info("股东信息创建成功: clientId={}, shareholderName={}", command.getClientId(), command.getShareholderName());
        return toDTO(shareholder);
    }

    /**
     * 更新股东信息
     */
    @Transactional
    public ClientShareholderDTO updateShareholder(UpdateShareholderCommand command) {
        ClientShareholder shareholder = shareholderRepository.getByIdOrThrow(command.getId(), "股东信息不存在");

        if (command.getShareholderName() != null) {
            shareholder.setShareholderName(command.getShareholderName());
        }
        if (command.getShareholderType() != null) {
            shareholder.setShareholderType(command.getShareholderType());
        }
        if (command.getIdCard() != null) {
            shareholder.setIdCard(command.getIdCard());
        }
        if (command.getCreditCode() != null) {
            shareholder.setCreditCode(command.getCreditCode());
        }
        if (command.getShareholdingRatio() != null) {
            shareholder.setShareholdingRatio(command.getShareholdingRatio());
        }
        if (command.getInvestmentAmount() != null) {
            shareholder.setInvestmentAmount(command.getInvestmentAmount());
        }
        if (command.getInvestmentDate() != null) {
            shareholder.setInvestmentDate(command.getInvestmentDate());
        }
        if (command.getPosition() != null) {
            shareholder.setPosition(command.getPosition());
        }
        if (command.getRemark() != null) {
            shareholder.setRemark(command.getRemark());
        }

        shareholderRepository.updateById(shareholder);
        log.info("股东信息更新成功: id={}", command.getId());
        return toDTO(shareholder);
    }

    /**
     * 删除股东信息
     */
    @Transactional
    public void deleteShareholder(Long id) {
        ClientShareholder shareholder = shareholderRepository.getByIdOrThrow(id, "股东信息不存在");
        shareholderRepository.getBaseMapper().deleteById(id);
        log.info("股东信息删除成功: id={}, shareholderName={}", id, shareholder.getShareholderName());
    }

    /**
     * 转换为DTO
     */
    private ClientShareholderDTO toDTO(ClientShareholder shareholder) {
        ClientShareholderDTO dto = new ClientShareholderDTO();
        dto.setId(shareholder.getId());
        dto.setClientId(shareholder.getClientId());
        dto.setShareholderName(shareholder.getShareholderName());
        dto.setShareholderType(shareholder.getShareholderType());
        dto.setShareholderTypeName(getShareholderTypeName(shareholder.getShareholderType()));
        dto.setIdCard(shareholder.getIdCard());
        dto.setCreditCode(shareholder.getCreditCode());
        dto.setShareholdingRatio(shareholder.getShareholdingRatio());
        dto.setInvestmentAmount(shareholder.getInvestmentAmount());
        dto.setInvestmentDate(shareholder.getInvestmentDate());
        dto.setPosition(shareholder.getPosition());
        dto.setRemark(shareholder.getRemark());
        dto.setCreatedAt(shareholder.getCreatedAt());
        dto.setUpdatedAt(shareholder.getUpdatedAt());
        return dto;
    }

    /**
     * 获取股东类型名称
     */
    private String getShareholderTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "INDIVIDUAL" -> "个人";
            case "ENTERPRISE" -> "企业";
            default -> type;
        };
    }
}

