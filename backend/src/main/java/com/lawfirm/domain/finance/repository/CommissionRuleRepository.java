package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.CommissionRule;
import com.lawfirm.infrastructure.persistence.mapper.CommissionRuleMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 提成规则 Repository */
@Repository
public class CommissionRuleRepository
    extends AbstractRepository<CommissionRuleMapper, CommissionRule> {

  /**
   * 查询默认规则
   *
   * @return 默认规则，如果不存在则返回空
   */
  public Optional<CommissionRule> findDefaultRule() {
    CommissionRule rule = baseMapper.selectDefaultRule();
    return Optional.ofNullable(rule);
  }

  /**
   * 查询适用的规则
   *
   * @param businessType 业务类型
   * @param amount 金额
   * @return 适用的规则，如果不存在则返回空
   */
  public Optional<CommissionRule> findApplicableRule(
      final String businessType, final BigDecimal amount) {
    CommissionRule rule = baseMapper.selectApplicableRule(businessType, amount);
    return Optional.ofNullable(rule);
  }

  /**
   * 查询所有启用的规则
   *
   * @return 启用的规则列表
   */
  public List<CommissionRule> findActiveRules() {
    return baseMapper.selectActiveRules();
  }

  /**
   * 根据规则编码查询
   *
   * @param ruleCode 规则编码
   * @return 规则，如果不存在则返回空
   */
  public Optional<CommissionRule> findByRuleCode(final String ruleCode) {
    CommissionRule rule =
        baseMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommissionRule>()
                .eq(CommissionRule::getRuleCode, ruleCode)
                .eq(CommissionRule::getDeleted, false));
    return Optional.ofNullable(rule);
  }

  /** 清除所有规则的默认状态. */
  public void clearDefault() {
    baseMapper.clearDefault();
  }

  /**
   * 设置某规则为默认
   *
   * @param id 规则ID
   */
  public void setDefault(final Long id) {
    baseMapper.clearDefault();
    baseMapper.setDefault(id);
  }
}
