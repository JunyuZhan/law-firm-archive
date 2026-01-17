package com.lawfirm.infrastructure.persistence.mapper;

import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户AI配额Mapper
 */
@Mapper
public interface AiUserQuotaMapper extends AiUserQuotaRepository {

    @Override
    @Insert("""
        INSERT INTO ai_user_quota (
            user_id, monthly_token_quota, monthly_cost_quota,
            current_month_tokens, current_month_cost, quota_reset_date,
            custom_charge_ratio, exempt_billing, created_at, created_by
        ) VALUES (
            #{userId}, #{monthlyTokenQuota}, #{monthlyCostQuota},
            #{currentMonthTokens}, #{currentMonthCost}, #{quotaResetDate},
            #{customChargeRatio}, #{exemptBilling}, CURRENT_TIMESTAMP, #{createdBy}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(AiUserQuota quota);

    @Override
    @Update("""
        UPDATE ai_user_quota SET
            monthly_token_quota = #{monthlyTokenQuota},
            monthly_cost_quota = #{monthlyCostQuota},
            current_month_tokens = #{currentMonthTokens},
            current_month_cost = #{currentMonthCost},
            quota_reset_date = #{quotaResetDate},
            custom_charge_ratio = #{customChargeRatio},
            exempt_billing = #{exemptBilling},
            updated_at = CURRENT_TIMESTAMP,
            updated_by = #{updatedBy}
        WHERE id = #{id} AND deleted = FALSE
        """)
    void update(AiUserQuota quota);

    @Override
    default void saveOrUpdate(AiUserQuota quota) {
        if (quota.getId() != null) {
            update(quota);
        } else {
            AiUserQuota existing = findByUserId(quota.getUserId());
            if (existing != null) {
                quota.setId(existing.getId());
                update(quota);
            } else {
                save(quota);
            }
        }
    }

    @Override
    @Select("SELECT * FROM ai_user_quota WHERE id = #{id} AND deleted = FALSE")
    AiUserQuota findById(Long id);

    @Override
    @Select("SELECT * FROM ai_user_quota WHERE user_id = #{userId} AND deleted = FALSE")
    AiUserQuota findByUserId(Long userId);

    @Override
    @Select("SELECT * FROM ai_user_quota WHERE deleted = FALSE ORDER BY user_id")
    List<AiUserQuota> findAll();

    @Override
    @Select("SELECT * FROM ai_user_quota WHERE exempt_billing = TRUE AND deleted = FALSE")
    List<AiUserQuota> findExemptUsers();

    @Override
    @Select("""
        SELECT COALESCE(exempt_billing, FALSE) 
        FROM ai_user_quota 
        WHERE user_id = #{userId} AND deleted = FALSE
        """)
    boolean isUserExempt(Long userId);

    @Override
    @Update("UPDATE ai_user_quota SET deleted = TRUE, updated_at = CURRENT_TIMESTAMP WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 更新用户当月使用量
     */
    @Update("""
        UPDATE ai_user_quota SET
            current_month_tokens = current_month_tokens + #{tokens},
            current_month_cost = current_month_cost + #{cost},
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = #{userId} AND deleted = FALSE
        """)
    void incrementUsage(@Param("userId") Long userId, 
                        @Param("tokens") int tokens, 
                        @Param("cost") java.math.BigDecimal cost);

    /**
     * 重置所有用户的月度使用量
     */
    @Update("""
        UPDATE ai_user_quota SET
            current_month_tokens = 0,
            current_month_cost = 0,
            quota_reset_date = CURRENT_DATE,
            updated_at = CURRENT_TIMESTAMP
        WHERE deleted = FALSE
        AND (quota_reset_date IS NULL OR quota_reset_date < DATE_TRUNC('month', CURRENT_DATE))
        """)
    int resetAllMonthlyUsage();
}
