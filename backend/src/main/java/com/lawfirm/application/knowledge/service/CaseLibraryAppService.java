package com.lawfirm.application.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.knowledge.command.CreateCaseLibraryCommand;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * 案例库应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseLibraryAppService {

    private final CaseCategoryRepository caseCategoryRepository;
    private final CaseCategoryMapper caseCategoryMapper;
    private final CaseLibraryRepository caseLibraryRepository;
    private final CaseLibraryMapper caseLibraryMapper;
    private final KnowledgeCollectionMapper knowledgeCollectionMapper;

    /**
     * 获取案例分类树
     */
    public List<CaseCategoryDTO> getCategoryTree() {
        List<CaseCategory> all = caseCategoryMapper.selectAllCategories();
        return buildCategoryTree(all, 0L);
    }

    /**
     * 分页查询案例
     */
    public PageResult<CaseLibraryDTO> listCases(CaseLibraryQueryDTO query) {
        IPage<CaseLibrary> page = caseLibraryMapper.selectCasePage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getCategoryId(),
                query.getSource(),
                query.getCaseType(),
                query.getKeyword()
        );

        Long userId = SecurityUtils.getUserId();
        List<CaseLibraryDTO> records = page.getRecords().stream()
                .map(c -> toCaseDTO(c, userId))
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取案例详情
     */
    public CaseLibraryDTO getCaseById(Long id) {
        CaseLibrary caseLib = caseLibraryRepository.getByIdOrThrow(id, "案例不存在");
        caseLibraryMapper.incrementViewCount(id);
        return toCaseDTO(caseLib, SecurityUtils.getUserId());
    }

    /**
     * 创建案例
     */
    @Transactional
    public CaseLibraryDTO createCase(CreateCaseLibraryCommand command) {
        if (!StringUtils.hasText(command.getTitle())) {
            throw new BusinessException("案例标题不能为空");
        }

        CaseLibrary caseLib = CaseLibrary.builder()
                .title(command.getTitle())
                .categoryId(command.getCategoryId())
                .caseNumber(command.getCaseNumber())
                .courtName(command.getCourtName())
                .judgeDate(command.getJudgeDate())
                .caseType(command.getCaseType())
                .causeOfAction(command.getCauseOfAction())
                .trialProcedure(command.getTrialProcedure())
                .plaintiff(command.getPlaintiff())
                .defendant(command.getDefendant())
                .caseSummary(command.getCaseSummary())
                .courtOpinion(command.getCourtOpinion())
                .judgmentResult(command.getJudgmentResult())
                .caseSignificance(command.getCaseSignificance())
                .keywords(command.getKeywords())
                .source(command.getSource() != null ? command.getSource() : CaseLibrary.SOURCE_EXTERNAL)
                .matterId(command.getMatterId())
                .attachmentUrl(command.getAttachmentUrl())
                .viewCount(0)
                .collectCount(0)
                .build();

        caseLibraryRepository.save(caseLib);
        log.info("案例创建成功: {}", caseLib.getTitle());
        return toCaseDTO(caseLib, null);
    }

    /**
     * 更新案例
     */
    @Transactional
    public CaseLibraryDTO updateCase(Long id, CreateCaseLibraryCommand command) {
        CaseLibrary caseLib = caseLibraryRepository.getByIdOrThrow(id, "案例不存在");

        if (StringUtils.hasText(command.getTitle())) {
            caseLib.setTitle(command.getTitle());
        }
        if (command.getCategoryId() != null) {
            caseLib.setCategoryId(command.getCategoryId());
        }
        if (command.getCaseNumber() != null) {
            caseLib.setCaseNumber(command.getCaseNumber());
        }
        if (command.getCourtName() != null) {
            caseLib.setCourtName(command.getCourtName());
        }
        if (command.getJudgeDate() != null) {
            caseLib.setJudgeDate(command.getJudgeDate());
        }
        if (command.getCaseType() != null) {
            caseLib.setCaseType(command.getCaseType());
        }
        if (command.getCauseOfAction() != null) {
            caseLib.setCauseOfAction(command.getCauseOfAction());
        }
        if (command.getTrialProcedure() != null) {
            caseLib.setTrialProcedure(command.getTrialProcedure());
        }
        if (command.getPlaintiff() != null) {
            caseLib.setPlaintiff(command.getPlaintiff());
        }
        if (command.getDefendant() != null) {
            caseLib.setDefendant(command.getDefendant());
        }
        if (command.getCaseSummary() != null) {
            caseLib.setCaseSummary(command.getCaseSummary());
        }
        if (command.getCourtOpinion() != null) {
            caseLib.setCourtOpinion(command.getCourtOpinion());
        }
        if (command.getJudgmentResult() != null) {
            caseLib.setJudgmentResult(command.getJudgmentResult());
        }
        if (command.getCaseSignificance() != null) {
            caseLib.setCaseSignificance(command.getCaseSignificance());
        }
        if (command.getKeywords() != null) {
            caseLib.setKeywords(command.getKeywords());
        }
        if (command.getAttachmentUrl() != null) {
            caseLib.setAttachmentUrl(command.getAttachmentUrl());
        }

        caseLibraryRepository.updateById(caseLib);
        log.info("案例更新成功: {}", caseLib.getTitle());
        return toCaseDTO(caseLib, null);
    }

    /**
     * 删除案例
     */
    @Transactional
    public void deleteCase(Long id) {
        CaseLibrary caseLib = caseLibraryRepository.getByIdOrThrow(id, "案例不存在");
        caseLibraryMapper.deleteById(id);
        log.info("案例删除成功: {}", caseLib.getTitle());
    }

    /**
     * 收藏案例
     */
    @Transactional
    public void collectCase(Long caseId) {
        Long userId = SecurityUtils.getUserId();
        caseLibraryRepository.getByIdOrThrow(caseId, "案例不存在");

        int count = knowledgeCollectionMapper.countByUserAndTarget(userId, KnowledgeCollection.TYPE_CASE, caseId);
        if (count > 0) {
            throw new BusinessException("已收藏该案例");
        }

        KnowledgeCollection collection = KnowledgeCollection.builder()
                .userId(userId)
                .targetType(KnowledgeCollection.TYPE_CASE)
                .targetId(caseId)
                .build();
        knowledgeCollectionMapper.insert(collection);
        caseLibraryMapper.incrementCollectCount(caseId);
        log.info("案例收藏成功: userId={}, caseId={}", userId, caseId);
    }

    /**
     * 取消收藏案例
     */
    @Transactional
    public void uncollectCase(Long caseId) {
        Long userId = SecurityUtils.getUserId();
        int deleted = knowledgeCollectionMapper.deleteByUserAndTarget(userId, KnowledgeCollection.TYPE_CASE, caseId);
        if (deleted > 0) {
            caseLibraryMapper.decrementCollectCount(caseId);
            log.info("案例取消收藏: userId={}, caseId={}", userId, caseId);
        }
    }

    /**
     * 获取我的收藏案例
     */
    public List<CaseLibraryDTO> getMyCollectedCases() {
        Long userId = SecurityUtils.getUserId();
        List<KnowledgeCollection> collections = knowledgeCollectionMapper.selectByUserAndType(userId, KnowledgeCollection.TYPE_CASE);
        
        return collections.stream()
                .map(c -> {
                    CaseLibrary caseLib = caseLibraryRepository.getById(c.getTargetId());
                    return caseLib != null ? toCaseDTO(caseLib, userId) : null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    private List<CaseCategoryDTO> buildCategoryTree(List<CaseCategory> all, Long parentId) {
        return all.stream()
                .filter(c -> parentId.equals(c.getParentId()))
                .map(c -> {
                    CaseCategoryDTO dto = toCategoryDTO(c);
                    dto.setChildren(buildCategoryTree(all, c.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private String getSourceName(String source) {
        if (source == null) return null;
        return switch (source) {
            case CaseLibrary.SOURCE_EXTERNAL -> "外部案例";
            case CaseLibrary.SOURCE_INTERNAL -> "内部案例";
            default -> source;
        };
    }

    private CaseCategoryDTO toCategoryDTO(CaseCategory category) {
        CaseCategoryDTO dto = new CaseCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setLevel(category.getLevel());
        dto.setSortOrder(category.getSortOrder());
        dto.setDescription(category.getDescription());
        return dto;
    }

    private CaseLibraryDTO toCaseDTO(CaseLibrary caseLib, Long userId) {
        CaseLibraryDTO dto = new CaseLibraryDTO();
        dto.setId(caseLib.getId());
        dto.setTitle(caseLib.getTitle());
        dto.setCategoryId(caseLib.getCategoryId());
        dto.setCaseNumber(caseLib.getCaseNumber());
        dto.setCourtName(caseLib.getCourtName());
        dto.setJudgeDate(caseLib.getJudgeDate());
        dto.setCaseType(caseLib.getCaseType());
        dto.setCauseOfAction(caseLib.getCauseOfAction());
        dto.setTrialProcedure(caseLib.getTrialProcedure());
        dto.setPlaintiff(caseLib.getPlaintiff());
        dto.setDefendant(caseLib.getDefendant());
        dto.setCaseSummary(caseLib.getCaseSummary());
        dto.setCourtOpinion(caseLib.getCourtOpinion());
        dto.setJudgmentResult(caseLib.getJudgmentResult());
        dto.setCaseSignificance(caseLib.getCaseSignificance());
        dto.setKeywords(caseLib.getKeywords());
        dto.setSource(caseLib.getSource());
        dto.setSourceName(getSourceName(caseLib.getSource()));
        dto.setMatterId(caseLib.getMatterId());
        dto.setAttachmentUrl(caseLib.getAttachmentUrl());
        dto.setViewCount(caseLib.getViewCount());
        dto.setCollectCount(caseLib.getCollectCount());
        dto.setCreatedAt(caseLib.getCreatedAt());

        // 获取分类名称
        CaseCategory category = caseCategoryRepository.getById(caseLib.getCategoryId());
        if (category != null) {
            dto.setCategoryName(category.getName());
        }

        // 检查是否已收藏
        if (userId != null) {
            int count = knowledgeCollectionMapper.countByUserAndTarget(userId, KnowledgeCollection.TYPE_CASE, caseLib.getId());
            dto.setCollected(count > 0);
        }

        return dto;
    }
}
