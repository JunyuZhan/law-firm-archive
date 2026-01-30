package com.lawfirm.interfaces.rest.system;

import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.infrastructure.cache.BusinessCacheService;
import com.lawfirm.infrastructure.cache.dto.CacheStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存管理控制器
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@Tag(name = "缓存管理", description = "缓存监控与管理接口")
public class CacheController {

  /** 业务缓存服务 */
  private final BusinessCacheService businessCacheService;

  /**
   * 获取缓存统计
   *
   * @return 缓存统计信息
   */
  @Operation(summary = "获取缓存统计")
  @GetMapping("/stats")
  @RequirePermission("sys:cache:view")
  public Result<CacheStats> getCacheStats() {
    return Result.success(businessCacheService.getCacheStats());
  }

  /**
   * 清除所有缓存
   *
   * @return 空结果
   */
  @Operation(summary = "清除所有缓存")
  @DeleteMapping("/all")
  @RequirePermission("sys:cache:clear")
  public Result<Void> clearAllCache() {
    log.warn("管理员清除所有业务缓存");
    businessCacheService.evictAll();
    return Result.success();
  }

  /**
   * 清除配置缓存
   *
   * @return 空结果
   */
  @Operation(summary = "清除配置缓存")
  @DeleteMapping("/config")
  @RequirePermission("sys:cache:clear")
  public Result<Void> clearConfigCache() {
    log.info("清除配置缓存");
    businessCacheService.evictAllConfigs();
    return Result.success();
  }

  /**
   * 清除菜单缓存
   *
   * @return 空结果
   */
  @Operation(summary = "清除菜单缓存")
  @DeleteMapping("/menu")
  @RequirePermission("sys:cache:clear")
  public Result<Void> clearMenuCache() {
    log.info("清除菜单缓存");
    businessCacheService.evictAllMenus();
    return Result.success();
  }

  /**
   * 清除部门缓存
   *
   * @return 空结果
   */
  @Operation(summary = "清除部门缓存")
  @DeleteMapping("/dept")
  @RequirePermission("sys:cache:clear")
  public Result<Void> clearDeptCache() {
    log.info("清除部门缓存");
    businessCacheService.evictAllDepartments();
    return Result.success();
  }

  /**
   * 清除指定配置缓存
   *
   * @param key 配置键
   * @return 空结果
   */
  @Operation(summary = "清除指定配置缓存")
  @DeleteMapping("/config/{key}")
  @RequirePermission("sys:cache:clear")
  public Result<Void> clearConfigCache(@PathVariable final String key) {
    log.info("清除指定配置缓存: {}", key);
    businessCacheService.evictConfig(key);
    return Result.success();
  }

  /**
   * 清除指定用户菜单缓存
   *
   * @param userId 用户ID
   * @return 空结果
   */
  @Operation(summary = "清除指定用户菜单缓存")
  @DeleteMapping("/menu/user/{userId}")
  @RequirePermission("sys:cache:clear")
  public Result<Void> clearUserMenuCache(@PathVariable final Long userId) {
    log.info("清除用户菜单缓存: userId={}", userId);
    businessCacheService.evictUserMenus(userId);
    return Result.success();
  }
}
