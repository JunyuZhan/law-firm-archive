package com.lawfirm.application.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.knowledge.command.CreateLawRegulationCommand;
import com.lawfirm.application.knowledge.dto.*;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.*;
import com.lawfirm.domain.knowledge.repository.*;
import com.lawfirm.infrastructure.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 法规库应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawRegulationAppService {

    private final LawCategoryRepository lawCategoryRepository;
    private final LawCategoryMapper lawCategoryMapper;
    private final LawRegulationRepository lawRegulationRepository;
    private final LawRegulationMapper lawRegulationMapper;
    private final KnowledgeCollectionMapper knowledgeCollectionMapper;

    /**
     * 获取法规分类树
     */
    public List<LawCategoryDTO> getCategoryTree() {
        List<LawCategory> all = lawCategoryMapper.selectAllCategories();
        return buildCategoryTree(all, 0L);
    }

    /**
     * 分页查询法规
     */
    public PageResult<LawRegulationDTO> listRegulations(LawRegulationQueryDTO query) {
        IPage<LawRegulation> page = lawRegulationMapper.selectRegulationPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getCategoryId(),
                query.getStatus(),
                query.getKeyword()
        );

        Long userId = SecurityUtils.getUserId();
        List<LawRegulationDTO> records = page.getRecords().stream()
                .map(r -> toRegulationDTO(r, userId))
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取法规详情
     */
    public LawRegulationDTO getRegulationById(Long id) {
        LawRegulation regulation = lawRegulationRepository.getByIdOrThrow(id, "法规不存在");
        lawRegulationMapper.incrementViewCount(id);
        return toRegulationDTO(regulation, SecurityUtils.getUserId());
    }

    /**
     * 创建法规
     */
    @Transactional
    public LawRegulationDTO createRegulation(CreateLawRegulationCommand command) {
        if (!StringUtils.hasText(command.getTitle())) {
            throw new BusinessException("法规标题不能为空");
        }

        LawRegulation regulation = LawRegulation.builder()
                .title(command.getTitle())
                .categoryId(command.getCategoryId())
                .docNumber(command.getDocNumber())
                .issuingAuthority(command.getIssuingAuthority())
                .issueDate(command.getIssueDate())
                .effectiveDate(command.getEffectiveDate())
                .expiryDate(command.getExpiryDate())
                .status(command.getStatus() != null ? command.getStatus() : LawRegulation.STATUS_EFFECTIVE)
                .content(command.getContent())
                .summary(command.getSummary())
                .keywords(command.getKeywords())
                .source(command.getSource())
                .attachmentUrl(command.getAttachmentUrl())
                .viewCount(0)
                .collectCount(0)
                .build();

        lawRegulationRepository.save(regulation);
        log.info("法规创建成功: {}", regulation.getTitle());
        return toRegulationDTO(regulation, null);
    }

    /**
     * 更新法规
     */
    @Transactional
    public LawRegulationDTO updateRegulation(Long id, CreateLawRegulationCommand command) {
        LawRegulation regulation = lawRegulationRepository.getByIdOrThrow(id, "法规不存在");

        if (StringUtils.hasText(command.getTitle())) {
            regulation.setTitle(command.getTitle());
        }
        if (command.getCategoryId() != null) {
            regulation.setCategoryId(command.getCategoryId());
        }
        if (command.getDocNumber() != null) {
            regulation.setDocNumber(command.getDocNumber());
        }
        if (command.getIssuingAuthority() != null) {
            regulation.setIssuingAuthority(command.getIssuingAuthority());
        }
        if (command.getIssueDate() != null) {
            regulation.setIssueDate(command.getIssueDate());
        }
        if (command.getEffectiveDate() != null) {
            regulation.setEffectiveDate(command.getEffectiveDate());
        }
        if (command.getExpiryDate() != null) {
            regulation.setExpiryDate(command.getExpiryDate());
        }
        if (command.getStatus() != null) {
            regulation.setStatus(command.getStatus());
        }
        if (command.getContent() != null) {
            regulation.setContent(command.getContent());
        }
        if (command.getSummary() != null) {
            regulation.setSummary(command.getSummary());
        }
        if (command.getKeywords() != null) {
            regulation.setKeywords(command.getKeywords());
        }
        if (command.getSource() != null) {
            regulation.setSource(command.getSource());
        }
        if (command.getAttachmentUrl() != null) {
            regulation.setAttachmentUrl(command.getAttachmentUrl());
        }

        lawRegulationRepository.updateById(regulation);
        log.info("法规更新成功: {}", regulation.getTitle());
        return toRegulationDTO(regulation, null);
    }

    /**
     * 删除法规
     */
    @Transactional
    public void deleteRegulation(Long id) {
        LawRegulation regulation = lawRegulationRepository.getByIdOrThrow(id, "法规不存在");
        lawRegulationMapper.deleteById(id);
        log.info("法规删除成功: {}", regulation.getTitle());
    }

    /**
     * 收藏法规
     */
    @Transactional
    public void collectRegulation(Long regulationId) {
        Long userId = SecurityUtils.getUserId();
        lawRegulationRepository.getByIdOrThrow(regulationId, "法规不存在");

        int count = knowledgeCollectionMapper.countByUserAndTarget(userId, KnowledgeCollection.TYPE_LAW, regulationId);
        if (count > 0) {
            throw new BusinessException("已收藏该法规");
        }

        KnowledgeCollection collection = KnowledgeCollection.builder()
                .userId(userId)
                .targetType(KnowledgeCollection.TYPE_LAW)
                .targetId(regulationId)
                .build();
        knowledgeCollectionMapper.insert(collection);
        lawRegulationMapper.incrementCollectCount(regulationId);
        log.info("法规收藏成功: userId={}, regulationId={}", userId, regulationId);
    }

    /**
     * 取消收藏法规
     */
    @Transactional
    public void uncollectRegulation(Long regulationId) {
        Long userId = SecurityUtils.getUserId();
        int deleted = knowledgeCollectionMapper.deleteByUserAndTarget(userId, KnowledgeCollection.TYPE_LAW, regulationId);
        if (deleted > 0) {
            lawRegulationMapper.decrementCollectCount(regulationId);
            log.info("法规取消收藏: userId={}, regulationId={}", userId, regulationId);
        }
    }

    /**
     * 获取我的收藏法规
     */
    public List<LawRegulationDTO> getMyCollectedRegulations() {
        Long userId = SecurityUtils.getUserId();
        List<KnowledgeCollection> collections = knowledgeCollectionMapper.selectByUserAndType(userId, KnowledgeCollection.TYPE_LAW);
        
        return collections.stream()
                .map(c -> {
                    LawRegulation regulation = lawRegulationRepository.getById(c.getTargetId());
                    return regulation != null ? toRegulationDTO(regulation, userId) : null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * 标注法规失效（M10-005）
     */
    @Transactional
    public LawRegulationDTO markAsRepealed(Long id, String reason) {
        LawRegulation regulation = lawRegulationRepository.getByIdOrThrow(id, "法规不存在");
        regulation.setStatus(LawRegulation.STATUS_REPEALED);
        if (StringUtils.hasText(reason)) {
            regulation.setSummary(regulation.getSummary() + "\n失效原因：" + reason);
        }
        lawRegulationRepository.updateById(regulation);
        log.info("标注法规失效: {}", regulation.getTitle());
        return toRegulationDTO(regulation, null);
    }

    private List<LawCategoryDTO> buildCategoryTree(List<LawCategory> all, Long parentId) {
        return all.stream()
                .filter(c -> parentId.equals(c.getParentId()))
                .map(c -> {
                    LawCategoryDTO dto = toCategoryDTO(c);
                    dto.setChildren(buildCategoryTree(all, c.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case LawRegulation.STATUS_EFFECTIVE -> "有效";
            case LawRegulation.STATUS_AMENDED -> "已修订";
            case LawRegulation.STATUS_REPEALED -> "已废止";
            default -> status;
        };
    }

    private LawCategoryDTO toCategoryDTO(LawCategory category) {
        LawCategoryDTO dto = new LawCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setLevel(category.getLevel());
        dto.setSortOrder(category.getSortOrder());
        dto.setDescription(category.getDescription());
        return dto;
    }

    private LawRegulationDTO toRegulationDTO(LawRegulation regulation, Long userId) {
        LawRegulationDTO dto = new LawRegulationDTO();
        dto.setId(regulation.getId());
        dto.setTitle(regulation.getTitle());
        dto.setCategoryId(regulation.getCategoryId());
        dto.setDocNumber(regulation.getDocNumber());
        dto.setIssuingAuthority(regulation.getIssuingAuthority());
        dto.setIssueDate(regulation.getIssueDate());
        dto.setEffectiveDate(regulation.getEffectiveDate());
        dto.setExpiryDate(regulation.getExpiryDate());
        dto.setStatus(regulation.getStatus());
        dto.setStatusName(getStatusName(regulation.getStatus()));
        dto.setContent(regulation.getContent());
        dto.setSummary(regulation.getSummary());
        dto.setKeywords(regulation.getKeywords());
        dto.setSource(regulation.getSource());
        dto.setAttachmentUrl(regulation.getAttachmentUrl());
        dto.setViewCount(regulation.getViewCount());
        dto.setCollectCount(regulation.getCollectCount());
        dto.setCreatedAt(regulation.getCreatedAt());

        // 获取分类名称
        LawCategory category = lawCategoryRepository.getById(regulation.getCategoryId());
        if (category != null) {
            dto.setCategoryName(category.getName());
        }

        // 检查是否已收藏
        if (userId != null) {
            int count = knowledgeCollectionMapper.countByUserAndTarget(userId, KnowledgeCollection.TYPE_LAW, regulation.getId());
            dto.setCollected(count > 0);
        }

        return dto;
    }
}
