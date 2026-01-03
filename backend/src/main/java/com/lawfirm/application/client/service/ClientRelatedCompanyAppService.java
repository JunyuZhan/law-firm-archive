package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.CreateRelatedCompanyCommand;
import com.lawfirm.application.client.command.UpdateRelatedCompanyCommand;
import com.lawfirm.application.client.dto.ClientRelatedCompanyDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientRelatedCompany;
import com.lawfirm.domain.client.repository.ClientRelatedCompanyRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户关联企业应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientRelatedCompanyAppService {

    private final ClientRelatedCompanyRepository relatedCompanyRepository;
    private final ClientRepository clientRepository;

    /**
     * 获取客户的关联企业列表
     */
    public List<ClientRelatedCompanyDTO> getRelatedCompaniesByClientId(Long clientId) {
        clientRepository.getByIdOrThrow(clientId, "客户不存在");
        List<ClientRelatedCompany> companies = relatedCompanyRepository.findByClientId(clientId);
        return companies.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建关联企业
     */
    @Transactional
    public ClientRelatedCompanyDTO createRelatedCompany(CreateRelatedCompanyCommand command) {
        Client client = clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

        // 验证：企业客户才能添加关联企业
        if (!"ENTERPRISE".equals(client.getClientType())) {
            throw new BusinessException("只有企业客户才能添加关联企业");
        }

        ClientRelatedCompany company = ClientRelatedCompany.builder()
                .clientId(command.getClientId())
                .relatedCompanyName(command.getRelatedCompanyName())
                .relatedCompanyType(command.getRelatedCompanyType())
                .creditCode(command.getCreditCode())
                .registeredAddress(command.getRegisteredAddress())
                .legalRepresentative(command.getLegalRepresentative())
                .relationshipDescription(command.getRelationshipDescription())
                .remark(command.getRemark())
                .build();

        relatedCompanyRepository.save(company);
        log.info("关联企业创建成功: clientId={}, companyName={}", command.getClientId(), command.getRelatedCompanyName());
        return toDTO(company);
    }

    /**
     * 更新关联企业
     */
    @Transactional
    public ClientRelatedCompanyDTO updateRelatedCompany(UpdateRelatedCompanyCommand command) {
        ClientRelatedCompany company = relatedCompanyRepository.getByIdOrThrow(command.getId(), "关联企业不存在");

        if (command.getRelatedCompanyName() != null) {
            company.setRelatedCompanyName(command.getRelatedCompanyName());
        }
        if (command.getRelatedCompanyType() != null) {
            company.setRelatedCompanyType(command.getRelatedCompanyType());
        }
        if (command.getCreditCode() != null) {
            company.setCreditCode(command.getCreditCode());
        }
        if (command.getRegisteredAddress() != null) {
            company.setRegisteredAddress(command.getRegisteredAddress());
        }
        if (command.getLegalRepresentative() != null) {
            company.setLegalRepresentative(command.getLegalRepresentative());
        }
        if (command.getRelationshipDescription() != null) {
            company.setRelationshipDescription(command.getRelationshipDescription());
        }
        if (command.getRemark() != null) {
            company.setRemark(command.getRemark());
        }

        relatedCompanyRepository.updateById(company);
        log.info("关联企业更新成功: id={}", command.getId());
        return toDTO(company);
    }

    /**
     * 删除关联企业
     */
    @Transactional
    public void deleteRelatedCompany(Long id) {
        ClientRelatedCompany company = relatedCompanyRepository.getByIdOrThrow(id, "关联企业不存在");
        relatedCompanyRepository.getBaseMapper().deleteById(id);
        log.info("关联企业删除成功: id={}, companyName={}", id, company.getRelatedCompanyName());
    }

    /**
     * 转换为DTO
     */
    private ClientRelatedCompanyDTO toDTO(ClientRelatedCompany company) {
        ClientRelatedCompanyDTO dto = new ClientRelatedCompanyDTO();
        dto.setId(company.getId());
        dto.setClientId(company.getClientId());
        dto.setRelatedCompanyName(company.getRelatedCompanyName());
        dto.setRelatedCompanyType(company.getRelatedCompanyType());
        dto.setRelatedCompanyTypeName(getRelatedCompanyTypeName(company.getRelatedCompanyType()));
        dto.setCreditCode(company.getCreditCode());
        dto.setRegisteredAddress(company.getRegisteredAddress());
        dto.setLegalRepresentative(company.getLegalRepresentative());
        dto.setRelationshipDescription(company.getRelationshipDescription());
        dto.setRemark(company.getRemark());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setUpdatedAt(company.getUpdatedAt());
        return dto;
    }

    /**
     * 获取关联类型名称
     */
    private String getRelatedCompanyTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "PARENT" -> "母公司";
            case "SUBSIDIARY" -> "子公司";
            case "AFFILIATE" -> "关联公司";
            default -> type;
        };
    }
}

