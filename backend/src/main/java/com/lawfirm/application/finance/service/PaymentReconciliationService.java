package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.dto.MatchCandidateDTO;
import com.lawfirm.application.finance.dto.ReconciliationResultDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import com.lawfirm.infrastructure.persistence.mapper.FeeMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收款智能匹配服务
 * 
 * 功能：
 * 1. 根据OCR识别结果智能匹配待收款项目
 * 2. 基于金额、付款方名称、日期等多维度计算匹配度
 * 3. 返回匹配候选列表，支持自动核销高匹配度记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final FeeRepository feeRepository;
    private final FeeMapper feeMapper;
    private final MatterMapper matterMapper;
    private final ClientMapper clientMapper;
    private final ClientRepository clientRepository;

    // 匹配阈值配置
    private static final double AUTO_RECONCILE_THRESHOLD = 0.85;  // 自动核销阈值
    private static final double RECOMMEND_THRESHOLD = 0.5;        // 推荐阈值
    private static final double AMOUNT_TOLERANCE = 0.05;          // 金额容差 5%

    /**
     * 智能匹配待收款记录
     * 
     * @param amount 收款金额
     * @param payerName 付款方名称
     * @param transactionNo 交易流水号（可选）
     * @param paymentDate 收款日期（可选）
     * @return 匹配结果
     */
    public ReconciliationResultDTO intelligentMatch(BigDecimal amount, String payerName, 
                                                     String transactionNo, java.time.LocalDate paymentDate) {
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
            MatchCandidateDTO candidate = calculateMatchScore(fee, amount, payerName, transactionNo, paymentDate);
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
        
        log.info("智能匹配完成: 候选数={}, 推荐={}, 可自动核销={}", 
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
     */
    private MatchCandidateDTO calculateMatchScore(Fee fee, BigDecimal amount, String payerName,
                                                   String transactionNo, java.time.LocalDate paymentDate) {
        double totalScore = 0;
        List<String> matchReasons = new ArrayList<>();
        
        // 获取关联信息
        Client client = fee.getClientId() != null ? clientRepository.findById(fee.getClientId()) : null;
        Matter matter = fee.getMatterId() != null ? matterMapper.selectById(fee.getMatterId()) : null;
        
        // 1. 金额匹配（权重 40%）
        if (amount != null && fee.getAmount() != null) {
            BigDecimal unpaid = fee.getAmount().subtract(
                    fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO);
            double amountScore = calculateAmountScore(amount, unpaid);
            totalScore += amountScore * 0.4;
            
            if (amountScore > 0.8) {
                matchReasons.add("金额高度匹配");
            } else if (amountScore > 0.5) {
                matchReasons.add("金额部分匹配");
            }
        }
        
        // 2. 付款方名称匹配（权重 35%）
        if (StringUtils.hasText(payerName) && client != null) {
            double nameScore = calculateNameScore(payerName, client);
            totalScore += nameScore * 0.35;
            
            if (nameScore > 0.8) {
                matchReasons.add("付款方名称高度匹配");
            } else if (nameScore > 0.5) {
                matchReasons.add("付款方名称部分匹配");
            }
        }
        
        // 3. 日期匹配（权重 15%）
        if (paymentDate != null && fee.getPlannedDate() != null) {
            double dateScore = calculateDateScore(paymentDate, fee.getPlannedDate());
            totalScore += dateScore * 0.15;
            
            if (dateScore > 0.8) {
                matchReasons.add("收款日期接近计划日期");
            }
        }
        
        // 4. 项目状态加分（权重 10%）
        if (matter != null && "ACTIVE".equals(matter.getStatus())) {
            totalScore += 0.1;
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
                .unpaidAmount(fee.getAmount().subtract(
                        fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO))
                .plannedDate(fee.getPlannedDate())
                .score(Math.min(totalScore, 1.0)) // 最高1.0
                .matchReasons(matchReasons)
                .build();
    }

    /**
     * 计算金额匹配度
     */
    private double calculateAmountScore(BigDecimal actual, BigDecimal expected) {
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
            return 0.95;
        }
        
        // 差异在10%以内
        if (ratio.doubleValue() <= 0.1) {
            return 0.8;
        }
        
        // 差异在20%以内
        if (ratio.doubleValue() <= 0.2) {
            return 0.6;
        }
        
        // 差异在50%以内（可能是分期付款）
        if (ratio.doubleValue() <= 0.5) {
            return 0.3;
        }
        
        return 0;
    }

    /**
     * 计算名称匹配度
     */
    private double calculateNameScore(String payerName, Client client) {
        if (client == null || !StringUtils.hasText(client.getName())) {
            return 0;
        }
        
        String clientName = client.getName().toLowerCase().trim();
        String payer = payerName.toLowerCase().trim();
        
        // 完全匹配
        if (clientName.equals(payer)) {
            return 1.0;
        }
        
        // 包含关系
        if (clientName.contains(payer) || payer.contains(clientName)) {
            return 0.85;
        }
        
        // 计算相似度（简单的字符重叠）
        double similarity = calculateStringSimilarity(clientName, payer);
        
        // 检查法定代表人名称
        if (StringUtils.hasText(client.getLegalRepresentative())) {
            String legalRep = client.getLegalRepresentative().toLowerCase().trim();
            if (payer.contains(legalRep) || legalRep.contains(payer)) {
                similarity = Math.max(similarity, 0.7);
            }
        }
        
        // 检查联系人名称
        if (StringUtils.hasText(client.getContactPerson())) {
            String contact = client.getContactPerson().toLowerCase().trim();
            if (payer.contains(contact) || contact.contains(payer)) {
                similarity = Math.max(similarity, 0.6);
            }
        }
        
        return similarity;
    }

    /**
     * 计算字符串相似度（Jaccard相似度）
     */
    private double calculateStringSimilarity(String s1, String s2) {
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
     */
    private double calculateDateScore(java.time.LocalDate actual, java.time.LocalDate planned) {
        long daysDiff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(actual, planned));
        
        if (daysDiff == 0) {
            return 1.0;
        } else if (daysDiff <= 3) {
            return 0.9;
        } else if (daysDiff <= 7) {
            return 0.7;
        } else if (daysDiff <= 30) {
            return 0.4;
        } else {
            return 0.1;
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
    public ReconciliationResultDTO matchFromOcr(BigDecimal ocrAmount, String ocrPayer, 
                                                 java.time.LocalDate ocrDate, String ocrTransactionNo) {
        return intelligentMatch(ocrAmount, ocrPayer, ocrTransactionNo, ocrDate);
    }
}
