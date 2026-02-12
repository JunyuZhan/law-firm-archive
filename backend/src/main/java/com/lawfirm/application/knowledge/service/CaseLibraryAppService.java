package com.lawfirm.application.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.knowledge.command.CreateCaseLibraryCommand;
import com.lawfirm.application.knowledge.dto.CaseCategoryDTO;
import com.lawfirm.application.knowledge.dto.CaseLibraryDTO;
import com.lawfirm.application.knowledge.dto.CaseLibraryQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.CaseCategory;
import com.lawfirm.domain.knowledge.entity.CaseLibrary;
import com.lawfirm.domain.knowledge.entity.KnowledgeCollection;
import com.lawfirm.domain.knowledge.repository.CaseCategoryRepository;
import com.lawfirm.domain.knowledge.repository.CaseLibraryRepository;
import com.lawfirm.infrastructure.persistence.mapper.CaseCategoryMapper;
import com.lawfirm.infrastructure.persistence.mapper.CaseLibraryMapper;
import com.lawfirm.infrastructure.persistence.mapper.KnowledgeCollectionMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 案例库应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseLibraryAppService {

  /** 案例分类仓储 */
  private final CaseCategoryRepository caseCategoryRepository;

  /** 案例分类Mapper */
  private final CaseCategoryMapper caseCategoryMapper;

  /** 案例库仓储 */
  private final CaseLibraryRepository caseLibraryRepository;

  /** 案例库Mapper */
  private final CaseLibraryMapper caseLibraryMapper;

  /** 知识收藏Mapper */
  private final KnowledgeCollectionMapper knowledgeCollectionMapper;

  /**
   * 获取案例分类树.
   *
   * @return 分类树
   */
  public List<CaseCategoryDTO> getCategoryTree() {
    List<CaseCategory> all = caseCategoryMapper.selectAllCategories();
    return buildCategoryTree(all, 0L);
  }

  /**
   * 分页查询案例. 优化N+1查询
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<CaseLibraryDTO> listCases(final CaseLibraryQueryDTO query) {
    IPage<CaseLibrary> page =
        caseLibraryMapper.selectCasePage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getCategoryId(),
            query.getSource(),
            query.getCaseType(),
            query.getKeyword());

    List<CaseLibrary> cases = page.getRecords();
    if (cases.isEmpty()) {
      return PageResult.of(
          java.util.Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
    }

    Long userId = SecurityUtils.getUserId();

    // 批量加载分类信息（避免N+1）
    java.util.Set<Long> categoryIds =
        cases.stream()
            .map(CaseLibrary::getCategoryId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    java.util.Map<Long, CaseCategory> categoryMap =
        categoryIds.isEmpty()
            ? java.util.Collections.emptyMap()
            : caseCategoryRepository.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(CaseCategory::getId, c -> c, (a, b) -> a));

    // 批量加载收藏状态（避免N+1）
    java.util.Map<Long, Boolean> collectedMap = new java.util.HashMap<>();
    if (userId != null) {
      java.util.List<Long> caseIds =
          cases.stream().map(CaseLibrary::getId).collect(Collectors.toList());

      // 批量查询用户收藏的案例
      java.util.List<KnowledgeCollection> collections =
          knowledgeCollectionMapper.selectBatchByUserAndTargets(
              userId, KnowledgeCollection.TYPE_CASE, caseIds);

      java.util.Set<Long> collectedCaseIds =
          collections.stream().map(KnowledgeCollection::getTargetId).collect(Collectors.toSet());

      for (Long caseId : caseIds) {
        collectedMap.put(caseId, collectedCaseIds.contains(caseId));
      }
    }

    // 使用预加载的Map转换DTO（避免N+1）
    List<CaseLibraryDTO> records =
        cases.stream()
            .map(c -> toCaseDTO(c, userId, categoryMap, collectedMap))
            .collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取案例详情.
   *
   * @param id 案例ID
   * @return 案例DTO
   */
  public CaseLibraryDTO getCaseById(final Long id) {
    CaseLibrary caseLib = caseLibraryRepository.getByIdOrThrow(id, "案例不存在");
    caseLibraryMapper.incrementViewCount(id);
    return toCaseDTO(caseLib, SecurityUtils.getUserId());
  }

  /**
   * 创建案例.
   *
   * @param command 创建命令
   * @return 案例DTO
   */
  @Transactional
  public CaseLibraryDTO createCase(final CreateCaseLibraryCommand command) {
    if (!StringUtils.hasText(command.getTitle())) {
      throw new BusinessException("案例标题不能为空");
    }

    CaseLibrary caseLib =
        CaseLibrary.builder()
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
   * 更新案例.
   *
   * @param id 案例ID
   * @param command 更新命令
   * @return 案例DTO
   */
  @Transactional
  public CaseLibraryDTO updateCase(final Long id, final CreateCaseLibraryCommand command) {
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
   * 删除案例.
   * 同时清理关联的收藏记录，保证数据一致性
   *
   * @param id 案例ID
   */
  @Transactional
  public void deleteCase(final Long id) {
    CaseLibrary caseLib = caseLibraryRepository.getByIdOrThrow(id, "案例不存在");

    // 先删除关联的收藏记录
    int deletedCollections =
        knowledgeCollectionMapper.deleteByTargetTypeAndTargetId(KnowledgeCollection.TYPE_CASE, id);
    if (deletedCollections > 0) {
      log.info("删除案例收藏记录: caseId={}, count={}", id, deletedCollections);
    }

    // 再删除案例
    caseLibraryMapper.deleteById(id);
    log.info("案例删除成功: {}", caseLib.getTitle());
  }

  /**
   * 收藏案例. 并发安全：使用数据库唯一约束 + DuplicateKeyException 处理
   *
   * @param caseId 案例ID
   */
  @Transactional(rollbackFor = Exception.class)
  public void collectCase(final Long caseId) {
    Long userId = SecurityUtils.getUserId();
    caseLibraryRepository.getByIdOrThrow(caseId, "案例不存在");

    try {
      // 直接插入，依赖数据库唯一约束（user_id + target_type + target_id）处理并发
      KnowledgeCollection collection =
          KnowledgeCollection.builder()
              .userId(userId)
              .targetType(KnowledgeCollection.TYPE_CASE)
              .targetId(caseId)
              .build();
      knowledgeCollectionMapper.insert(collection);
      caseLibraryMapper.incrementCollectCount(caseId);
      log.info("案例收藏成功: userId={}, caseId={}", userId, caseId);

    } catch (org.springframework.dao.DuplicateKeyException e) {
      // 唯一约束冲突 = 已收藏
      throw new BusinessException("已收藏该案例");
    }
  }

  /**
   * 取消收藏案例.
   *
   * @param caseId 案例ID
   */
  @Transactional
  public void uncollectCase(final Long caseId) {
    Long userId = SecurityUtils.getUserId();
    int deleted =
        knowledgeCollectionMapper.deleteByUserAndTarget(
            userId, KnowledgeCollection.TYPE_CASE, caseId);
    if (deleted > 0) {
      caseLibraryMapper.decrementCollectCount(caseId);
      log.info("案例取消收藏: userId={}, caseId={}", userId, caseId);
    }
  }

  /**
   * 获取我的收藏案例
   * 优化：使用批量加载避免N+1查询
   *
   * @return 案例列表
   */
  public List<CaseLibraryDTO> getMyCollectedCases() {
    Long userId = SecurityUtils.getUserId();
    List<KnowledgeCollection> collections =
        knowledgeCollectionMapper.selectByUserAndType(userId, KnowledgeCollection.TYPE_CASE);

    if (collections.isEmpty()) {
      return java.util.Collections.emptyList();
    }

    // 批量加载案例信息
    java.util.List<Long> caseIds =
        collections.stream()
            .map(KnowledgeCollection::getTargetId)
            .collect(Collectors.toList());
    java.util.Map<Long, CaseLibrary> caseMap =
        caseLibraryRepository.listByIds(caseIds).stream()
            .collect(Collectors.toMap(CaseLibrary::getId, c -> c, (a, b) -> a));

    // 批量加载分类信息
    java.util.Set<Long> categoryIds =
        caseMap.values().stream()
            .map(CaseLibrary::getCategoryId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    java.util.Map<Long, CaseCategory> categoryMap =
        categoryIds.isEmpty()
            ? java.util.Collections.emptyMap()
            : caseCategoryRepository.listByIds(new java.util.ArrayList<>(categoryIds)).stream()
                .collect(Collectors.toMap(CaseCategory::getId, c -> c, (a, b) -> a));

    // 所有收藏的案例都标记为已收藏
    java.util.Map<Long, Boolean> collectedMap =
        caseIds.stream().collect(Collectors.toMap(id -> id, id -> true));

    // 按原始收藏顺序返回
    return caseIds.stream()
        .map(caseMap::get)
        .filter(java.util.Objects::nonNull)
        .map(c -> toCaseDTO(c, userId, categoryMap, collectedMap))
        .collect(Collectors.toList());
  }

  /**
   * 构建分类树.
   *
   * @param all 所有分类列表
   * @param parentId 父分类ID
   * @return 分类树
   */
  /**
   * 构建分类树.
   *
   * @param all 所有分类
   * @param parentId 父分类ID
   * @return 分类树列表
   */
  private List<CaseCategoryDTO> buildCategoryTree(
      final List<CaseCategory> all, final Long parentId) {
    return all.stream()
        .filter(c -> parentId.equals(c.getParentId()))
        .map(
            c -> {
              CaseCategoryDTO dto = toCategoryDTO(c);
              dto.setChildren(buildCategoryTree(all, c.getId()));
              return dto;
            })
        .collect(Collectors.toList());
  }

  /**
   * 获取来源名称.
   *
   * @param source 来源代码
   * @return 来源名称
   */
  /**
   * 获取来源名称.
   *
   * @param source 来源代码
   * @return 来源名称
   */
  private String getSourceName(final String source) {
    if (source == null) {
      return null;
    }
    return switch (source) {
      case CaseLibrary.SOURCE_EXTERNAL -> "外部案例";
      case CaseLibrary.SOURCE_INTERNAL -> "内部案例";
      default -> source;
    };
  }

  /**
   * Entity转DTO.
   *
   * @param category 案例分类实体
   * @return 案例分类DTO
   */
  /**
   * 分类实体转DTO.
   *
   * @param category 分类实体
   * @return 分类DTO
   */
  private CaseCategoryDTO toCategoryDTO(final CaseCategory category) {
    CaseCategoryDTO dto = new CaseCategoryDTO();
    dto.setId(category.getId());
    dto.setName(category.getName());
    dto.setParentId(category.getParentId());
    dto.setLevel(category.getLevel());
    dto.setSortOrder(category.getSortOrder());
    dto.setDescription(category.getDescription());
    return dto;
  }

  /**
   * Entity转DTO（带预加载数据，避免N+1查询）.
   *
   * @param caseLib 案例实体
   * @param userId 用户ID
   * @param categoryMap 分类Map
   * @param collectedMap 收藏Map
   * @return 案例DTO
   */
  private CaseLibraryDTO toCaseDTO(
      final CaseLibrary caseLib,
      final Long userId,
      final java.util.Map<Long, CaseCategory> categoryMap,
      final java.util.Map<Long, Boolean> collectedMap) {
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

    // 从预加载Map获取分类名称（避免N+1）
    if (caseLib.getCategoryId() != null) {
      CaseCategory category = categoryMap.get(caseLib.getCategoryId());
      if (category != null) {
        dto.setCategoryName(category.getName());
      }
    }

    // 从预加载Map获取收藏状态（避免N+1）
    if (userId != null) {
      dto.setCollected(collectedMap.getOrDefault(caseLib.getId(), false));
    }

    return dto;
  }

  /**
   * Entity转DTO（单条查询用，会触发额外查询）
   *
   * @param caseLib 案例实体
   * @param userId 用户ID
   * @return 案例DTO
   */
  private CaseLibraryDTO toCaseDTO(final CaseLibrary caseLib, final Long userId) {
    // 单条查询时构建Map
    java.util.Map<Long, CaseCategory> categoryMap = new java.util.HashMap<>();
    java.util.Map<Long, Boolean> collectedMap = new java.util.HashMap<>();

    if (caseLib.getCategoryId() != null) {
      CaseCategory category = caseCategoryRepository.getById(caseLib.getCategoryId());
      if (category != null) {
        categoryMap.put(category.getId(), category);
      }
    }

    if (userId != null) {
      int count =
          knowledgeCollectionMapper.countByUserAndTarget(
              userId, KnowledgeCollection.TYPE_CASE, caseLib.getId());
      collectedMap.put(caseLib.getId(), count > 0);
    }

    return toCaseDTO(caseLib, userId, categoryMap, collectedMap);
  }
}
