package com.lawfirm.infrastructure.cache.dto;

import lombok.Data;

/**
 * 缓存统计信息
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Data
public class CacheStats {

  /** 本地缓存统计（来自 Caffeine） */
  private String localCacheStats;

  /** 熔断器状态 */
  private String circuitBreakerState;

  /** 熔断器详细信息 */
  private String circuitBreakerInfo;

  /** 配置缓存数量 */
  private int configCacheCount;

  /** 菜单缓存数量 */
  private int menuCacheCount;

  /** 部门缓存数量 */
  private int deptCacheCount;

  /** 总缓存数量 */
  private int totalCacheCount;

  /** Redis 是否可用 */
  private boolean redisAvailable = true;
}
