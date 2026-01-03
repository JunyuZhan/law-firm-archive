package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.CommissionRule;
import com.lawfirm.infrastructure.persistence.mapper.CommissionRuleMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 提成规则 Repository
 */
@Repository
public class CommissionRuleRepository extends AbstractRepository<CommissionRuleMapper, CommissionRule> {

    /**
     * 查询默认规则
     */
    public Optional<CommissionRule> findDefaultRule() {
        CommissionRule rule = baseMapper.selectDefaultRule();
        return Optional.ofNullable(rule);
    }

    /**
     * 查询适用的规则
     */
    public Optional<CommissionRule> findApplicableRule(String businessType, BigDecimal amount) {
        CommissionRule rule = baseMapper.selectApplicableRule(businessType, amount);
        return Optional.ofNullable(rule);
    }

    /**
     * 查询所有启用的规则
     */
    public List<CommissionRule> findActiveRules() {
        return baseMapper.selectActiveRules();
    }

    /**
     * 根据规则编码查询
     */
    public Optional<CommissionRule> findByRuleCode(String ruleCode) {
        CommissionRule rule = baseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommissionRule>()
                        .eq(CommissionRule::getRuleCode, ruleCode)
                        .eq(CommissionRule::getDeleted, false)
        );
        return Optional.ofNullable(rule);
    }
}

