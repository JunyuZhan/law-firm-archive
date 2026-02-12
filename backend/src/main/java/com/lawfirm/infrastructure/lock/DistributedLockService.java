package com.lawfirm.infrastructure.lock;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 分布式锁服务.
 *
 * <p>基于 Redis 实现的分布式锁，用于集群环境下防止定时任务重复执行
 *
 * @author system
 * @since 2026-02-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

  /** Redis 模板. */
  private final RedisTemplate<String, Object> redisTemplate;

  /** 锁前缀. */
  private static final String LOCK_PREFIX = "distributed:lock:";

  /** 定时任务锁前缀. */
  private static final String SCHEDULER_LOCK_PREFIX = "scheduler:lock:";

  /**
   * 尝试获取分布式锁.
   *
   * @param lockKey 锁的唯一标识
   * @param expireSeconds 锁的过期时间（秒）
   * @return 是否成功获取锁
   */
  public boolean tryLock(final String lockKey, final long expireSeconds) {
    String key = LOCK_PREFIX + lockKey;
    try {
      Boolean success =
          redisTemplate.opsForValue().setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
      if (Boolean.TRUE.equals(success)) {
        log.debug("获取分布式锁成功: key={}", key);
        return true;
      }
      log.debug("获取分布式锁失败（已被占用）: key={}", key);
      return false;
    } catch (Exception e) {
      log.error("获取分布式锁异常: key={}", key, e);
      return false;
    }
  }

  /**
   * 释放分布式锁.
   *
   * @param lockKey 锁的唯一标识
   */
  public void unlock(final String lockKey) {
    String key = LOCK_PREFIX + lockKey;
    try {
      redisTemplate.delete(key);
      log.debug("释放分布式锁: key={}", key);
    } catch (Exception e) {
      log.error("释放分布式锁异常: key={}", key, e);
    }
  }

  /**
   * 尝试获取定时任务锁.
   *
   * <p>专门用于定时任务，使用独立的前缀
   *
   * @param taskName 任务名称
   * @param expireSeconds 锁的过期时间（秒），建议设置为任务预计执行时间的2倍
   * @return 是否成功获取锁
   */
  public boolean trySchedulerLock(final String taskName, final long expireSeconds) {
    String key = SCHEDULER_LOCK_PREFIX + taskName;
    try {
      Boolean success =
          redisTemplate.opsForValue().setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
      if (Boolean.TRUE.equals(success)) {
        log.debug("获取定时任务锁成功: task={}", taskName);
        return true;
      }
      log.debug("定时任务已在其他节点执行，跳过: task={}", taskName);
      return false;
    } catch (Exception e) {
      log.error("获取定时任务锁异常: task={}", taskName, e);
      // 获取锁失败时，默认不执行任务（安全策略）
      return false;
    }
  }

  /**
   * 释放定时任务锁.
   *
   * @param taskName 任务名称
   */
  public void unlockScheduler(final String taskName) {
    String key = SCHEDULER_LOCK_PREFIX + taskName;
    try {
      redisTemplate.delete(key);
      log.debug("释放定时任务锁: task={}", taskName);
    } catch (Exception e) {
      log.error("释放定时任务锁异常: task={}", taskName, e);
    }
  }

  /**
   * 执行带分布式锁的定时任务.
   *
   * <p>推荐使用此方法，自动处理锁的获取和释放
   *
   * @param taskName 任务名称
   * @param expireSeconds 锁的过期时间（秒）
   * @param task 要执行的任务
   * @return 是否执行了任务（false 表示未获取到锁，任务未执行）
   */
  public boolean executeWithLock(
      final String taskName, final long expireSeconds, final Runnable task) {
    if (!trySchedulerLock(taskName, expireSeconds)) {
      return false;
    }
    try {
      task.run();
      return true;
    } finally {
      unlockScheduler(taskName);
    }
  }
}
