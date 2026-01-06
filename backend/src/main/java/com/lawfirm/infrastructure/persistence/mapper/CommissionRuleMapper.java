package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.CommissionRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 提成规则 Mapper
 */
@Mapper
public interface CommissionRuleMapper extends BaseMapper<CommissionRule> {

    /**
     * 查询默认规则
     */
    @Select("SELECT * FROM finance_commission_rule WHERE is_default = true AND active = true AND deleted = false LIMIT 1")
    CommissionRule selectDefaultRule();

    /**
     * 查询适用的规则（按业务类型匹配）
     * 注：金额范围的阶梯费率在 rate_tiers JSON 字段中处理
     */
    @Select("""
        SELECT * FROM finance_commission_rule
        WHERE active = true AND deleted = false
        AND (rule_type = #{businessType} OR rule_type IS NULL)
        AND (effective_date IS NULL OR effective_date <= CURRENT_DATE)
        AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE)
        ORDER BY is_default DESC, created_at DESC
        LIMIT 1
        """)
    CommissionRule selectApplicableRule(@Param("businessType") String businessType, @Param("amount") java.math.BigDecimal amount);

    /**
     * 查询所有启用的规则
     */
    @Select("SELECT * FROM finance_commission_rule WHERE active = true AND deleted = false ORDER BY is_default DESC, created_at DESC")
    List<CommissionRule> selectActiveRules();

    /**
     * 清除所有规则的默认状态
     */
    @Update("UPDATE finance_commission_rule SET is_default = false WHERE deleted = false")
    void clearDefault();

    /**
     * 设置某规则为默认
     */
    @Update("UPDATE finance_commission_rule SET is_default = true WHERE id = #{id} AND deleted = false")
    void setDefault(@Param("id") Long id);
}

