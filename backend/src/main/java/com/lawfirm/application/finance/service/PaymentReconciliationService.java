package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.dto.MatchCandidateDTO;
import com.lawfirm.application.finance.dto.ReconciliationResultDTO;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.infrastructure.persistence.mapper.FeeMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 收款智能匹配服务
 *
 * <p>功能： 1. 根据OCR识别结果智能匹配待收款项目 2. 基于金额、付款方名称、日期等多维度计算匹配度 3. 返回匹配候选列表，支持自动核销高匹配度记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // 保留匹配阈值常量供算法调优使用
public class PaymentReconciliationService {

  /** 费用Mapper. */
  private final FeeMapper feeMapper;

  /** 项目Mapper. */
  private final MatterMapper matterMapper;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /** 自动核销阈值. */
  private static final double AUTO_RECONCILE_THRESHOLD = 0.85;

  /** 推荐阈值. */
  private static final double RECOMMEND_THRESHOLD = 0.5;

  /** 金额容差 5%. */
  private static final double AMOUNT_TOLERANCE = 0.05;

  /** 匹配度阈值：自动核销 */
  private static final double MATCH_SCORE_AUTO_RECONCILE = 0.95;

  /** 匹配度阈值：差异10%以内 */
  private static final double MATCH_SCORE_DIFF_10_PERCENT = 0.8;

  /** 匹配度阈值：差异20%以内 */
  private static final double MATCH_SCORE_DIFF_20_PERCENT = 0.6;

  /** 匹配度阈值：差异50%以内 */
  private static final double MATCH_SCORE_DIFF_50_PERCENT = 0.3;

  /** 匹配度阈值：差异10% */
  private static final double DIFF_THRESHOLD_10_PERCENT = 0.1;

  /** 匹配度阈值：差异20% */
  private static final double DIFF_THRESHOLD_20_PERCENT = 0.2;

  /** 匹配度阈值：差异50% */
  private static final double DIFF_THRESHOLD_50_PERCENT = 0.5;

  /** 名称匹配度：完全匹配 */
  private static final double NAME_SCORE_EXACT = 1.0;

  /** 名称匹配度：包含关系 */
  private static final double NAME_SCORE_CONTAINS = 0.85;

  /** 名称匹配度：法定代表人匹配 */
  private static final double NAME_SCORE_LEGAL_REP = 0.7;

  /** 名称匹配度：联系人匹配 */
  private static final double NAME_SCORE_CONTACT = 0.6;

  /** 权重：金额. */
  private static final double WEIGHT_AMOUNT = 0.4;

  /** 权重：名称. */
  private static final double WEIGHT_NAME = 0.35;

  /** 权重：日期. */
  private static final double WEIGHT_DATE = 0.15;

  /** 权重：状态. */
  private static final double WEIGHT_STATUS = 0.1;

  /** 分数阈值：高. */
  private static final double SCORE_HIGH = 0.8;

  /** 分数阈值：中. */
  private static final double SCORE_MEDIUM = 0.5;

  /** 金额匹配分数：容差范围内. */
  private static final double AMOUNT_SCORE_TOLERANCE = 0.95;

  /** 金额匹配分数：差异10%以内. */
  private static final double AMOUNT_SCORE_10_PERCENT = 0.8;

  /** 金额匹配分数：差异20%以内. */
  private static final double AMOUNT_SCORE_20_PERCENT = 0.6;

  /** 金额匹配分数：差异50%以内. */
  private static final double AMOUNT_SCORE_50_PERCENT = 0.3;

  /** 日期匹配分数：3天内. */
  private static final double DATE_SCORE_3_DAYS = 0.9;

  /** 日期匹配分数：7天内. */
  private static final double DATE_SCORE_7_DAYS = 0.7;

  /** 日期匹配分数：30天内. */
  private static final double DATE_SCORE_30_DAYS = 0.4;

  /** 日期匹配分数：超过30天. */
  private static final double DATE_SCORE_OVER_30 = 0.1;

  /** 日期差异阈值：3天. */
  private static final int DAYS_DIFF_3 = 3;

  /** 日期差异阈值：7天. */
  private static final int DAYS_DIFF_7 = 7;

  /** 日期差异阈值：30天. */
  private static final int DAYS_DIFF_30 = 30;

  /** 比例阈值：10%. */
  private static final double RATIO_10_PERCENT = 0.1;

  /** 比例阈值：20%. */
  private static final double RATIO_20_PERCENT = 0.2;

  /** 比例阈值：50%. */
  private static final double RATIO_50_PERCENT = 0.5;

  /**
   * 智能匹配待收款记录
   *
   * @param amount 收款金额
   * @param payerName 付款方名称
   * @param transactionNo 交易流水号（可选）
   * @param paymentDate 收款日期（可选）
   * @return 匹配结果
   */
  public ReconciliationResultDTO intelligentMatch(
      final BigDecimal amount,
      final String payerName,
      final String transactionNo,
      final java.time.LocalDate paymentDate) {
    log.info("开始智能匹配: amount={}, payerName={}, transactionNo={}", amount, payerName, transactionNo);

    List<MatchCandidateDTO> candidates = new ArrayList<>();

    // 1. 查询所有待收款记录
    List<Fee> pendingFees = feeMapper.selectPendingFees();

    if (pendingFees.isEmpty()) {
      log.info("没有待收款记录");
      return ReconciliationResultDTO.builder()
          .candidates(Collections.emptyList())
          .hasRecommended(false)
          .build();
    }

    // 2. 对每条待收款记录计算匹配度
    for (Fee fee : pendingFees) {
      MatchCandidateDTO candidate =
          calculateMatchScore(fee, amount, payerName, transactionNo, paymentDate);
      if (candidate.getScore() > 0) {
        candidates.add(candidate);
      }
    }

    // 3. 按匹配度排序
    candidates.sort(Comparator.comparing(MatchCandidateDTO::getScore).reversed());

    // 4. 确定推荐记录
    MatchCandidateDTO recommended = null;
    boolean canAutoReconcile = false;

    if (!candidates.isEmpty()) {
      MatchCandidateDTO top = candidates.get(0);
      if (top.getScore() >= RECOMMEND_THRESHOLD) {
        recommended = top;
        canAutoReconcile = top.getScore() >= AUTO_RECONCILE_THRESHOLD;
      }
    }

    log.info(
        "智能匹配完成: 候选数={}, 推荐={}, 可自动核销={}",
        candidates.size(),
        recommended != null ? recommended.getFeeNo() : "无",
        canAutoReconcile);

    return ReconciliationResultDTO.builder()
        .candidates(candidates.stream().limit(10).collect(Collectors.toList())) // 最多返回10条
        .recommended(recommended)
        .hasRecommended(recommended != null)
        .canAutoReconcile(canAutoReconcile)
        .build();
  }

  /**
   * 计算单条收费记录的匹配度
   *
   * @param fee 收费记录
   * @param amount 金额
   * @param payerName 付款人名称
   * @param transactionNo 交易号
   * @param paymentDate 付款日期
   * @return 匹配候选DTO
   */
  private MatchCandidateDTO calculateMatchScore(
      final Fee fee,
      final BigDecimal amount,
      final String payerName,
      final String transactionNo,
      final java.time.LocalDate paymentDate) {
    double totalScore = 0;
    List<String> matchReasons = new ArrayList<>();

    // 获取关联信息
    Client client = fee.getClientId() != null ? clientRepository.findById(fee.getClientId()) : null;
    Matter matter = fee.getMatterId() != null ? matterMapper.selectById(fee.getMatterId()) : null;

    // 1. 金额匹配（权重 40%）
    if (amount != null && fee.getAmount() != null) {
      BigDecimal unpaid =
          fee.getAmount()
              .subtract(fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO);
      double amountScore = calculateAmountScore(amount, unpaid);
      totalScore += amountScore * WEIGHT_AMOUNT;

      if (amountScore > SCORE_HIGH) {
        matchReasons.add("金额高度匹配");
      } else if (amountScore > SCORE_MEDIUM) {
        matchReasons.add("金额部分匹配");
      }
    }

    // 2. 付款方名称匹配（权重 35%）
    if (StringUtils.hasText(payerName) && client != null) {
      double nameScore = calculateNameScore(payerName, client);
      totalScore += nameScore * WEIGHT_NAME;

      if (nameScore > SCORE_HIGH) {
        matchReasons.add("付款方名称高度匹配");
      } else if (nameScore > SCORE_MEDIUM) {
        matchReasons.add("付款方名称部分匹配");
      }
    }

    // 3. 日期匹配（权重 15%）
    if (paymentDate != null && fee.getPlannedDate() != null) {
      double dateScore = calculateDateScore(paymentDate, fee.getPlannedDate());
      totalScore += dateScore * WEIGHT_DATE;

      if (dateScore > SCORE_HIGH) {
        matchReasons.add("收款日期接近计划日期");
      }
    }

    // 4. 项目状态加分（权重 10%）
    if (matter != null && "ACTIVE".equals(matter.getStatus())) {
      totalScore += WEIGHT_STATUS;
      matchReasons.add("项目进行中");
    }

    return MatchCandidateDTO.builder()
        .feeId(fee.getId())
        .feeNo(fee.getFeeNo())
        .feeName(fee.getFeeName())
        .matterId(fee.getMatterId())
        .matterName(matter != null ? matter.getName() : null)
        .matterNo(matter != null ? matter.getMatterNo() : null)
        .clientId(fee.getClientId())
        .clientName(client != null ? client.getName() : null)
        .expectedAmount(fee.getAmount())
        .unpaidAmount(
            fee.getAmount()
                .subtract(fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO))
        .plannedDate(fee.getPlannedDate())
        .score(Math.min(totalScore, 1.0)) // 最高1.0
        .matchReasons(matchReasons)
        .build();
  }

  /**
   * 计算金额匹配度
   *
   * @param actual 实际金额
   * @param expected 期望金额
   * @return 匹配度分数
   */
  private double calculateAmountScore(final BigDecimal actual, final BigDecimal expected) {
    if (expected == null || expected.compareTo(BigDecimal.ZERO) == 0) {
      return 0;
    }

    // 完全匹配
    if (actual.compareTo(expected) == 0) {
      return 1.0;
    }

    // 计算差异比例
    BigDecimal diff = actual.subtract(expected).abs();
    BigDecimal ratio = diff.divide(expected, 4, RoundingMode.HALF_UP);

    // 在容差范围内
    if (ratio.doubleValue() <= AMOUNT_TOLERANCE) {
      return MATCH_SCORE_AUTO_RECONCILE;
    }

    // 差异在10%以内
    if (ratio.doubleValue() <= DIFF_THRESHOLD_10_PERCENT) {
      return MATCH_SCORE_DIFF_10_PERCENT;
    }

    // 差异在20%以内
    if (ratio.doubleValue() <= DIFF_THRESHOLD_20_PERCENT) {
      return MATCH_SCORE_DIFF_20_PERCENT;
    }

    // 差异在50%以内（可能是分期付款）
    if (ratio.doubleValue() <= DIFF_THRESHOLD_50_PERCENT) {
      return MATCH_SCORE_DIFF_50_PERCENT;
    }

    return 0;
  }

  /**
   * 计算名称匹配度
   *
   * @param payerName 付款人名称
   * @param client 客户
   * @return 匹配度分数
   */
  private double calculateNameScore(final String payerName, final Client client) {
    if (client == null || !StringUtils.hasText(client.getName())) {
      return 0;
    }

    String clientName = client.getName().toLowerCase().trim();
    String payer = payerName.toLowerCase().trim();

    // 完全匹配
    if (clientName.equals(payer)) {
      return NAME_SCORE_EXACT;
    }

    // 包含关系
    if (clientName.contains(payer) || payer.contains(clientName)) {
      return NAME_SCORE_CONTAINS;
    }

    // 计算相似度（简单的字符重叠）
    double similarity = calculateStringSimilarity(clientName, payer);

    // 检查法定代表人名称
    if (StringUtils.hasText(client.getLegalRepresentative())) {
      String legalRep = client.getLegalRepresentative().toLowerCase().trim();
      if (payer.contains(legalRep) || legalRep.contains(payer)) {
        similarity = Math.max(similarity, NAME_SCORE_LEGAL_REP);
      }
    }

    // 检查联系人名称
    if (StringUtils.hasText(client.getContactPerson())) {
      String contact = client.getContactPerson().toLowerCase().trim();
      if (payer.contains(contact) || contact.contains(payer)) {
        similarity = Math.max(similarity, NAME_SCORE_CONTACT);
      }
    }

    return similarity;
  }

  /**
   * 计算字符串相似度（Jaccard相似度）
   *
   * @param s1 字符串1
   * @param s2 字符串2
   * @return 相似度分数
   */
  private double calculateStringSimilarity(final String s1, final String s2) {
    Set<Character> set1 = s1.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
    Set<Character> set2 = s2.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());

    Set<Character> intersection = new HashSet<>(set1);
    intersection.retainAll(set2);

    Set<Character> union = new HashSet<>(set1);
    union.addAll(set2);

    if (union.isEmpty()) {
      return 0;
    }

    return (double) intersection.size() / union.size();
  }

  /**
   * 计算日期匹配度
   *
   * @param actual 实际日期
   * @param planned 计划日期
   * @return 匹配度分数
   */
  private double calculateDateScore(
      final java.time.LocalDate actual, final java.time.LocalDate planned) {
    long daysDiff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(actual, planned));

    if (daysDiff == 0) {
      return 1.0;
    } else if (daysDiff <= DAYS_DIFF_3) {
      return DATE_SCORE_3_DAYS;
    } else if (daysDiff <= DAYS_DIFF_7) {
      return DATE_SCORE_7_DAYS;
    } else if (daysDiff <= DAYS_DIFF_30) {
      return DATE_SCORE_30_DAYS;
    } else {
      return DATE_SCORE_OVER_30;
    }
  }

  /**
   * 根据OCR识别结果进行智能匹配
   *
   * @param ocrAmount OCR识别的金额
   * @param ocrPayer OCR识别的付款方
   * @param ocrDate OCR识别的日期
   * @param ocrTransactionNo OCR识别的流水号
   * @return 匹配结果
   */
  public ReconciliationResultDTO matchFromOcr(
      final BigDecimal ocrAmount,
      final String ocrPayer,
      final java.time.LocalDate ocrDate,
      final String ocrTransactionNo) {
    return intelligentMatch(ocrAmount, ocrPayer, ocrTransactionNo, ocrDate);
  }
}
