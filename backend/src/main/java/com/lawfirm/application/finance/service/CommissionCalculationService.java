package com.lawfirm.application.finance.service;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.domain.finance.entity.CommissionRule;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.finance.repository.CommissionRepository;
import com.lawfirm.domain.finance.repository.CommissionRuleRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.domain.matter.repository.MatterParticipantRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 提成计算服务
 * 
 * 核心业务逻辑：
 * 1. 三层分配模型：律所留存 -> 案源提成 -> 办案提成
 * 2. 薪酬模式过滤：授薪制律师不参与提成分配
 * 3. 分配比例归一化：排除授薪制后重新计算比例
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionCalculationService {

    private final PaymentRepository paymentRepository;
    private final MatterRepository matterRepository;
    private final MatterParticipantRepository matterParticipantRepository;
    private final CommissionRuleRepository commissionRuleRepository;
    private final CommissionRepository commissionRepository;
    private final UserRepository userRepository;

    /**
     * 计算提成（收款核销后自动触发）
     * 
     * @param paymentId 收款ID
     * @return 提成记录列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Commission> calculateCommission(Long paymentId) {
        log.info("开始计算提成，收款ID: {}", paymentId);

        // 1. 获取收款信息
        Payment payment = paymentRepository.findById(paymentId);
        if (payment == null) {
            throw new BusinessException("收款记录不存在");
        }

        if (!"CONFIRMED".equals(payment.getStatus())) {
            throw new BusinessException("只有已确认的收款才能计算提成");
        }

        // 2. 检查是否已计算过提成
        List<Commission> existingCommissions = commissionRepository.findByPaymentId(paymentId);
        if (!existingCommissions.isEmpty()) {
            log.warn("收款ID {} 已存在提成记录，将删除后重新计算", paymentId);
            commissionRepository.deleteByPaymentId(paymentId);
        }

        // 3. 获取项目信息
        Matter matter = matterRepository.findById(payment.getMatterId());
        if (matter == null) {
            throw new BusinessException("项目不存在");
        }

        // 4. 获取项目参与人
        List<MatterParticipant> participants = matterParticipantRepository.findByMatterId(matter.getId());
        if (participants.isEmpty()) {
            throw new BusinessException("项目没有参与人，无法计算提成");
        }

        // 5. 获取适用的提成规则
        CommissionRule rule = findApplicableRule(matter, payment.getAmount());

        // 6. 计算办案成本（暂时为0，后续可扩展）
        BigDecimal costAmount = BigDecimal.ZERO;

        // 7. 执行三层分配计算
        List<Commission> commissions = calculateThreeTierCommission(
                payment, matter, participants, rule, costAmount);

        // 8. 保存提成记录
        commissionRepository.saveBatch(commissions);

        log.info("提成计算完成，收款ID: {}, 生成 {} 条提成记录", paymentId, commissions.size());
        return commissions;
    }

    /**
     * 三层分配模型计算
     * 
     * 第一层：律所留存
     * 第二层：案源提成（占剩余部分）
     * 第三层：办案提成（剩余部分，按分配比例）
     */
    private List<Commission> calculateThreeTierCommission(
            Payment payment, Matter matter, List<MatterParticipant> participants,
            CommissionRule rule, BigDecimal costAmount) {

        BigDecimal grossAmount = payment.getAmount();
        List<Commission> commissions = new ArrayList<>();

        // ========== 第一层：律所留存 ==========
        BigDecimal firmRetention = grossAmount.multiply(rule.getFirmRetentionRate())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAfterFirm = grossAmount.subtract(firmRetention);

        log.info("第一层 - 律所留存: {} ({}%), 剩余: {}", 
                firmRetention, rule.getFirmRetentionRate().multiply(BigDecimal.valueOf(100)), remainingAfterFirm);

        // ========== 第二层：案源提成 ==========
        // 案源提成比例是占剩余部分的比例
        BigDecimal originatorTotal = remainingAfterFirm.multiply(rule.getOriginatorRate())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAfterOriginator = remainingAfterFirm.subtract(originatorTotal);

        log.info("第二层 - 案源提成总额: {} (占剩余{}%), 剩余: {}", 
                originatorTotal, rule.getOriginatorRate().multiply(BigDecimal.valueOf(100)), remainingAfterOriginator);

        // 分配案源提成给案源人
        List<MatterParticipant> originators = participants.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsOriginator()))
                .collect(Collectors.toList());

        if (!originators.isEmpty() && originatorTotal.compareTo(BigDecimal.ZERO) > 0) {
            // 过滤出有提成资格的案源人
            List<MatterParticipant> eligibleOriginators = filterCommissionEligible(originators);
            
            if (!eligibleOriginators.isEmpty()) {
                // 计算案源人分配比例（使用commissionRate作为分配比例）
                normalizeDistributionRatio(eligibleOriginators);
                
                for (MatterParticipant originator : eligibleOriginators) {
                    BigDecimal originatorRatio = getDistributionRatio(originator);
                    BigDecimal originatorAmount = originatorTotal.multiply(originatorRatio)
                            .setScale(2, RoundingMode.HALF_UP);
                    
                    Commission commission = createCommission(
                            payment, matter, originator, rule, costAmount,
                            originatorAmount, originatorRatio, "ORIGINATOR");
                    commissions.add(commission);
                    
                    log.info("案源人 {} 获得案源提成: {} (比例: {}%)", 
                            originator.getUserId(), originatorAmount, originatorRatio.multiply(BigDecimal.valueOf(100)));
                }
            }
        }

        // ========== 第三层：办案提成 ==========
        // 办案提成 = 剩余部分，按团队成员分配比例分配
        List<MatterParticipant> caseHandlers = participants.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsOriginator()) || 
                           (Boolean.TRUE.equals(p.getIsOriginator()) && 
                            !"LEAD".equals(p.getRole()) && !"CO_COUNSEL".equals(p.getRole())))
                .collect(Collectors.toList());

        // 如果案源人同时也是办案人员，需要从办案提成中排除（避免重复计算）
        // 这里简化处理：如果案源人不是主办或协办，则只拿案源提成；如果是，则只拿办案提成
        List<MatterParticipant> pureCaseHandlers = caseHandlers.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsOriginator()) || 
                           "LEAD".equals(p.getRole()) || "CO_COUNSEL".equals(p.getRole()))
                .collect(Collectors.toList());

        if (!pureCaseHandlers.isEmpty() && remainingAfterOriginator.compareTo(BigDecimal.ZERO) > 0) {
            // 过滤出有提成资格的办案人员
            List<MatterParticipant> eligibleHandlers = filterCommissionEligible(pureCaseHandlers);
            
            if (!eligibleHandlers.isEmpty()) {
                // 归一化分配比例
                normalizeDistributionRatio(eligibleHandlers);
                
                for (MatterParticipant handler : eligibleHandlers) {
                    BigDecimal handlerRatio = getDistributionRatio(handler);
                    BigDecimal handlerAmount = remainingAfterOriginator.multiply(handlerRatio)
                            .setScale(2, RoundingMode.HALF_UP);
                    
                    Commission commission = createCommission(
                            payment, matter, handler, rule, costAmount,
                            handlerAmount, handlerRatio, "CASE_HANDLER");
                    commissions.add(commission);
                    
                    log.info("办案人员 {} 获得办案提成: {} (比例: {}%)", 
                            handler.getUserId(), handlerAmount, handlerRatio.multiply(BigDecimal.valueOf(100)));
                }
            }
        }

        // ========== 为授薪制律师生成0提成记录 ==========
        for (MatterParticipant participant : participants) {
            if (!isCommissionEligible(participant.getUserId())) {
                Commission zeroCommission = createZeroCommission(payment, matter, participant, rule);
                commissions.add(zeroCommission);
            }
        }

        return commissions;
    }

    /**
     * 创建提成记录
     */
    private Commission createCommission(
            Payment payment, Matter matter, MatterParticipant participant,
            CommissionRule rule, BigDecimal costAmount,
            BigDecimal netAmount, BigDecimal distributionRatio, String type) {

        BigDecimal grossAmount = payment.getAmount();
        
        // 计算税费和管理费（按比例分配）
        BigDecimal taxAmount = grossAmount.multiply(rule.getTaxRate())
                .multiply(distributionRatio)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal managementFee = grossAmount.multiply(rule.getManagementFeeRate())
                .multiply(distributionRatio)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = costAmount.multiply(distributionRatio)
                .setScale(2, RoundingMode.HALF_UP);

        // 计算提成比例（根据阶梯规则，这里简化处理，使用固定比例）
        // TODO: 实现阶梯提成比例计算
        BigDecimal commissionRate = BigDecimal.valueOf(1.0); // 默认100%

        // 提成金额 = 净收入 × 提成比例
        BigDecimal commissionAmount = netAmount.multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        User user = userRepository.findById(participant.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return Commission.builder()
                .paymentId(payment.getId())
                .matterId(matter.getId())
                .userId(participant.getUserId())
                .ruleId(rule.getId())
                .grossAmount(grossAmount.multiply(distributionRatio).setScale(2, RoundingMode.HALF_UP))
                .taxAmount(taxAmount)
                .managementFee(managementFee)
                .costAmount(cost)
                .netAmount(netAmount)
                .distributionRatio(distributionRatio)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .compensationType(user.getCompensationType())
                .status("CALCULATED")
                .remark(type.equals("ORIGINATOR") ? "案源提成" : "办案提成")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 为授薪制律师创建0提成记录
     */
    private Commission createZeroCommission(
            Payment payment, Matter matter, MatterParticipant participant, CommissionRule rule) {

        User user = userRepository.findById(participant.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return Commission.builder()
                .paymentId(payment.getId())
                .matterId(matter.getId())
                .userId(participant.getUserId())
                .ruleId(rule.getId())
                .grossAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .managementFee(BigDecimal.ZERO)
                .costAmount(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .distributionRatio(BigDecimal.ZERO)
                .commissionRate(BigDecimal.ZERO)
                .commissionAmount(BigDecimal.ZERO)
                .compensationType("SALARIED")
                .status("SALARIED_EXEMPT")
                .remark("授薪制律师不参与项目提成分配")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 过滤出有提成资格的参与人（提成制或混合制）
     */
    private List<MatterParticipant> filterCommissionEligible(List<MatterParticipant> participants) {
        return participants.stream()
                .filter(p -> isCommissionEligible(p.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * 判断律师是否有提成资格
     */
    private boolean isCommissionEligible(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            return false;
        }
        return user.isCommissionEligible();
    }

    /**
     * 归一化分配比例（排除授薪制后，重新计算提成制律师的分配比例）
     */
    private void normalizeDistributionRatio(List<MatterParticipant> eligibleParticipants) {
        BigDecimal totalRatio = eligibleParticipants.stream()
                .map(this::getDistributionRatio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRatio.compareTo(BigDecimal.ZERO) > 0) {
            for (MatterParticipant p : eligibleParticipants) {
                BigDecimal originalRatio = getDistributionRatio(p);
                BigDecimal normalizedRatio = originalRatio
                        .divide(totalRatio, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)); // 转换为百分比
                // 临时存储归一化后的比例（使用反射或临时字段）
                // 这里简化处理，直接使用commissionRate字段存储归一化后的值
                p.setCommissionRate(normalizedRatio);
            }
        }
    }

    /**
     * 获取分配比例（从commissionRate字段获取，转换为小数）
     */
    private BigDecimal getDistributionRatio(MatterParticipant participant) {
        if (participant.getCommissionRate() != null) {
            // commissionRate存储的是百分比，需要转换为小数
            return participant.getCommissionRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 查找适用的提成规则
     */
    private CommissionRule findApplicableRule(Matter matter, BigDecimal amount) {
        // 优先查找按业务类型和金额范围匹配的规则
        CommissionRule rule = commissionRuleRepository
                .findApplicableRule(matter.getBusinessType(), amount)
                .orElse(null);

        // 如果没有匹配的规则，使用默认规则
        if (rule == null) {
            rule = commissionRuleRepository.findDefaultRule()
                    .orElseThrow(() -> new BusinessException("未找到适用的提成规则，请先配置默认规则"));
        }

        return rule;
    }
}

