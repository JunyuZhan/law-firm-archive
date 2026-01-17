package com.lawfirm.domain.ai.repository;

import com.lawfirm.domain.ai.entity.AiUserQuota;

import java.util.List;

/**
 * 用户AI配额仓储接口
 */
public interface AiUserQuotaRepository {

    /**
     * 保存用户配额
     */
    void save(AiUserQuota quota);

    /**
     * 更新用户配额
     */
    void update(AiUserQuota quota);

    /**
     * 保存或更新用户配额
     */
    void saveOrUpdate(AiUserQuota quota);

    /**
     * 根据ID查询
     */
    AiUserQuota findById(Long id);

    /**
     * 根据用户ID查询
     */
    AiUserQuota findByUserId(Long userId);

    /**
     * 查询所有用户配额
     */
    List<AiUserQuota> findAll();

    /**
     * 查询免计费用户
     */
    List<AiUserQuota> findExemptUsers();

    /**
     * 检查用户是否免计费
     */
    boolean isUserExempt(Long userId);

    /**
     * 删除用户配额（逻辑删除）
     */
    void deleteByUserId(Long userId);
}
