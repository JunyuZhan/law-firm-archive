package com.lawfirm.application.system.service;

import com.lawfirm.infrastructure.persistence.mapper.UserRoleMapper;
import com.lawfirm.infrastructure.security.UserDetailsServiceImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** 权限缓存管理服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

  /** RedisTemplate. */
  private final RedisTemplate<String, Object> redisTemplate;

  /** UserDetails Service. */
  private final UserDetailsServiceImpl userDetailsService;

  /** UserRole Mapper. */
  private final UserRoleMapper userRoleMapper;

  /** Token缓存前缀. */
  private static final String TOKEN_CACHE_PREFIX = "token:";

  /** 权限缓存前缀. */
  private static final String PERMISSION_CACHE_PREFIX = "user:permissions:";

  /**
   * 清除用户权限缓存 包括Token缓存、权限缓存和用户认证缓存
   *
   * @param userId 用户ID
   */
  public void clearUserPermissionCache(final Long userId) {
    if (userId == null) {
      return;
    }

    // 清除Token缓存
    String tokenCacheKey = TOKEN_CACHE_PREFIX + userId;
    redisTemplate.delete(tokenCacheKey);

    // 清除权限缓存
    String permissionCacheKey = PERMISSION_CACHE_PREFIX + userId;
    redisTemplate.delete(permissionCacheKey);

    // 清除用户认证缓存
    userDetailsService.clearUserAuthCacheByUserId(userId);

    log.info("已清除用户权限缓存: userId={}", userId);
  }

  /**
   * 批量清除权限缓存
   *
   * @param userIds 用户ID列表
   */
  public void batchClearPermissionCache(final List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return;
    }

    for (Long userId : userIds) {
      clearUserPermissionCache(userId);
    }

    log.info("批量清除权限缓存完成: userIds={}, count={}", userIds, userIds.size());
  }

  /** 清除所有权限缓存（谨慎使用）. */
  public void clearAllPermissionCache() {
    // 注意：这个方法会清除所有用户的Token和权限缓存，导致所有用户需要重新登录
    // 应该谨慎使用，通常只在系统维护时使用
    log.warn("清除所有权限缓存 - 这将导致所有用户需要重新登录");

    // 这里可以使用Redis的KEYS命令或SCAN命令来查找所有匹配的key
    // 但为了性能考虑，建议通过其他方式管理（如使用Redis的命名空间）
    // 暂时不实现，避免性能问题
  }

  /**
   * 清除角色相关的所有用户权限缓存 当角色权限变更时，清除所有拥有该角色的用户的缓存
   *
   * @param roleId 角色ID
   */
  public void clearRolePermissionCache(final Long roleId) {
    if (roleId == null) {
      return;
    }

    // 查询所有拥有该角色的用户ID
    List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);

    if (userIds == null || userIds.isEmpty()) {
      log.info("角色无关联用户，无需清除缓存: roleId={}", roleId);
      return;
    }

    // 批量清除这些用户的权限缓存
    batchClearPermissionCache(userIds);

    log.info("清除角色权限缓存完成: roleId={}, affectedUsers={}", roleId, userIds.size());
  }
}
