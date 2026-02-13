package com.lawfirm.common.aspect;

import com.lawfirm.common.annotation.Idempotent;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口幂等性切面
 *
 * <p>实现原理： 1. 根据配置的策略生成幂等键 2. 使用 Redis SETNX 实现分布式锁 3. 根据配置决定是在执行前还是执行后标记
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Aspect
@Component
@Order(1) // 优先级高于其他切面
@RequiredArgsConstructor
public class IdempotentAspect {

  /** Redis模板 */
  private final StringRedisTemplate redisTemplate;

  /** 幂等键前缀 */
  private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

  /** SpEL表达式解析器 */
  private static final SpelExpressionParser PARSER = new SpelExpressionParser();

  /** 参数名发现器 */
  private static final DefaultParameterNameDiscoverer NAME_DISCOVERER =
      new DefaultParameterNameDiscoverer();

  /** 分布式锁默认过期时间（秒） */
  private static final int DEFAULT_LOCK_EXPIRE_SECONDS = 30;

  /**
   * 幂等性切面处理
   *
   * @param point 连接点
   * @param idempotent 注解
   * @return 方法返回值
   * @throws Throwable 异常
   */
  @Around("@annotation(idempotent)")
  public Object around(final ProceedingJoinPoint point, final Idempotent idempotent)
      throws Throwable {
    // 生成幂等键
    String idempotentKey = generateIdempotentKey(point, idempotent);
    if (!StringUtils.hasText(idempotentKey)) {
      log.warn("幂等键为空，跳过幂等性检查");
      return point.proceed();
    }

    String redisKey = IDEMPOTENT_KEY_PREFIX + idempotentKey;
    long expireSeconds = idempotent.expireSeconds();

    if (idempotent.markAfterSuccess()) {
      // 执行成功后才标记
      return executeWithMarkAfterSuccess(point, redisKey, expireSeconds, idempotent.message());
    } else {
      // 执行前就标记（严格防重）
      return executeWithMarkBefore(point, redisKey, expireSeconds, idempotent.message());
    }
  }

  /**
   * 执行前标记（严格防重模式）
   *
   * @param point 连接点
   * @param redisKey Redis键
   * @param expireSeconds 过期秒数
   * @param message 错误消息
   * @return 方法返回值
   * @throws Throwable 异常
   */
  private Object executeWithMarkBefore(
      final ProceedingJoinPoint point,
      final String redisKey,
      final long expireSeconds,
      final String message)
      throws Throwable {
    // 尝试设置幂等键
    Boolean success =
        redisTemplate.opsForValue().setIfAbsent(redisKey, "1", expireSeconds, TimeUnit.SECONDS);

    if (Boolean.FALSE.equals(success)) {
      log.warn("接口幂等性检查失败，请求已处理: key={}", redisKey);
      throw new BusinessException(message);
    }

    try {
      log.debug("幂等性检查通过，开始执行: key={}", redisKey);
      return point.proceed();
    } catch (Throwable e) {
      // 执行失败，删除幂等键，允许重试
      redisTemplate.delete(redisKey);
      log.debug("执行失败，已删除幂等键允许重试: key={}", redisKey);
      throw e;
    }
  }

  /**
   * 执行成功后标记（允许失败重试模式）
   *
   * @param point 连接点
   * @param redisKey Redis键
   * @param expireSeconds 过期秒数
   * @param message 错误消息
   * @return 方法返回值
   * @throws Throwable 异常
   */
  private Object executeWithMarkAfterSuccess(
      final ProceedingJoinPoint point,
      final String redisKey,
      final long expireSeconds,
      final String message)
      throws Throwable {
    // 先检查是否已处理
    String existingValue = redisTemplate.opsForValue().get(redisKey);
    if (StringUtils.hasText(existingValue)) {
      log.warn("接口幂等性检查失败，请求已处理: key={}", redisKey);
      throw new BusinessException(message);
    }

    // 使用分布式锁防止并发
    String lockKey = redisKey + ":lock";
    Boolean locked =
        redisTemplate
            .opsForValue()
            .setIfAbsent(lockKey, "1", DEFAULT_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

    if (Boolean.FALSE.equals(locked)) {
      log.warn("获取幂等锁失败，请求正在处理中: key={}", redisKey);
      throw new BusinessException("请求正在处理中，请稍后");
    }

    try {
      // 再次检查（双重检查）
      existingValue = redisTemplate.opsForValue().get(redisKey);
      if (StringUtils.hasText(existingValue)) {
        log.warn("接口幂等性检查失败（双重检查），请求已处理: key={}", redisKey);
        throw new BusinessException(message);
      }

      // 执行业务逻辑
      log.debug("幂等性检查通过，开始执行: key={}", redisKey);
      Object result = point.proceed();

      // 执行成功，标记为已处理
      redisTemplate.opsForValue().set(redisKey, "1", expireSeconds, TimeUnit.SECONDS);
      log.debug("执行成功，已标记幂等键: key={}", redisKey);

      return result;
    } finally {
      // 释放锁
      redisTemplate.delete(lockKey);
    }
  }

  /**
   * 生成幂等键
   *
   * @param point 连接点
   * @param idempotent 注解
   * @return 幂等键
   */
  private String generateIdempotentKey(
      final ProceedingJoinPoint point, final Idempotent idempotent) {
    String key = null;

    switch (idempotent.keySource()) {
      case SPEL:
        key = parseSpelKey(point, idempotent.key());
        break;
      case HEADER:
        key = getHeaderKey(idempotent.headerName());
        break;
      case PARAM:
        key = getParamKey(idempotent.key());
        break;
      default:
        key = null;
        break;
    }

    if (!StringUtils.hasText(key)) {
      return null;
    }

    // 添加用户ID前缀（如果已登录）
    try {
      Long userId = SecurityUtils.getUserId();
      if (userId != null) {
        key = userId + ":" + key;
      }
    } catch (Exception e) {
      // 未登录，不添加用户前缀
    }

    // 添加方法签名
    MethodSignature signature = (MethodSignature) point.getSignature();
    String methodKey = signature.getDeclaringTypeName() + "." + signature.getName();

    return methodKey + ":" + key;
  }

  /**
   * 解析SpEL表达式
   *
   * @param point 连接点
   * @param spelExpression SpEL表达式
   * @return 解析结果
   */
  private String parseSpelKey(final ProceedingJoinPoint point, final String spelExpression) {
    if (!StringUtils.hasText(spelExpression)) {
      return null;
    }

    try {
      MethodSignature signature = (MethodSignature) point.getSignature();
      Method method = signature.getMethod();
      Object[] args = point.getArgs();
      String[] paramNames = NAME_DISCOVERER.getParameterNames(method);

      if (paramNames == null || paramNames.length == 0) {
        return null;
      }

      EvaluationContext context = new StandardEvaluationContext();
      for (int i = 0; i < paramNames.length; i++) {
        context.setVariable(paramNames[i], args[i]);
      }

      Expression expression = PARSER.parseExpression(spelExpression);
      Object value = expression.getValue(context);
      return value != null ? value.toString() : null;
    } catch (Exception e) {
      log.warn("解析SpEL表达式失败: {}", spelExpression, e);
      return null;
    }
  }

  /**
   * 从请求头获取幂等键
   *
   * @param headerName 请求头名称
   * @return 幂等键
   */
  private String getHeaderKey(final String headerName) {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(headerName);
      }
    } catch (Exception e) {
      log.warn("获取请求头失败: {}", headerName, e);
    }
    return null;
  }

  /**
   * 从请求参数获取幂等键
   *
   * @param paramName 参数名
   * @return 幂等键
   */
  private String getParamKey(final String paramName) {
    if (!StringUtils.hasText(paramName)) {
      return null;
    }

    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        return request.getParameter(paramName);
      }
    } catch (Exception e) {
      log.warn("获取请求参数失败: {}", paramName, e);
    }
    return null;
  }
}
