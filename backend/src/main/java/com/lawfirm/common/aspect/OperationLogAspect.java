package com.lawfirm.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.util.IpUtils;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 操作日志切面 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /** 操作日志仓储 */
  private final OperationLogRepository operationLogRepository;

  /** 参数最大长度限制 */
  private static final int MAX_PARAM_LENGTH = 2000;

  /**
   * 日志上下文参数对象，用于封装操作日志的请求和用户信息。
   *
   * <p>此类用于减少 saveLogAsync 方法的参数数量，将相关的上下文信息组合在一起。
   */
  private static class LogContext {
    /** 用户ID */
    private final Long userId;

    /** 用户名 */
    private final String username;

    /** 请求URI */
    private final String requestUri;

    /** 请求方法 */
    private final String requestMethod;

    /** IP地址 */
    private final String ip;

    /** 请求参数 */
    private final String params;

    /** 方法名 */
    private final String methodName;

    /**
     * 构造日志上下文。
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param requestUri 请求URI
     * @param requestMethod 请求方法
     * @param ip IP地址
     * @param params 请求参数
     * @param methodName 方法名
     */
    LogContext(
        final Long userId,
        final String username,
        final String requestUri,
        final String requestMethod,
        final String ip,
        final String params,
        final String methodName) {
      this.userId = userId;
      this.username = username;
      this.requestUri = requestUri;
      this.requestMethod = requestMethod;
      this.ip = ip;
      this.params = params;
      this.methodName = methodName;
    }

    public Long getUserId() {
      return userId;
    }

    public String getUsername() {
      return username;
    }

    public String getRequestUri() {
      return requestUri;
    }

    public String getRequestMethod() {
      return requestMethod;
    }

    public String getIp() {
      return ip;
    }

    public String getParams() {
      return params;
    }

    public String getMethodName() {
      return methodName;
    }
  }

  /**
   * 操作日志切面
   *
   * @param joinPoint 连接点
   * @return 方法返回值
   * @throws Throwable 异常
   */
  @Around("@annotation(com.lawfirm.common.annotation.OperationLog)")
  public Object logOperation(final ProceedingJoinPoint joinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();

    // 1. 获取注解信息
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    OperationLog annotation = method.getAnnotation(OperationLog.class);

    // 2. 获取请求信息
    HttpServletRequest request = getRequest();
    String requestUri = request != null ? request.getRequestURI() : "";
    String requestMethod = request != null ? request.getMethod() : "";
    // ✅ 使用 IpUtils 获取真实IP
    String ip = request != null ? IpUtils.getIpAddr(request) : "";

    // 3. 获取用户信息
    Long userId = null;
    String username = "anonymous";
    try {
      userId = SecurityUtils.getUserId();
      username = SecurityUtils.getUsername();
    } catch (Exception ignored) {
      // 未登录状态
    }

    // 4. 记录请求参数
    String params = "";
    if (annotation.saveParams()) {
      try {
        Object[] args = joinPoint.getArgs();
        params = objectMapper.writeValueAsString(args);
        if (params.length() > MAX_PARAM_LENGTH) {
          params = params.substring(0, MAX_PARAM_LENGTH) + "...";
        }
      } catch (Exception e) {
        params = "参数序列化失败";
      }
    }

    // 5. 执行方法
    Object result = null;
    String errorMsg = null;
    boolean success = true;
    try {
      result = joinPoint.proceed();
    } catch (Throwable e) {
      success = false;
      errorMsg = e.getMessage();
      throw e;
    } finally {
      long costTime = System.currentTimeMillis() - startTime;

      // 6. 记录日志
      log.info(
          "操作日志 | 模块:{} | 操作:{} | 用户:{}({}) | IP:{} | URI:{} {} | 耗时:{}ms | 成功:{} | 错误:{}",
          annotation.module(),
          annotation.action(),
          username,
          userId,
          ip,
          requestMethod,
          requestUri,
          costTime,
          success,
          errorMsg);

      // 7. 异步保存到数据库
      if (userId != null) {
        String methodName =
            signature.getDeclaringTypeName() + "." + signature.getMethod().getName();
        LogContext logContext =
            new LogContext(userId, username, requestUri, requestMethod, ip, params, methodName);
        saveLogAsync(annotation, logContext, costTime, success, errorMsg);
      }
    }

    return result;
  }

  private HttpServletRequest getRequest() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return attributes != null ? attributes.getRequest() : null;
  }

  /**
   * 异步保存操作日志
   *
   * @param annotation 注解
   * @param logContext 日志上下文（包含用户、请求、方法等信息）
   * @param costTime 耗时
   * @param success 是否成功
   * @param errorMsg 错误消息
   */
  @Async
  private void saveLogAsync(
      final OperationLog annotation,
      final LogContext logContext,
      final long costTime,
      final boolean success,
      final String errorMsg) {
    try {
      com.lawfirm.domain.system.entity.OperationLog logEntity =
          com.lawfirm.domain.system.entity.OperationLog.builder()
              .userId(logContext.getUserId())
              .userName(logContext.getUsername())
              .module(annotation.module())
              .operationType(annotation.action())
              .description(annotation.module() + " - " + annotation.action())
              .method(logContext.getMethodName())
              .requestUrl(logContext.getRequestUri())
              .requestMethod(logContext.getRequestMethod())
              .requestParams(logContext.getParams())
              .ipAddress(logContext.getIp())
              .executionTime(costTime)
              .status(
                  success
                      ? com.lawfirm.domain.system.entity.OperationLog.STATUS_SUCCESS
                      : com.lawfirm.domain.system.entity.OperationLog.STATUS_FAIL)
              .errorMessage(errorMsg)
              .build();

      operationLogRepository.save(logEntity);
    } catch (Exception e) {
      log.error("保存操作日志失败", e);
    }
  }
}
