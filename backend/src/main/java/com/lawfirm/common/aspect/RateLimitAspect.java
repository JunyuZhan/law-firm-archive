package com.lawfirm.common.aspect;

import com.lawfirm.common.annotation.RateLimit;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** API速率限制切面 使用Redis实现分布式限流 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

  /** Redis模板 */
  private final RedisTemplate<String, Object> redisTemplate;

  /** 限流键前缀 */
  private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

  /**
   * 限流检查
   *
   * @param joinPoint 连接点
   * @param rateLimit 限流注解
   * @return 方法执行结果
   * @throws Throwable 异常
   */
  @Around("@annotation(rateLimit)")
  public Object checkRateLimit(final ProceedingJoinPoint joinPoint, final RateLimit rateLimit)
      throws Throwable {
    // 构建限流Key
    String key = buildRateLimitKey(rateLimit, joinPoint);

    // 使用原子操作：先 increment，再判断是否超限（避免 check-then-act 竞态）
    Long count = redisTemplate.opsForValue().increment(key);
    if (count == null) {
      // Redis 连接问题，允许通过但记录警告
      log.warn("限流计数失败，允许请求通过: key={}", key);
      return joinPoint.proceed();
    }

    // 首次访问时设置过期时间
    if (count == 1) {
      redisTemplate.expire(key, rateLimit.period(), TimeUnit.SECONDS);
    }

    // 检查是否超过限制
    if (count > rateLimit.limit()) {
      log.warn("API速率限制: key={}, count={}, limit={}", key, count, rateLimit.limit());
      throw new BusinessException("429", rateLimit.message());
    }

    // 继续执行
    return joinPoint.proceed();
  }

  /**
   * 构建限流Key
   *
   * @param rateLimit 限流注解
   * @param joinPoint 连接点
   * @return 限流Key
   */
  private String buildRateLimitKey(final RateLimit rateLimit, final ProceedingJoinPoint joinPoint) {
    StringBuilder keyBuilder = new StringBuilder(RATE_LIMIT_KEY_PREFIX);
    keyBuilder.append(rateLimit.key()).append(":");

    switch (rateLimit.type()) {
      case USER:
        // 按用户限流
        try {
          Long userId = SecurityUtils.getUserId();
          keyBuilder.append("user:").append(userId);
        } catch (Exception e) {
          // 未登录时使用IP
          keyBuilder.append("ip:").append(getClientIp());
        }
        break;

      case IP:
        // 按IP限流
        keyBuilder.append("ip:").append(getClientIp());
        break;

      case GLOBAL:
        // 全局限流
        keyBuilder.append("global");
        break;

      default:
        // 默认按方法限流
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        keyBuilder
            .append("method:")
            .append(method.getDeclaringClass().getSimpleName())
            .append(".")
            .append(method.getName());
    }

    return keyBuilder.toString();
  }

  /**
   * 获取客户端IP
   *
   * @return 客户端IP地址，获取失败返回"unknown"
   */
  private String getClientIp() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
        return "unknown";
      }

      HttpServletRequest request = attributes.getRequest();

      // 尝试从各种Header中获取真实IP
      String[] headers = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_CLIENT_IP",
        "HTTP_X_FORWARDED_FOR"
      };

      String ip = null;
      for (String header : headers) {
        ip = request.getHeader(header);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
          // X-Forwarded-For 可能包含多个IP，取第一个
          if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
          }
          break;
        }
      }

      if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getRemoteAddr();
      }

      // IPv6本地地址转换
      if ("0:0:0:0:0:0:0:1".equals(ip)) {
        ip = "127.0.0.1";
      }

      return ip;
    } catch (Exception e) {
      log.warn("获取客户端IP失败", e);
      return "unknown";
    }
  }
}
