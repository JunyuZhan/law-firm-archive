package com.lawfirm.infrastructure.aspect;

import com.lawfirm.infrastructure.notification.WecomNotificationChannel;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 慢接口监控切面
 *
 * <p>功能： 1. 监控所有 Controller 接口执行时间 2. 超过阈值自动告警（记录日志） 3. 统计接口调用情况
 *
 * @author junyuzhan
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SlowApiAspect {

  /** 企业微信通知渠道 */
  private final WecomNotificationChannel wecomNotificationChannel;

  /** 慢接口阈值（毫秒），默认 3000ms */
  @Value("${law-firm.performance.slow-api-threshold:3000}")
  private long slowApiThreshold;

  /** 警告阈值（毫秒），默认 1000ms. */
  @Value("${law-firm.performance.warn-api-threshold:1000}")
  private long warnApiThreshold;

  /** 是否启用企业微信告警，默认关闭. */
  @Value("${law-firm.performance.slow-api-alert-enabled:false}")
  private boolean alertEnabled;

  /** 切入点：所有 Controller 的 public 方法. */
  @Pointcut("execution(public * com.lawfirm.interfaces.rest..*Controller.*(..))")
  public void controllerPointcut() {}

  /**
   * 环绕通知：监控接口执行时间.
   *
   * @param point 切入点
   * @return 方法执行结果
   * @throws Throwable 方法执行异常
   */
  @Around("controllerPointcut()")
  public Object around(final ProceedingJoinPoint point) throws Throwable {
    long startTime = System.currentTimeMillis();

    // 获取请求信息
    String requestUri = "";
    String requestMethod = "";
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        requestUri = request.getRequestURI();
        requestMethod = request.getMethod();
      }
    } catch (Exception e) {
      // 忽略获取请求信息的异常
    }

    // 获取方法信息
    MethodSignature signature = (MethodSignature) point.getSignature();
    String className = signature.getDeclaringType().getSimpleName();
    String methodName = signature.getName();

    Object result = null;
    Throwable throwable = null;

    try {
      result = point.proceed();
      return result;
    } catch (Throwable t) {
      throwable = t;
      throw t;
    } finally {
      long costTime = System.currentTimeMillis() - startTime;

      // 记录日志
      logApiPerformance(
          requestMethod, requestUri, className, methodName, costTime, throwable != null);
    }
  }

  /**
   * 记录 API 性能日志.
   *
   * @param method HTTP方法
   * @param uri 请求URI
   * @param className 类名
   * @param methodName 方法名
   * @param costTime 耗时
   * @param hasError 是否有错误
   */
  private void logApiPerformance(
      final String method,
      final String uri,
      final String className,
      final String methodName,
      final long costTime,
      final boolean hasError) {
    String apiInfo = String.format("%s %s [%s.%s]", method, uri, className, methodName);

    if (hasError) {
      log.error("❌ 接口异常 - {} - {}ms", apiInfo, costTime);
    } else if (costTime >= slowApiThreshold) {
      // 慢接口告警
      log.error("🐢 慢接口告警 - {} - {}ms (阈值: {}ms)", apiInfo, costTime, slowApiThreshold);
      // 发送企业微信告警通知
      sendSlowApiAlert(apiInfo, costTime);
    } else if (costTime >= warnApiThreshold) {
      // 性能警告
      log.warn("⚠️ 接口较慢 - {} - {}ms", apiInfo, costTime);
    } else {
      // 正常日志（DEBUG 级别，生产环境可关闭）
      if (log.isDebugEnabled()) {
        log.debug("✅ 接口正常 - {} - {}ms", apiInfo, costTime);
      }
    }
  }

  /**
   * 发送慢接口告警通知.
   *
   * @param apiInfo 接口信息
   * @param costTime 耗时
   */
  private void sendSlowApiAlert(final String apiInfo, final long costTime) {
    if (!alertEnabled) {
      return;
    }

    try {
      String title = "慢接口告警";
      String content =
          String.format(
              "**接口**: %s\n"
                  + "**耗时**: %dms\n"
                  + "**阈值**: %dms\n"
                  + "**时间**: %s\n\n"
                  + "请检查接口性能问题！",
              apiInfo,
              costTime,
              slowApiThreshold,
              java.time.LocalDateTime.now()
                  .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

      // 发送紧急通知到企业微信
      wecomNotificationChannel.sendUrgentNotification(title, content, Collections.emptyList());

    } catch (Exception e) {
      log.warn("发送慢接口告警失败: {}", e.getMessage());
    }
  }
}
