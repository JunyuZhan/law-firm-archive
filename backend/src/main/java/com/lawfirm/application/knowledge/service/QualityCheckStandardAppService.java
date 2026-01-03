package com.lawfirm.application.knowledge.service;

import com.lawfirm.application.knowledge.command.CreateQualityCheckStandardCommand;
import com.lawfirm.application.knowledge.dto.QualityCheckStandardDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.knowledge.entity.QualityCheckStandard;
import com.lawfirm.domain.knowledge.repository.QualityCheckStandardRepository;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckStandardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 质量检查标准应用服务（M10-030）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityCheckStandardAppService {

    private final QualityCheckStandardRepository standardRepository;
    private final QualityCheckStandardMapper standardMapper;

    /**
     * 获取所有启用的检查标准
     */
    public List<QualityCheckStandardDTO> getEnabledStandards() {
        List<QualityCheckStandard> standards = standardMapper.selectEnabledStandards();
        return standards.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按分类查询检查标准
     */
    public List<QualityCheckStandardDTO> getStandardsByCategory(String category) {
        List<QualityCheckStandard> standards = standardMapper.selectByCategory(category);
        return standards.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建检查标准
     */
    @Transactional
    public QualityCheckStandardDTO createStandard(CreateQualityCheckStandardCommand command) {
        if (!StringUtils.hasText(command.getStandardName())) {
            throw new BusinessException("标准名称不能为空");
        }

        QualityCheckStandard standard = QualityCheckStandard.builder()
                .standardNo(generateStandardNo())
                .standardName(command.getStandardName())
                .category(command.getCategory())
                .description(command.getDescription())
                .checkItems(command.getCheckItems())
                .applicableMatterTypes(command.getApplicableMatterTypes())
                .weight(command.getWeight() != null ? command.getWeight() : java.math.BigDecimal.ONE)
                .enabled(command.getEnabled() != null ? command.getEnabled() : true)
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .build();

        standardRepository.save(standard);
        log.info("创建质量检查标准: {}", standard.getStandardName());
        return toDTO(standard);
    }

    /**
     * 更新检查标准
     */
    @Transactional
    public QualityCheckStandardDTO updateStandard(Long id, CreateQualityCheckStandardCommand command) {
        QualityCheckStandard standard = standardRepository.getByIdOrThrow(id, "检查标准不存在");

        if (StringUtils.hasText(command.getStandardName())) {
            standard.setStandardName(command.getStandardName());
        }
        if (command.getCategory() != null) {
            standard.setCategory(command.getCategory());
        }
        if (command.getDescription() != null) {
            standard.setDescription(command.getDescription());
        }
        if (command.getCheckItems() != null) {
            standard.setCheckItems(command.getCheckItems());
        }
        if (command.getApplicableMatterTypes() != null) {
            standard.setApplicableMatterTypes(command.getApplicableMatterTypes());
        }
        if (command.getWeight() != null) {
            standard.setWeight(command.getWeight());
        }
        if (command.getEnabled() != null) {
            standard.setEnabled(command.getEnabled());
        }
        if (command.getSortOrder() != null) {
            standard.setSortOrder(command.getSortOrder());
        }

        standardRepository.updateById(standard);
        log.info("更新质量检查标准: {}", standard.getStandardName());
        return toDTO(standard);
    }

    /**
     * 删除检查标准
     */
    @Transactional
    public void deleteStandard(Long id) {
        standardRepository.getByIdOrThrow(id, "检查标准不存在");
        standardRepository.removeById(id);
        log.info("删除质量检查标准: id={}", id);
    }

    /**
     * 获取检查标准详情
     */
    public QualityCheckStandardDTO getStandardById(Long id) {
        QualityCheckStandard standard = standardRepository.getByIdOrThrow(id, "检查标准不存在");
        return toDTO(standard);
    }

    private String generateStandardNo() {
        return "QC-STD-" + System.currentTimeMillis();
    }

    private String getCategoryName(String category) {
        if (category == null) return null;
        return switch (category) {
            case QualityCheckStandard.CATEGORY_CONTRACT -> "合同";
            case QualityCheckStandard.CATEGORY_DOCUMENT -> "文书";
            case QualityCheckStandard.CATEGORY_PROCEDURE -> "程序";
            case QualityCheckStandard.CATEGORY_OTHER -> "其他";
            default -> category;
        };
    }

    private QualityCheckStandardDTO toDTO(QualityCheckStandard standard) {
        QualityCheckStandardDTO dto = new QualityCheckStandardDTO();
        dto.setId(standard.getId());
        dto.setStandardNo(standard.getStandardNo());
        dto.setStandardName(standard.getStandardName());
        dto.setCategory(standard.getCategory());
        dto.setCategoryName(getCategoryName(standard.getCategory()));
        dto.setDescription(standard.getDescription());
        dto.setCheckItems(standard.getCheckItems());
        dto.setApplicableMatterTypes(standard.getApplicableMatterTypes());
        dto.setWeight(standard.getWeight());
        dto.setEnabled(standard.getEnabled());
        dto.setSortOrder(standard.getSortOrder());
        dto.setCreatedAt(standard.getCreatedAt());
        dto.setUpdatedAt(standard.getUpdatedAt());
        return dto;
    }
}

