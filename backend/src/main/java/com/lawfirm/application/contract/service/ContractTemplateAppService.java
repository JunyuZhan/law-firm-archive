package com.lawfirm.application.contract.service;

import com.lawfirm.application.contract.command.CreateContractTemplateCommand;
import com.lawfirm.application.contract.dto.ContractTemplateDTO;
import com.lawfirm.domain.contract.entity.ContractTemplate;
import com.lawfirm.domain.contract.repository.ContractTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 合同模板应用服务
 */
@Service
@RequiredArgsConstructor
public class ContractTemplateAppService {

    private final ContractTemplateRepository contractTemplateRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取所有启用的模板
     */
    public List<ContractTemplateDTO> getActiveTemplates() {
        return contractTemplateRepository.findActiveTemplates().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有模板
     */
    public List<ContractTemplateDTO> getAllTemplates() {
        return contractTemplateRepository.findAllTemplates().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按合同类型获取模板
     */
    public List<ContractTemplateDTO> getTemplatesByType(String contractType) {
        return contractTemplateRepository.findByContractType(contractType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取模板详情
     */
    public ContractTemplateDTO getTemplate(Long id) {
        ContractTemplate template = contractTemplateRepository.getByIdOrThrow(id, "模板不存在");
        return toDTO(template);
    }

    /**
     * 创建模板
     */
    @Transactional
    public ContractTemplateDTO createTemplate(CreateContractTemplateCommand command) {
        ContractTemplate template = ContractTemplate.builder()
                .templateNo(contractTemplateRepository.generateTemplateNo())
                .name(command.getName())
                .contractType(command.getContractType())
                .feeType(command.getFeeType())
                .content(command.getContent())
                .clauses(command.getClauses())
                .description(command.getDescription())
                .status("ACTIVE")
                .sortOrder(0)
                .build();

        contractTemplateRepository.save(template);
        return toDTO(template);
    }

    /**
     * 更新模板
     */
    @Transactional
    public ContractTemplateDTO updateTemplate(Long id, CreateContractTemplateCommand command) {
        ContractTemplate template = contractTemplateRepository.getByIdOrThrow(id, "模板不存在");
        
        template.setName(command.getName());
        template.setContractType(command.getContractType());
        template.setFeeType(command.getFeeType());
        template.setContent(command.getContent());
        template.setClauses(command.getClauses());
        template.setDescription(command.getDescription());

        contractTemplateRepository.updateById(template);
        return toDTO(template);
    }

    /**
     * 切换模板状态
     */
    @Transactional
    public void toggleStatus(Long id) {
        ContractTemplate template = contractTemplateRepository.getByIdOrThrow(id, "模板不存在");
        template.setStatus("ACTIVE".equals(template.getStatus()) ? "INACTIVE" : "ACTIVE");
        contractTemplateRepository.updateById(template);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        ContractTemplate template = contractTemplateRepository.getByIdOrThrow(id, "模板不存在");
        template.setDeleted(true);
        contractTemplateRepository.updateById(template);
    }

    private ContractTemplateDTO toDTO(ContractTemplate entity) {
        ContractTemplateDTO dto = new ContractTemplateDTO();
        BeanUtils.copyProperties(entity, dto);
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(FORMATTER));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().format(FORMATTER));
        }
        return dto;
    }
}
