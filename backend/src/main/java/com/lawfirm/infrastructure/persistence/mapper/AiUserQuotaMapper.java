package com.lawfirm.infrastructure.persistence.mapper;

import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 用户AI配额Mapper */
@Mapper
public interface AiUserQuotaMapper extends AiUserQuotaRepository {

  /**
   * 保存用户AI配额.
   *
   * @param quota 用户AI配额实体
   */
  @Override
  @Insert(
      """
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

  /**
   * 更新用户AI配额.
   *
   * @param quota 用户AI配额实体
   */
  @Override
  @Update(
      """
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

  /**
   * 保存或更新用户AI配额.
   *
   * @param quota 用户AI配额实体
   */
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

  /**
   * 根据ID查询用户AI配额.
   *
   * @param id 配额ID
   * @return 用户AI配额实体
   */
  @Override
  @Select("SELECT * FROM ai_user_quota WHERE id = #{id} AND deleted = FALSE")
  AiUserQuota findById(Long id);

  /**
   * 根据用户ID查询用户AI配额.
   *
   * @param userId 用户ID
   * @return 用户AI配额实体
   */
  @Override
  @Select("SELECT * FROM ai_user_quota WHERE user_id = #{userId} AND deleted = FALSE")
  AiUserQuota findByUserId(Long userId);

  /**
   * 查询所有用户AI配额.
   *
   * @return 用户AI配额列表
   */
  @Override
  @Select("SELECT * FROM ai_user_quota WHERE deleted = FALSE ORDER BY user_id")
  List<AiUserQuota> findAll();

  /**
   * 查询所有免费用户.
   *
   * @return 免费用户列表
   */
  @Override
  @Select("SELECT * FROM ai_user_quota WHERE exempt_billing = TRUE AND deleted = FALSE")
  List<AiUserQuota> findExemptUsers();

  /**
   * 检查用户是否免费.
   *
   * @param userId 用户ID
   * @return 是否免费
   */
  @Override
  @Select(
      """
        SELECT COALESCE(exempt_billing, FALSE)
        FROM ai_user_quota
        WHERE user_id = #{userId} AND deleted = FALSE
        """)
  boolean isUserExempt(Long userId);

  /**
   * 根据用户ID删除用户AI配额（逻辑删除）.
   *
   * @param userId 用户ID
   */
  @Override
  @Update(
      "UPDATE ai_user_quota SET deleted = TRUE, updated_at = CURRENT_TIMESTAMP WHERE user_id = #{userId}")
  void deleteByUserId(Long userId);

  /**
   * 更新用户当月使用量.
   *
   * @param userId 用户ID
   * @param tokens Token数量
   * @param cost 费用
   */
  @Update(
      """
        UPDATE ai_user_quota SET
            current_month_tokens = current_month_tokens + #{tokens},
            current_month_cost = current_month_cost + #{cost},
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = #{userId} AND deleted = FALSE
        """)
  void incrementUsage(
      @Param("userId") Long userId,
      @Param("tokens") int tokens,
      @Param("cost") java.math.BigDecimal cost);

  /**
   * 重置所有用户的月度使用量.
   *
   * @return 影响行数
   */
  @Update(
      """
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
