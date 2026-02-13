package com.lawfirm.application.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.client.command.CreateConflictCheckCommand;
import com.lawfirm.application.client.dto.ConflictCheckDTO;
import com.lawfirm.application.client.dto.ConflictCheckItemDTO;
import com.lawfirm.application.client.dto.ConflictCheckQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.ConflictStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ConflictCheck;
import com.lawfirm.domain.client.entity.ConflictCheckItem;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ConflictCheckRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.persistence.mapper.ConflictCheckItemMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 利冲检查应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictCheckAppService {

  /** 随机数范围. */
  private static final int RANDOM_BOUND = 100000;

  /** 匹配分数：高风险阈值. */
  private static final int SCORE_HIGH_RISK = 80;

  /** 匹配分数：中风险阈值. */
  private static final int SCORE_MEDIUM_RISK = 60;

  /** 匹配分数：最大值. */
  private static final int SCORE_MAX = 99;

  /** 匹配分数：最小值. */
  private static final int SCORE_MIN = 30;

  /** 长度差异惩罚系数 */
  private static final double LENGTH_PENALTY_FACTOR = 0.5;

  /** 高风险匹配分数阈值 */
  private static final int HIGH_RISK_SCORE_THRESHOLD = 90;

  /** 中风险匹配分数阈值 */
  private static final int MEDIUM_RISK_SCORE_THRESHOLD = 70;

  /** 匹配分数：前缀匹配加分. */
  private static final int SCORE_PREFIX_BONUS = 15;

  /** 匹配分数：后缀匹配加分. */
  private static final int SCORE_SUFFIX_BONUS = 10;

  /** 匹配分数：前缀匹配最大基础分. */
  private static final int SCORE_PREFIX_MAX_BASE = 95;

  /** 匹配分数：后缀匹配最大基础分. */
  private static final int SCORE_SUFFIX_MAX_BASE = 90;

  /** 匹配分数：去后缀完全匹配. */
  private static final int SCORE_NO_SUFFIX_MATCH = 95;

  /** 匹配分数：去后缀包含匹配. */
  private static final int SCORE_NO_SUFFIX_CONTAINS = 75;

  /** 相似度阈值. */
  private static final int SIMILARITY_THRESHOLD = 50;

  /** 相似度高风险阈值. */
  private static final int SIMILARITY_HIGH_RISK = 80;

  /** 相似度标准化匹配分数. */
  private static final int SIMILARITY_NORMALIZED_MATCH = 95;

  /** 利冲检查仓储. */
  private final ConflictCheckRepository conflictCheckRepository;

  /** 利冲检查项Mapper. */
  private final ConflictCheckItemMapper conflictCheckItemMapper;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /** 案件仓储. */
  private final MatterRepository matterRepository;

  /** 审批服务. */
  private final ApprovalService approvalService;

  /** 审批人服务. */
  private final ApproverService approverService;

  /** 用户Mapper. */
  private final UserMapper userMapper;

  /**
   * 分页查询利冲检查（优化N+1查询）
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<ConflictCheckDTO> listConflictChecks(final ConflictCheckQueryDTO query) {
    LambdaQueryWrapper<ConflictCheck> wrapper = new LambdaQueryWrapper<>();

    if (StringUtils.hasText(query.getCheckNo())) {
      wrapper.like(ConflictCheck::getCheckNo, query.getCheckNo());
    }
    if (query.getMatterId() != null) {
      wrapper.eq(ConflictCheck::getMatterId, query.getMatterId());
    }
    if (query.getClientId() != null) {
      wrapper.eq(ConflictCheck::getClientId, query.getClientId());
    }
    if (StringUtils.hasText(query.getStatus())) {
      wrapper.eq(ConflictCheck::getStatus, query.getStatus());
    }
    if (query.getApplicantId() != null) {
      wrapper.eq(ConflictCheck::getApplicantId, query.getApplicantId());
    }

    wrapper.orderByDesc(ConflictCheck::getCreatedAt);

    IPage<ConflictCheck> page =
        conflictCheckRepository.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

    List<ConflictCheck> checks = page.getRecords();
    if (checks.isEmpty()) {
      return PageResult.of(
          java.util.Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
    }

    // 批量加载用户信息（解决N+1查询）
    java.util.Set<Long> userIds = new java.util.HashSet<>();
    checks.forEach(
        check -> {
          if (check.getApplicantId() != null) {
            userIds.add(check.getApplicantId());
          }
          if (check.getReviewerId() != null) {
            userIds.add(check.getReviewerId());
          }
        });
    java.util.Map<Long, User> userMap =
        userIds.isEmpty()
            ? java.util.Collections.emptyMap()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

    // 批量加载项目信息
    java.util.Set<Long> matterIds =
        checks.stream()
            .map(ConflictCheck::getMatterId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    java.util.Map<Long, Matter> matterMap =
        matterIds.isEmpty()
            ? java.util.Collections.emptyMap()
            : matterRepository.listByIds(matterIds).stream()
                .collect(Collectors.toMap(Matter::getId, m -> m, (a, b) -> a));

    // 使用预加载的Map转换DTO（避免N+1）
    List<ConflictCheckDTO> records =
        checks.stream().map(check -> toDTO(check, userMap, matterMap)).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 创建利冲检查
   *
   * @param command 创建利冲检查命令
   * @return 利冲检查DTO
   */
  @Transactional
  public ConflictCheckDTO createConflictCheck(final CreateConflictCheckCommand command) {
    // 1. 验证案件存在
    Matter matter = matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");

    // 2. 生成检查编号
    String checkNo = generateCheckNo();

    // 3. 创建检查记录
    ConflictCheck check =
        ConflictCheck.builder()
            .checkNo(checkNo)
            .checkType("NEW_MATTER")
            .matterId(command.getMatterId())
            .clientId(command.getClientId() != null ? command.getClientId() : matter.getClientId())
            .clientName(matter.getName())
            .status(ConflictStatus.PENDING)
            .applicantId(SecurityUtils.getUserId())
            .remark(command.getRemark())
            .build();

    conflictCheckRepository.save(check);

    // 4. 创建检查项并执行检查
    List<ConflictCheckItem> items = new ArrayList<>();
    boolean hasConflict = false;

    for (CreateConflictCheckCommand.PartyCommand party : command.getParties()) {
      ConflictCheckItem item =
          ConflictCheckItem.builder()
              .checkId(check.getId())
              .partyName(party.getPartyName())
              .partyType(party.getPartyType())
              .idNumber(party.getIdNumber())
              .hasConflict(false)
              .build();

      // 执行冲突检查
      ConflictResult result =
          checkConflict(party.getPartyName(), party.getIdNumber(), command.getMatterId());
      if (result.isHasConflict()) {
        item.setHasConflict(true);
        item.setConflictDetail(result.getDetail());
        item.setRelatedMatterId(result.getRelatedMatterId());
        item.setRelatedClientId(result.getRelatedClientId());
        hasConflict = true;
      }

      conflictCheckItemMapper.insert(item);
      items.add(item);
    }

    // 5. 更新检查状态
    check.setStatus(hasConflict ? ConflictStatus.CONFLICT : ConflictStatus.PASSED);
    conflictCheckRepository.updateById(check);

    // 6. 更新案件冲突状态
    matter.setConflictStatus(hasConflict ? ConflictStatus.CONFLICT : ConflictStatus.PASSED);
    matterRepository.updateById(matter);

    // 7. 如果检测到冲突，创建审批记录
    if (hasConflict) {
      Long approverId = approverService.findConflictCheckApprover();
      if (approverId == null) {
        approverId = approverService.findDefaultApprover();
      }

      approvalService.createApproval(
          "CONFLICT_CHECK",
          check.getId(),
          check.getCheckNo(),
          "利冲检查：" + check.getClientName(),
          approverId,
          "HIGH", // 冲突检查优先级高
          "URGENT", // 冲突检查紧急
          null // businessSnapshot
          );

      log.info("利冲检查发现冲突，已创建审批记录: {} (审批人: {})", check.getCheckNo(), approverId);
    }

    log.info("利冲检查完成: {} ({}), 结果: {}", check.getCheckNo(), hasConflict ? "存在冲突" : "无冲突");

    ConflictCheckDTO dto = toDTO(check);
    dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
    return dto;
  }

  /**
   * 快速利冲检索结果（增强版，包含候选列表和风险评估）.
   *
   * @param hasConflict 是否存在高风险冲突
   * @param conflictDetail 冲突详情（兼容旧版）
   * @param candidates 候选冲突列表
   * @param riskLevel 整体风险等级: HIGH/MEDIUM/LOW/NONE
   * @param riskSummary 风险摘要
   */
  public record QuickConflictCheckResult(
      boolean hasConflict, // 是否存在高风险冲突
      String conflictDetail, // 冲突详情（兼容旧版）
      List<ConflictCandidate> candidates, // 候选冲突列表
      String riskLevel, // 整体风险等级: HIGH/MEDIUM/LOW/NONE
      String riskSummary // 风险摘要
      ) {
    /**
     * 兼容旧版构造函数
     *
     * @param hasConflict 是否存在冲突
     * @param conflictDetail 冲突详情
     */
    public QuickConflictCheckResult(final boolean hasConflict, final String conflictDetail) {
      this(
          hasConflict,
          conflictDetail,
          new ArrayList<>(),
          hasConflict ? "HIGH" : "NONE",
          conflictDetail);
    }

    /**
     * 获取风险等级描述
     *
     * @return 风险等级描述
     */
    public String getRiskLevelDescription() {
      return switch (riskLevel) {
        case "HIGH" -> "高风险";
        case "MEDIUM" -> "中风险";
        case "LOW" -> "低风险";
        case "NONE" -> "无风险";
        default -> "未知风险";
      };
    }
  }

  /**
   * 冲突候选项.
   *
   * @param clientId 客户ID
   * @param clientNo 客户编号
   * @param clientName 客户名称
   * @param clientType 客户类型: PERSONAL/ENTERPRISE
   * @param matchScore 匹配分数 0-100
   * @param matchType 匹配类型: EXACT/CONTAINS/SIMILAR
   * @param riskLevel 风险等级: HIGH/MEDIUM/LOW
   * @param riskReason 风险原因
   */
  public record ConflictCandidate(
      Long clientId, // 客户ID
      String clientNo, // 客户编号
      String clientName, // 客户名称
      String clientType, // 客户类型: PERSONAL/ENTERPRISE
      int matchScore, // 匹配分数 0-100
      String matchType, // 匹配类型: EXACT/CONTAINS/SIMILAR
      String riskLevel, // 风险等级: HIGH/MEDIUM/LOW
      String riskReason // 风险原因
      ) {}

  /**
   * 快速利冲检索（增强版） 返回候选列表和风险评估，支持模糊匹配和相似度计算
   *
   * @param clientName 客户名称
   * @param opposingParty 对方当事人
   * @return 检查结果（包含候选列表和风险评估）
   */
  public QuickConflictCheckResult quickConflictCheck(
      final String clientName, final String opposingParty) {
    // 名称太短时返回空结果
    if (opposingParty == null || opposingParty.trim().length() < 2) {
      return new QuickConflictCheckResult(
          false, null, new ArrayList<>(), "NONE", "输入名称过短，无法进行有效检索");
    }

    String searchName = opposingParty.trim();
    List<ConflictCandidate> candidates = new ArrayList<>();

    // 1. 精确匹配查询
    List<Client> exactMatches =
        clientRepository.list(new LambdaQueryWrapper<Client>().eq(Client::getName, searchName));
    for (Client client : exactMatches) {
      candidates.add(
          new ConflictCandidate(
              client.getId(),
              client.getClientNo(),
              client.getName(),
              client.getClientType(),
              100, // 精确匹配100分
              "EXACT",
              "HIGH",
              "名称完全匹配，确认为同一主体"));
    }

    // 2. 包含匹配查询（排除已精确匹配的）
    List<Client> containsMatches =
        clientRepository.list(
            new LambdaQueryWrapper<Client>()
                .like(Client::getName, searchName)
                .notIn(
                    !exactMatches.isEmpty(),
                    Client::getId,
                    exactMatches.stream().map(Client::getId).collect(Collectors.toList())));
    for (Client client : containsMatches) {
      int score = calculateContainsScore(searchName, client.getName());
      String riskLevel =
          score >= SCORE_HIGH_RISK ? "HIGH" : (score >= SCORE_MEDIUM_RISK ? "MEDIUM" : "LOW");
      candidates.add(
          new ConflictCandidate(
              client.getId(),
              client.getClientNo(),
              client.getName(),
              client.getClientType(),
              score,
              "CONTAINS",
              riskLevel,
              generateMatchReason(searchName, client.getName(), "CONTAINS")));
    }

    // 3. 相似度匹配（针对可能的错别字、简称等情况）
    // 只在没有精确匹配时进行，避免性能问题
    if (exactMatches.isEmpty() && searchName.length() >= 2) {
      List<ConflictCandidate> similarCandidates = findSimilarClients(searchName, candidates);
      candidates.addAll(similarCandidates);
    }

    // 4. 按匹配分数排序
    candidates.sort((a, b) -> Integer.compare(b.matchScore(), a.matchScore()));

    // 5. 限制返回数量（最多10个）
    if (candidates.size() > 10) {
      candidates = new ArrayList<>(candidates.subList(0, 10));
    }

    // 6. 计算整体风险等级
    String overallRisk = calculateOverallRisk(candidates);
    boolean hasHighRisk = candidates.stream().anyMatch(c -> "HIGH".equals(c.riskLevel()));

    // 7. 生成风险摘要
    String riskSummary = generateRiskSummary(searchName, candidates, overallRisk);

    // 8. 兼容旧版：如果有高风险匹配，设置conflictDetail
    String conflictDetail = null;
    if (hasHighRisk && !candidates.isEmpty()) {
      ConflictCandidate topMatch = candidates.get(0);
      conflictDetail =
          String.format(
              "对方当事人【%s】与本所客户【%s】（编号：%s）高度匹配（相似度%d%%）。建议人工核实。",
              searchName, topMatch.clientName(), topMatch.clientNo(), topMatch.matchScore());
    }

    return new QuickConflictCheckResult(
        hasHighRisk, conflictDetail, candidates, overallRisk, riskSummary);
  }

  /**
   * 计算包含匹配的分数
   *
   * @param searchName 搜索名称
   * @param clientName 客户名称
   * @return 匹配分数
   */
  private int calculateContainsScore(final String searchName, final String clientName) {
    // 基础分数：搜索词占客户名称的比例
    double ratio = (double) searchName.length() / clientName.length();
    int baseScore = (int) (ratio * 100);

    // 加分项
    // 1. 如果搜索词是客户名称的前缀（如"北京XX"匹配"北京XX有限公司"）
    if (clientName.startsWith(searchName)) {
      baseScore = Math.min(SCORE_PREFIX_MAX_BASE, baseScore + SCORE_PREFIX_BONUS);
    } else if (clientName.endsWith(searchName)) {
      // 2. 如果搜索词是客户名称的后缀
      baseScore = Math.min(SCORE_SUFFIX_MAX_BASE, baseScore + SCORE_SUFFIX_BONUS);
    }

    // 企业名称特殊处理：去掉常见后缀后比较
    String normalizedClient = normalizeCompanyName(clientName);
    String normalizedSearch = normalizeCompanyName(searchName);
    if (normalizedClient.equals(normalizedSearch)) {
      baseScore = SCORE_NO_SUFFIX_MATCH; // 去掉后缀后完全匹配
    } else if (normalizedClient.contains(normalizedSearch)
        || normalizedSearch.contains(normalizedClient)) {
      baseScore = Math.max(baseScore, SCORE_NO_SUFFIX_CONTAINS);
    }

    return Math.min(SCORE_MAX, Math.max(SCORE_MIN, baseScore)); // 包含匹配最高99分，最低30分
  }

  /**
   * 标准化公司名称（去掉常见后缀）
   *
   * @param name 公司名称
   * @return 标准化后的名称
   */
  private String normalizeCompanyName(final String name) {
    if (name == null) {
      return "";
    }
    return name.replaceAll("(有限公司|股份有限公司|有限责任公司|集团|公司|企业|厂|店|中心|事务所)$", "")
        .replaceAll("^(中国|北京|上海|广州|深圳|天津|重庆)", "")
        .trim();
  }

  /**
   * 查找相似客户（处理错别字、简称等）
   *
   * @param searchName 搜索名称
   * @param existingCandidates 已有候选列表
   * @return 相似客户列表
   */
  private List<ConflictCandidate> findSimilarClients(
      final String searchName, final List<ConflictCandidate> existingCandidates) {
    List<ConflictCandidate> similarCandidates = new ArrayList<>();
    Set<Long> existingIds =
        existingCandidates.stream().map(ConflictCandidate::clientId).collect(Collectors.toSet());

    // 获取所有客户进行相似度比较（实际生产环境应该用搜索引擎或限制范围）
    // 这里限制只查询最近的1000个客户，避免性能问题
    List<Client> recentClients =
        clientRepository.list(
            new LambdaQueryWrapper<Client>()
                .notIn(!existingIds.isEmpty(), Client::getId, existingIds)
                .orderByDesc(Client::getCreatedAt)
                .last("LIMIT 1000"));

    for (Client client : recentClients) {
      int similarity = calculateSimilarity(searchName, client.getName());
      if (similarity >= SIMILARITY_THRESHOLD) {
        // 相似度>=50%才考虑
        String riskLevel = similarity >= SIMILARITY_HIGH_RISK ? "MEDIUM" : "LOW";
        similarCandidates.add(
            new ConflictCandidate(
                client.getId(),
                client.getClientNo(),
                client.getName(),
                client.getClientType(),
                similarity,
                "SIMILAR",
                riskLevel,
                generateMatchReason(searchName, client.getName(), "SIMILAR")));
      }
    }

    // 只返回相似度最高的5个
    similarCandidates.sort((a, b) -> Integer.compare(b.matchScore(), a.matchScore()));
    if (similarCandidates.size() > 5) {
      similarCandidates = new ArrayList<>(similarCandidates.subList(0, 5));
    }

    return similarCandidates;
  }

  /**
   * 计算字符串相似度（编辑距离算法）
   *
   * @param s1 字符串1
   * @param s2 字符串2
   * @return 相似度分数
   */
  private int calculateSimilarity(final String s1, final String s2) {
    if (s1 == null || s2 == null) {
      return 0;
    }
    if (s1.equals(s2)) {
      return 100;
    }

    // 标准化后比较
    String n1 = normalizeCompanyName(s1);
    String n2 = normalizeCompanyName(s2);
    if (n1.equals(n2)) {
      return SIMILARITY_NORMALIZED_MATCH;
    }

    // 计算编辑距离
    int distance = levenshteinDistance(s1, s2);
    int maxLen = Math.max(s1.length(), s2.length());
    if (maxLen == 0) {
      return 100;
    }

    // 转换为相似度百分比
    int similarity = (int) ((1.0 - (double) distance / maxLen) * 100);

    // 额外检查：共同字符比例（使用较长字符串作为分母，避免短名称匹配过高）
    int commonChars = countCommonChars(s1, s2);
    int commonRatio = (int) ((double) commonChars / Math.max(s1.length(), s2.length()) * 100);

    // 如果长度差异较大，降低相似度（避免"张三"和"张三疯"被认为100%相似）
    int lenDiff = Math.abs(s1.length() - s2.length());
    int minLen = Math.min(s1.length(), s2.length());
    if (lenDiff > 0 && minLen > 0) {
      // 长度差异惩罚：每多一个字符差异，降低一定比例
      double lenPenalty = 1.0 - (double) lenDiff / (minLen + lenDiff) * LENGTH_PENALTY_FACTOR;
      similarity = (int) (similarity * lenPenalty);
      commonRatio = (int) (commonRatio * lenPenalty);
    }

    // 取两种算法的较高值
    return Math.max(similarity, commonRatio);
  }

  /**
   * 计算编辑距离（Levenshtein Distance）
   *
   * @param s1 字符串1
   * @param s2 字符串2
   * @return 编辑距离
   */
  private int levenshteinDistance(final String s1, final String s2) {
    int m = s1.length();
    int n = s2.length();
    int[][] dp = new int[m + 1][n + 1];

    for (int i = 0; i <= m; i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= n; j++) {
      dp[0][j] = j;
    }

    for (int i = 1; i <= m; i++) {
      for (int j = 1; j <= n; j++) {
        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
          dp[i][j] = dp[i - 1][j - 1];
        } else {
          dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
        }
      }
    }
    return dp[m][n];
  }

  /**
   * 计算共同字符数
   *
   * @param s1 字符串1
   * @param s2 字符串2
   * @return 共同字符数
   */
  private int countCommonChars(final String s1, final String s2) {
    Set<Character> set1 = s1.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
    return (int) s2.chars().mapToObj(c -> (char) c).filter(set1::contains).count();
  }

  /**
   * 生成匹配原因说明
   *
   * @param searchName 搜索名称
   * @param clientName 客户名称
   * @param matchType 匹配类型
   * @return 匹配原因说明
   */
  private String generateMatchReason(
      final String searchName, final String clientName, final String matchType) {
    return switch (matchType) {
      case "EXACT" -> "名称完全匹配";
      case "CONTAINS" -> {
        if (clientName.startsWith(searchName)) {
          yield "搜索词「" + searchName + "」是客户名称的前缀";
        } else if (clientName.endsWith(searchName)) {
          yield "搜索词「" + searchName + "」是客户名称的后缀";
        } else {
          yield "客户名称包含搜索词「" + searchName + "」";
        }
      }
      case "SIMILAR" -> "名称相似，可能是简称、别名或存在错别字";
      default -> "名称匹配";
    };
  }

  /**
   * 计算整体风险等级
   *
   * @param candidates 候选冲突列表
   * @return 风险等级
   */
  private String calculateOverallRisk(final List<ConflictCandidate> candidates) {
    if (candidates.isEmpty()) {
      return "NONE";
    }

    // 有精确匹配或高分匹配
    boolean hasExact = candidates.stream().anyMatch(c -> "EXACT".equals(c.matchType()));
    boolean hasHighScore =
        candidates.stream().anyMatch(c -> c.matchScore() >= HIGH_RISK_SCORE_THRESHOLD);
    boolean hasMediumScore =
        candidates.stream().anyMatch(c -> c.matchScore() >= MEDIUM_RISK_SCORE_THRESHOLD);

    if (hasExact || hasHighScore) {
      return "HIGH";
    }
    if (hasMediumScore) {
      return "MEDIUM";
    }
    return "LOW";
  }

  /**
   * 生成风险摘要
   *
   * @param searchName 搜索名称
   * @param candidates 候选列表
   * @param riskLevel 风险等级
   * @return 风险摘要
   */
  private String generateRiskSummary(
      final String searchName, final List<ConflictCandidate> candidates, final String riskLevel) {
    if (candidates.isEmpty()) {
      return "未发现与「" + searchName + "」名称相近的现有客户";
    }

    int highCount = (int) candidates.stream().filter(c -> "HIGH".equals(c.riskLevel())).count();
    int mediumCount = (int) candidates.stream().filter(c -> "MEDIUM".equals(c.riskLevel())).count();
    int lowCount = (int) candidates.stream().filter(c -> "LOW".equals(c.riskLevel())).count();

    StringBuilder sb = new StringBuilder();
    sb.append("发现 ").append(candidates.size()).append(" 个相似客户：");

    if (highCount > 0) {
      sb.append("高度相似 ").append(highCount).append(" 个，");
    }
    if (mediumCount > 0) {
      sb.append("部分相似 ").append(mediumCount).append(" 个，");
    }
    if (lowCount > 0) {
      sb.append("名称相近 ").append(lowCount).append(" 个，");
    }

    sb.setLength(sb.length() - 1); // 去掉最后的逗号
    sb.append("。");

    if ("HIGH".equals(riskLevel)) {
      sb.append("请仔细核对是否为同一客户。");
    } else if ("MEDIUM".equals(riskLevel)) {
      sb.append("请确认是否为同一人/公司。");
    }

    return sb.toString();
  }

  /** 手动申请利冲审查（简化版，不需要关联案件）. */
  /**
   * 申请利冲检查
   *
   * @param clientName 客户名称
   * @param opposingParty 对方当事人
   * @param matterName 案件名称
   * @param checkType 检查类型
   * @param remark 备注
   * @return 利冲检查DTO
   */
  @Transactional
  public ConflictCheckDTO applyConflictCheck(
      final String clientName,
      final String opposingParty,
      final String matterName,
      final String checkType,
      final String remark) {
    // 1. 生成检查编号
    String checkNo = generateCheckNo();

    // 2. 创建检查记录
    ConflictCheck check =
        ConflictCheck.builder()
            .checkNo(checkNo)
            .checkType(checkType != null ? checkType : "MANUAL")
            .clientName(clientName)
            .opposingParty(opposingParty)
            .status(ConflictStatus.PENDING)
            .applicantId(SecurityUtils.getUserId())
            .remark(remark)
            .build();

    conflictCheckRepository.save(check);

    // 3. 创建检查项
    List<ConflictCheckItem> items = new ArrayList<>();
    boolean hasConflict = false;

    // ===== 核心利冲逻辑 =====
    // 规则1：检查【对方当事人】是否是我们的现有客户（最重要！）
    //        如果对方当事人是我们客户，则存在利益冲突
    ConflictCheckItem opposingItem =
        ConflictCheckItem.builder()
            .checkId(check.getId())
            .partyName(opposingParty)
            .partyType("OPPOSING")
            .hasConflict(false)
            .build();
    ConflictResult opposingResult = checkOpposingPartyConflict(opposingParty);
    if (opposingResult.isHasConflict()) {
      opposingItem.setHasConflict(true);
      opposingItem.setConflictDetail(opposingResult.getDetail());
      opposingItem.setRelatedMatterId(opposingResult.getRelatedMatterId());
      opposingItem.setRelatedClientId(opposingResult.getRelatedClientId());
      hasConflict = true;
    }
    conflictCheckItemMapper.insert(opposingItem);
    items.add(opposingItem);

    // 规则2：检查【客户】是否曾作为其他案件的对方当事人
    //        如果我们之前代理别人告过这个客户，可能存在冲突
    ConflictCheckItem clientItem =
        ConflictCheckItem.builder()
            .checkId(check.getId())
            .partyName(clientName)
            .partyType("CLIENT")
            .hasConflict(false)
            .build();
    ConflictResult clientResult = checkClientConflict(clientName);
    if (clientResult.isHasConflict()) {
      clientItem.setHasConflict(true);
      clientItem.setConflictDetail(clientResult.getDetail());
      clientItem.setRelatedMatterId(clientResult.getRelatedMatterId());
      clientItem.setRelatedClientId(clientResult.getRelatedClientId());
      hasConflict = true;
    }
    conflictCheckItemMapper.insert(clientItem);
    items.add(clientItem);

    // 4. 更新检查状态
    check.setStatus(hasConflict ? ConflictStatus.CONFLICT : ConflictStatus.PASSED);
    conflictCheckRepository.updateById(check);

    // 5. 如果检测到冲突，创建审批记录
    if (hasConflict) {
      Long approverId = approverService.findConflictCheckApprover();
      if (approverId == null) {
        approverId = approverService.findDefaultApprover();
      }

      approvalService.createApproval(
          "CONFLICT_CHECK",
          check.getId(),
          check.getCheckNo(),
          "利冲检查：" + check.getClientName() + " vs " + opposingParty,
          approverId,
          "HIGH",
          "URGENT",
          null);

      log.info("利冲检查发现冲突，已创建审批记录: {} (审批人: {})", check.getCheckNo(), approverId);
    }

    log.info("利冲检查完成: {} ({}), 结果: {}", check.getCheckNo(), hasConflict ? "存在冲突" : "无冲突");

    ConflictCheckDTO dto = toDTO(check);
    dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
    return dto;
  }

  /**
   * 【核心利冲规则1】检查对方当事人是否是我们的现有客户
   *
   * <p>如果对方当事人是我们的客户，则存在利益冲突！ 例如：张三委托我们告李四，但李四也是我们的客户 → 冲突
   *
   * @param opposingParty 对方当事人
   * @return 冲突检查结果
   */
  private ConflictResult checkOpposingPartyConflict(final String opposingParty) {
    ConflictResult result = new ConflictResult();

    // 名称太短时不进行模糊匹配，避免误报
    if (opposingParty == null || opposingParty.trim().length() < 2) {
      return result;
    }

    String trimmedName = opposingParty.trim();

    // 核心检查：对方当事人是否是我们的现有客户？
    // 使用精确匹配优先，模糊匹配仅用于较长名称（>=4字符）
    List<Client> clients;
    if (trimmedName.length() >= 4) {
      clients =
          clientRepository.list(
              new LambdaQueryWrapper<Client>().like(Client::getName, trimmedName));
    } else {
      // 短名称使用精确匹配
      clients =
          clientRepository.list(new LambdaQueryWrapper<Client>().eq(Client::getName, trimmedName));
    }

    if (!clients.isEmpty()) {
      Client client = clients.get(0);
      result.setHasConflict(true);
      result.setDetail("【利益冲突】对方当事人「" + opposingParty + "」是本所现有客户，不能同时代理双方");
      result.setRelatedClientId(client.getId());
      return result;
    }

    // 次要检查：对方当事人是否曾作为我方客户出现在其他案件中
    // 安全实现：先查询匹配的客户ID，再查询关联的案件（避免SQL注入）
    List<Long> matchingClientIds;
    if (trimmedName.length() >= 4) {
      matchingClientIds =
          clientRepository
              .list(new LambdaQueryWrapper<Client>().like(Client::getName, trimmedName))
              .stream()
              .map(Client::getId)
              .collect(Collectors.toList());
    } else {
      matchingClientIds =
          clientRepository
              .list(new LambdaQueryWrapper<Client>().eq(Client::getName, trimmedName))
              .stream()
              .map(Client::getId)
              .collect(Collectors.toList());
    }

    if (!matchingClientIds.isEmpty()) {
      List<Matter> mattersAsClient =
          matterRepository.list(
              new LambdaQueryWrapper<Matter>().in(Matter::getClientId, matchingClientIds));
      if (!mattersAsClient.isEmpty()) {
        Matter relatedMatter = mattersAsClient.get(0);
        result.setHasConflict(true);
        result.setDetail(
            "【潜在冲突】对方当事人「" + opposingParty + "」曾作为委托人出现在案件「" + relatedMatter.getName() + "」中");
        result.setRelatedMatterId(relatedMatter.getId());
      }
    }

    return result;
  }

  /**
   * 【核心利冲规则2】检查客户是否曾作为其他案件的对方当事人
   *
   * <p>如果我们之前代理别人告过这个客户，可能存在冲突 例如：我们之前代理王五告过张三，现在张三要委托我们 → 潜在冲突
   *
   * @param clientName 客户名称
   * @return 冲突检查结果
   */
  private ConflictResult checkClientConflict(final String clientName) {
    ConflictResult result = new ConflictResult();

    // 名称太短时不进行模糊匹配，避免误报
    if (clientName == null || clientName.trim().length() < 2) {
      return result;
    }

    String trimmedName = clientName.trim();

    // 检查客户名称是否曾作为其他案件的对方当事人
    // 使用精确匹配优先，模糊匹配仅用于较长名称（>=4字符）
    List<Matter> mattersAsOpposing;
    if (trimmedName.length() >= 4) {
      mattersAsOpposing =
          matterRepository.list(
              new LambdaQueryWrapper<Matter>().like(Matter::getOpposingParty, trimmedName));
    } else {
      // 短名称使用精确匹配
      mattersAsOpposing =
          matterRepository.list(
              new LambdaQueryWrapper<Matter>().eq(Matter::getOpposingParty, trimmedName));
    }

    if (!mattersAsOpposing.isEmpty()) {
      Matter matter = mattersAsOpposing.get(0);
      result.setHasConflict(true);
      result.setDetail("【潜在冲突】客户「" + clientName + "」曾在案件「" + matter.getName() + "」中作为对方当事人");
      result.setRelatedMatterId(matter.getId());
    }

    return result;
  }

  /**
   * 获取利冲检查详情
   *
   * @param id 利冲检查ID
   * @return 利冲检查DTO
   */
  public ConflictCheckDTO getConflictCheckById(final Long id) {
    ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");
    ConflictCheckDTO dto = toDTO(check);

    // 加载检查项
    List<ConflictCheckItem> items = conflictCheckItemMapper.selectByCheckId(id);
    dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));

    return dto;
  }

  /**
   * 审核通过
   *
   * @param id 利冲检查ID
   * @param comment 审核意见
   */
  @Transactional
  public void approve(final Long id, final String comment) {
    ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");

    if (!ConflictStatus.canApplyExemption(check.getStatus())) {
      throw new BusinessException("当前状态不允许审核");
    }

    check.setStatus(ConflictStatus.WAIVED);
    check.setReviewerId(SecurityUtils.getUserId());
    check.setReviewedAt(LocalDateTime.now());
    check.setReviewComment(comment);
    conflictCheckRepository.updateById(check);

    // 更新案件冲突状态
    if (check.getMatterId() != null) {
      Matter matter = matterRepository.findById(check.getMatterId());
      if (matter != null) {
        matter.setConflictStatus(ConflictStatus.WAIVED);
        matterRepository.updateById(matter);
      }
    }

    log.info("利冲检查审核通过: {}", check.getCheckNo());
  }

  /** 申请利益冲突豁免. */
  /**
   * 申请豁免
   *
   * @param command 申请豁免命令
   * @return 利冲检查DTO
   */
  @Transactional
  public ConflictCheckDTO applyExemption(
      final com.lawfirm.application.client.command.ApplyExemptionCommand command) {
    ConflictCheck check =
        conflictCheckRepository.getByIdOrThrow(command.getConflictCheckId(), "利冲检查不存在");

    // 只有发现冲突的检查才能申请豁免
    if (!ConflictStatus.hasConflict(check.getStatus())) {
      throw new BusinessException("只有存在冲突的利冲检查才能申请豁免");
    }

    // 更新检查记录，添加豁免申请信息
    check.setStatus(ConflictStatus.EXEMPTION_PENDING); // 豁免待审批
    check.setReviewComment(command.getExemptionReason());
    check.setRemark(command.getExemptionDescription());
    conflictCheckRepository.updateById(check);

    // 触发审批流程
    Long approverId = approverService.findDefaultApprover();
    if (approverId == null) {
      throw new BusinessException("未找到合适的审批人，请联系管理员配置审批流程");
    }

    try {
      String businessSnapshot =
          new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(check);
      approvalService.createApproval(
          "CONFLICT_EXEMPTION",
          check.getId(),
          check.getCheckNo(),
          "利益冲突豁免申请: " + check.getCheckNo() + " - " + command.getExemptionReason(),
          approverId,
          "HIGH", // 高优先级
          "URGENT", // 紧急
          businessSnapshot);
    } catch (Exception e) {
      log.error("创建豁免审批失败", e);
      throw new BusinessException("创建豁免审批失败");
    }

    log.info("申请利益冲突豁免: checkNo={}, reason={}", check.getCheckNo(), command.getExemptionReason());
    return getConflictCheckById(check.getId());
  }

  /**
   * 审批豁免申请（通过）
   *
   * @param id 利冲检查ID
   * @param comment 审批意见
   */
  @Transactional
  public void approveExemption(final Long id, final String comment) {
    ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");

    if (!ConflictStatus.isExemptionPending(check.getStatus())) {
      throw new BusinessException("当前状态不允许审批豁免");
    }

    check.setStatus(ConflictStatus.WAIVED);
    check.setReviewerId(SecurityUtils.getUserId());
    check.setReviewedAt(LocalDateTime.now());
    if (StringUtils.hasText(comment)) {
      check.setReviewComment(check.getReviewComment() + "\n审批意见: " + comment);
    }
    conflictCheckRepository.updateById(check);

    // 更新案件冲突状态
    if (check.getMatterId() != null) {
      Matter matter = matterRepository.findById(check.getMatterId());
      if (matter != null) {
        matter.setConflictStatus(ConflictStatus.WAIVED);
        matterRepository.updateById(matter);
      }
    }

    log.info("利益冲突豁免审批通过: {}", check.getCheckNo());
  }

  /**
   * 审批豁免申请（拒绝）
   *
   * @param id 利冲检查ID
   * @param comment 拒绝原因
   */
  @Transactional
  public void rejectExemption(final Long id, final String comment) {
    ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");

    if (!ConflictStatus.isExemptionPending(check.getStatus())) {
      throw new BusinessException("当前状态不允许审批豁免");
    }

    // 拒绝后恢复为冲突状态
    check.setStatus(ConflictStatus.CONFLICT);
    check.setReviewerId(SecurityUtils.getUserId());
    check.setReviewedAt(LocalDateTime.now());
    if (StringUtils.hasText(comment)) {
      check.setReviewComment(check.getReviewComment() + "\n拒绝原因: " + comment);
    }
    conflictCheckRepository.updateById(check);

    // 更新案件冲突状态
    if (check.getMatterId() != null) {
      Matter matter = matterRepository.findById(check.getMatterId());
      if (matter != null) {
        matter.setConflictStatus(ConflictStatus.CONFLICT);
        matterRepository.updateById(matter);
      }
    }

    log.info("利益冲突豁免审批拒绝: {}", check.getCheckNo());
  }

  /**
   * 审核拒绝
   *
   * @param id 利冲检查ID
   * @param comment 拒绝原因
   */
  @Transactional
  public void reject(final Long id, final String comment) {
    ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");

    if (!ConflictStatus.canApplyExemption(check.getStatus())) {
      throw new BusinessException("当前状态不允许审核");
    }

    check.setStatus(ConflictStatus.REJECTED);
    check.setReviewerId(SecurityUtils.getUserId());
    check.setReviewedAt(LocalDateTime.now());
    check.setReviewComment(comment);
    conflictCheckRepository.updateById(check);

    // 更新案件冲突状态
    if (check.getMatterId() != null) {
      Matter matter = matterRepository.findById(check.getMatterId());
      if (matter != null) {
        matter.setConflictStatus(ConflictStatus.REJECTED);
        matterRepository.updateById(matter);
      }
    }

    log.info("利冲检查审核拒绝: {}", check.getCheckNo());
  }

  /**
   * 执行冲突检查
   *
   * @param partyName 当事人名称
   * @param idNumber 身份证号/统一社会信用代码
   * @param excludeMatterId 排除的案件ID
   * @return 冲突检查结果
   */
  private ConflictResult checkConflict(
      final String partyName, final String idNumber, final Long excludeMatterId) {
    ConflictResult result = new ConflictResult();

    // 1. 检查是否为现有客户的对方当事人
    List<Matter> matters =
        matterRepository.list(
            new LambdaQueryWrapper<Matter>()
                .ne(Matter::getId, excludeMatterId)
                .and(
                    w ->
                        w.like(Matter::getOpposingParty, partyName)
                            .or()
                            .like(Matter::getName, partyName)));

    if (!matters.isEmpty()) {
      result.hasConflict = true;
      result.detail = "该当事人与现有案件存在关联";
      result.relatedMatterId = matters.get(0).getId();
    }

    // 2. 检查是否为现有客户
    if (StringUtils.hasText(idNumber)) {
      List<Client> clients =
          clientRepository.list(
              new LambdaQueryWrapper<Client>()
                  .eq(Client::getCreditCode, idNumber)
                  .or()
                  .eq(Client::getIdCard, idNumber));

      if (!clients.isEmpty()) {
        // 检查该客户是否有对立案件
        Client client = clients.get(0);
        List<Matter> clientMatters =
            matterRepository.list(
                new LambdaQueryWrapper<Matter>()
                    .eq(Matter::getClientId, client.getId())
                    .ne(Matter::getId, excludeMatterId));

        if (!clientMatters.isEmpty()) {
          result.hasConflict = true;
          result.detail = "该当事人为现有客户，存在潜在利益冲突";
          result.relatedClientId = client.getId();
          result.relatedMatterId = clientMatters.get(0).getId();
        }
      }
    }

    return result;
  }

  /**
   * 生成检查编号
   *
   * @return 检查编号
   */
  private String generateCheckNo() {
    String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    return "CC" + System.currentTimeMillis() % RANDOM_BOUND + random;
  }

  /**
   * 获取状态名称
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    return ConflictStatus.getStatusName(status);
  }

  /**
   * 获取检查类型名称
   *
   * @param type 检查类型
   * @return 检查类型名称
   */
  private String getCheckTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "NEW_CLIENT" -> "新客户";
      case "NEW_MATTER" -> "新案件";
      case "MANUAL" -> "手动检查";
      default -> type;
    };
  }

  /**
   * 获取当事人类型名称
   *
   * @param type 当事人类型
   * @return 当事人类型名称
   */
  private String getPartyTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "CLIENT" -> "委托人";
      case "OPPOSING" -> "对方当事人";
      case "RELATED" -> "关联方";
      default -> type;
    };
  }

  /**
   * Entity 转 DTO（带预加载数据，避免N+1查询）
   *
   * @param check 利冲检查实体
   * @param userMap 用户映射
   * @param matterMap 案件映射
   * @return 利冲检查DTO
   */
  private ConflictCheckDTO toDTO(
      final ConflictCheck check,
      final java.util.Map<Long, User> userMap,
      final java.util.Map<Long, Matter> matterMap) {
    ConflictCheckDTO dto = new ConflictCheckDTO();
    dto.setId(check.getId());
    dto.setCheckNo(check.getCheckNo());
    dto.setMatterId(check.getMatterId());
    dto.setClientId(check.getClientId());
    dto.setClientName(check.getClientName());
    dto.setOpposingParty(check.getOpposingParty());
    dto.setCheckType(check.getCheckType());
    dto.setCheckTypeName(getCheckTypeName(check.getCheckType()));
    dto.setStatus(check.getStatus());
    dto.setStatusName(getStatusName(check.getStatus()));
    dto.setApplicantId(check.getApplicantId());
    dto.setApplyTime(check.getCreatedAt()); // 申请时间 = 创建时间
    dto.setReviewerId(check.getReviewerId());
    dto.setReviewTime(check.getReviewedAt());
    dto.setReviewComment(check.getReviewComment());
    dto.setRemark(check.getRemark());
    dto.setCreatedAt(check.getCreatedAt());
    dto.setUpdatedAt(check.getUpdatedAt());

    // 从预加载Map获取申请人姓名（避免N+1）
    if (check.getApplicantId() != null) {
      User applicant = userMap.get(check.getApplicantId());
      if (applicant != null) {
        dto.setApplicantName(applicant.getRealName());
      }
    }

    // 从预加载Map获取审核人姓名（避免N+1）
    if (check.getReviewerId() != null) {
      User reviewer = userMap.get(check.getReviewerId());
      if (reviewer != null) {
        dto.setReviewerName(reviewer.getRealName());
      }
    }

    // 从预加载Map获取项目名称（避免N+1）
    if (check.getMatterId() != null) {
      Matter matter = matterMap.get(check.getMatterId());
      if (matter != null) {
        dto.setMatterName(matter.getName());
      }
    }

    return dto;
  }

  /**
   * Entity 转 DTO（单条查询用，会触发额外查询）
   *
   * @param check 利冲检查实体
   * @return 利冲检查DTO
   */
  private ConflictCheckDTO toDTO(final ConflictCheck check) {
    // 单条查询时构建Map
    java.util.Map<Long, User> userMap = new java.util.HashMap<>();
    java.util.Map<Long, Matter> matterMap = new java.util.HashMap<>();

    if (check.getApplicantId() != null) {
      User applicant = userMapper.selectById(check.getApplicantId());
      if (applicant != null) {
        userMap.put(applicant.getId(), applicant);
      }
    }
    if (check.getReviewerId() != null) {
      User reviewer = userMapper.selectById(check.getReviewerId());
      if (reviewer != null) {
        userMap.put(reviewer.getId(), reviewer);
      }
    }
    if (check.getMatterId() != null) {
      Matter matter = matterRepository.findById(check.getMatterId());
      if (matter != null) {
        matterMap.put(matter.getId(), matter);
      }
    }

    return toDTO(check, userMap, matterMap);
  }

  /**
   * Item Entity 转 DTO
   *
   * @param item 利冲检查项实体
   * @return 利冲检查项DTO
   */
  private ConflictCheckItemDTO toItemDTO(final ConflictCheckItem item) {
    ConflictCheckItemDTO dto = new ConflictCheckItemDTO();
    dto.setId(item.getId());
    dto.setCheckId(item.getCheckId());
    dto.setPartyName(item.getPartyName());
    dto.setPartyType(item.getPartyType());
    dto.setPartyTypeName(getPartyTypeName(item.getPartyType()));
    dto.setIdNumber(item.getIdNumber());
    dto.setHasConflict(item.getHasConflict());
    dto.setConflictDetail(item.getConflictDetail());
    dto.setRelatedMatterId(item.getRelatedMatterId());
    dto.setRelatedClientId(item.getRelatedClientId());
    return dto;
  }

  /** 冲突检查结果. */
  @lombok.Getter
  @lombok.Setter
  private static class ConflictResult {
    /** 是否存在冲突. */
    private boolean hasConflict = false;

    /** 冲突详情. */
    private String detail;

    /** 关联案件ID. */
    private Long relatedMatterId;

    /** 关联客户ID. */
    private Long relatedClientId;
  }
}
