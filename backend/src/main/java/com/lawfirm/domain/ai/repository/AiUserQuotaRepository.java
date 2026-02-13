package com.lawfirm.domain.ai.repository;

import com.lawfirm.domain.ai.entity.AiUserQuota;
import java.util.List;

/** 用户AI配额仓储接口 */
public interface AiUserQuotaRepository {

  /**
   * 保存用户配额
   *
   * @param quota 用户配额
   */
  void save(AiUserQuota quota);

  /**
   * 更新用户配额
   *
   * @param quota 用户配额
   */
  void update(AiUserQuota quota);

  /**
   * 保存或更新用户配额
   *
   * @param quota 用户配额
   */
  void saveOrUpdate(AiUserQuota quota);

  /**
   * 根据ID查询
   *
   * @param id 配额ID
   * @return 用户配额
   */
  AiUserQuota findById(Long id);

  /**
   * 根据用户ID查询
   *
   * @param userId 用户ID
   * @return 用户配额
   */
  AiUserQuota findByUserId(Long userId);

  /**
   * 查询所有用户配额
   *
   * @return 用户配额列表
   */
  List<AiUserQuota> findAll();

  /**
   * 查询免计费用户
   *
   * @return 免计费用户配额列表
   */
  List<AiUserQuota> findExemptUsers();

  /**
   * 检查用户是否免计费
   *
   * @param userId 用户ID
   * @return 是否免计费
   */
  boolean isUserExempt(Long userId);

  /**
   * 删除用户配额（逻辑删除）
   *
   * @param userId 用户ID
   */
  void deleteByUserId(Long userId);
}
