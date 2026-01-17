package com.lawfirm.infrastructure.cache;

import com.lawfirm.infrastructure.cache.dto.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.function.Supplier;

/**
 * 业务缓存服务
 * 
 * 封装业务层缓存操作，提供类型安全的缓存 API。
 * 基于 CacheDegradationService 实现，支持 Redis + 本地缓存双层架构。
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessCacheService {

    private final CacheDegradationService cacheDegradationService;
    private final RedisTemplate<String, Object> redisTemplate;

    // ========== 缓存键前缀常量 ==========
    
    /** 系统配置缓存前缀 */
    public static final String PREFIX_CONFIG = "lawfirm:config:";
    
    /** 用户菜单缓存前缀 */
    public static final String PREFIX_MENU_USER = "lawfirm:menu:user:";
    
    /** 全部菜单缓存键 */
    public static final String KEY_MENU_ALL = "lawfirm:menu:all";
    
    /** 部门缓存前缀 */
    public static final String PREFIX_DEPT = "lawfirm:dept:";
    
    /** 部门树缓存键 */
    public static final String KEY_DEPT_TREE = "lawfirm:dept:tree";
    
    /** 字典项缓存前缀 */
    public static final String PREFIX_DICT_ITEMS = "lawfirm:dict:items:";

    // ========== TTL 常量（秒）==========
    
    /** 系统配置 TTL：30分钟 */
    public static final long TTL_CONFIG = 1800;
    
    /** 用户菜单 TTL：10分钟 */
    public static final long TTL_MENU = 600;
    
    /** 部门数据 TTL：30分钟 */
    public static final long TTL_DEPT = 1800;
    
    /** 字典数据 TTL：1小时（字典数据变化少） */
    public static final long TTL_DICT = 3600;

    // ========== 系统配置缓存 ==========

    /**
     * 获取系统配置（带缓存）
     * 
     * @param key 配置键
     * @param dbLoader 数据库加载器
     * @return 配置值
     */
    public String getConfig(String key, Supplier<String> dbLoader) {
        String cacheKey = PREFIX_CONFIG + key;
        log.debug("获取配置缓存: {}", cacheKey);
        return cacheDegradationService.getWithFallback(cacheKey, dbLoader, TTL_CONFIG);
    }

    /**
     * 清除指定配置缓存
     * 
     * @param key 配置键
     */
    public void evictConfig(String key) {
        String cacheKey = PREFIX_CONFIG + key;
        log.info("清除配置缓存: {}", cacheKey);
        cacheDegradationService.deleteWithFallback(cacheKey);
    }

    /**
     * 清除所有配置缓存
     */
    public void evictAllConfigs() {
        log.info("清除所有配置缓存");
        evictByPattern(PREFIX_CONFIG + "*");
        // 同时清除本地缓存（Redis 不可用时配置缓存在本地）
        cacheDegradationService.clearLocalCache();
    }

    // ========== 用户菜单缓存 ==========

    /**
     * 获取用户菜单（带缓存）
     * 
     * @param userId 用户ID
     * @param dbLoader 数据库加载器
     * @param <T> 菜单类型
     * @return 菜单列表
     */
    @SuppressWarnings("unchecked")
    public <T> T getUserMenus(Long userId, Supplier<T> dbLoader) {
        String cacheKey = PREFIX_MENU_USER + userId;
        log.debug("获取用户菜单缓存: {}", cacheKey);
        return cacheDegradationService.getWithFallback(cacheKey, dbLoader, TTL_MENU);
    }

    /**
     * 清除指定用户的菜单缓存
     * 
     * @param userId 用户ID
     */
    public void evictUserMenus(Long userId) {
        String cacheKey = PREFIX_MENU_USER + userId;
        log.info("清除用户菜单缓存: userId={}", userId);
        cacheDegradationService.deleteWithFallback(cacheKey);
    }

    /**
     * 清除所有用户菜单缓存
     */
    public void evictAllMenus() {
        log.info("清除所有菜单缓存");
        evictByPattern(PREFIX_MENU_USER + "*");
        cacheDegradationService.deleteWithFallback(KEY_MENU_ALL);
    }

    // ========== 部门缓存 ==========

    /**
     * 获取部门信息（带缓存）
     * 
     * @param id 部门ID
     * @param dbLoader 数据库加载器
     * @param <T> 部门类型
     * @return 部门信息
     */
    @SuppressWarnings("unchecked")
    public <T> T getDepartment(Long id, Supplier<T> dbLoader) {
        String cacheKey = PREFIX_DEPT + id;
        log.debug("获取部门缓存: {}", cacheKey);
        return cacheDegradationService.getWithFallback(cacheKey, dbLoader, TTL_DEPT);
    }

    /**
     * 获取部门树（带缓存）
     * 
     * @param dbLoader 数据库加载器
     * @param <T> 部门树类型
     * @return 部门树
     */
    @SuppressWarnings("unchecked")
    public <T> T getDepartmentTree(Supplier<T> dbLoader) {
        log.debug("获取部门树缓存");
        return cacheDegradationService.getWithFallback(KEY_DEPT_TREE, dbLoader, TTL_DEPT);
    }

    /**
     * 清除指定部门缓存
     * 
     * @param id 部门ID
     */
    public void evictDepartment(Long id) {
        String cacheKey = PREFIX_DEPT + id;
        log.info("清除部门缓存: id={}", id);
        cacheDegradationService.deleteWithFallback(cacheKey);
        // 同时清除部门树缓存
        cacheDegradationService.deleteWithFallback(KEY_DEPT_TREE);
    }

    /**
     * 清除所有部门缓存
     */
    public void evictAllDepartments() {
        log.info("清除所有部门缓存");
        evictByPattern(PREFIX_DEPT + "*");
        cacheDegradationService.deleteWithFallback(KEY_DEPT_TREE);
    }

    // ========== 字典缓存 ==========

    /**
     * 获取字典项（带缓存）
     * 问题498修复：字典查询添加缓存
     * 
     * @param code 字典编码
     * @param dbLoader 数据库加载器
     * @param <T> 字典项类型
     * @return 字典项列表
     */
    @SuppressWarnings("unchecked")
    public <T> T getDictItems(String code, Supplier<T> dbLoader) {
        String cacheKey = PREFIX_DICT_ITEMS + code;
        log.debug("获取字典项缓存: {}", cacheKey);
        return cacheDegradationService.getWithFallback(cacheKey, dbLoader, TTL_DICT);
    }

    /**
     * 清除指定字典的缓存
     * 
     * @param code 字典编码
     */
    public void evictDictItems(String code) {
        String cacheKey = PREFIX_DICT_ITEMS + code;
        log.info("清除字典项缓存: code={}", code);
        cacheDegradationService.deleteWithFallback(cacheKey);
    }

    /**
     * 清除所有字典缓存
     */
    public void evictAllDictItems() {
        log.info("清除所有字典缓存");
        evictByPattern(PREFIX_DICT_ITEMS + "*");
    }

    // ========== 缓存统计 ==========

    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        
        // 从 CacheDegradationService 获取本地缓存统计
        String localStats = cacheDegradationService.getLocalCacheStats();
        stats.setLocalCacheStats(localStats);
        
        // 获取熔断器状态
        stats.setCircuitBreakerState(cacheDegradationService.getCircuitBreakerState().name());
        stats.setCircuitBreakerInfo(cacheDegradationService.getCircuitBreakerInfo());
        
        // 统计 Redis 中的缓存键数量
        try {
            Set<String> configKeys = redisTemplate.keys(PREFIX_CONFIG + "*");
            Set<String> menuKeys = redisTemplate.keys(PREFIX_MENU_USER + "*");
            Set<String> deptKeys = redisTemplate.keys(PREFIX_DEPT + "*");
            Set<String> dictKeys = redisTemplate.keys(PREFIX_DICT_ITEMS + "*");
            
            stats.setConfigCacheCount(configKeys != null ? configKeys.size() : 0);
            stats.setMenuCacheCount(menuKeys != null ? menuKeys.size() : 0);
            stats.setDeptCacheCount(deptKeys != null ? deptKeys.size() : 0);
            stats.setTotalCacheCount(stats.getConfigCacheCount() + stats.getMenuCacheCount() + 
                    stats.getDeptCacheCount() + (dictKeys != null ? dictKeys.size() : 0));
        } catch (Exception e) {
            log.warn("获取 Redis 缓存统计失败: {}", e.getMessage());
            stats.setRedisAvailable(false);
        }
        
        return stats;
    }

    // ========== 缓存管理 ==========

    /**
     * 清除所有业务缓存
     */
    public void evictAll() {
        log.warn("清除所有业务缓存");
        evictAllConfigs();
        evictAllMenus();
        evictAllDepartments();
        evictAllDictItems();
        cacheDegradationService.clearLocalCache();
    }

    /**
     * 按模式清除缓存
     * 
     * @param pattern 键模式（支持 * 通配符）
     */
    private void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("按模式清除缓存: pattern={}, count={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.warn("按模式清除缓存失败: pattern={}, error={}", pattern, e.getMessage());
        }
    }
}
