package com.lawfirm.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 日志工具类
 *
 * <p>提供增强的日志功能： - 性能日志记录 - 方法调用格式化 - 敏感数据脱敏 - 条件日志
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public final class LogUtils {

  /** JSON对象映射器 */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** 敏感字段默认列表 */
  private static final String[] DEFAULT_SENSITIVE_FIELDS = {
    "password", "pwd", "secret", "token", "accessToken", "refreshToken",
    "idCard", "idNumber", "bankCard", "bankAccount", "phone", "mobile",
    "email", "address"
  };

  /** 最大JSON长度. */
  private static final int MAX_JSON_LENGTH = 200;

  private LogUtils() {}

  /**
   * 记录方法执行性能
   *
   * @param operation 操作名称
   * @param task 要执行的任务
   */
  public static void logWithPerformance(final String operation, final Runnable task) {
    long startTime = System.currentTimeMillis();
    try {
      task.run();
      long duration = System.currentTimeMillis() - startTime;
      log.info("[性能] {} 执行成功，耗时 {} ms", operation, duration);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("[性能] {} 执行失败，耗时 {} ms，错误: {}", operation, duration, e.getMessage());
      throw e;
    }
  }

  /**
   * 记录方法执行性能（带返回值）
   *
   * @param operation 操作名称
   * @param supplier 要执行的任务
   * @param <T> 返回类型
   * @return 任务执行结果
   */
  public static <T> T logWithPerformance(final String operation, final Supplier<T> supplier) {
    long startTime = System.currentTimeMillis();
    try {
      T result = supplier.get();
      long duration = System.currentTimeMillis() - startTime;
      log.info("[性能] {} 执行成功，耗时 {} ms", operation, duration);
      return result;
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("[性能] {} 执行失败，耗时 {} ms，错误: {}", operation, duration, e.getMessage());
      throw e;
    }
  }

  /**
   * 记录慢操作（超过阈值才记录）
   *
   * @param operation 操作名称
   * @param thresholdMs 阈值（毫秒）
   * @param task 要执行的任务
   */
  public static void logSlowOperation(
      final String operation, final long thresholdMs, final Runnable task) {
    long startTime = System.currentTimeMillis();
    try {
      task.run();
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      if (duration > thresholdMs) {
        log.warn("[慢操作] {} 耗时 {} ms（阈值 {} ms）", operation, duration, thresholdMs);
      }
    }
  }

  /**
   * 获取方法调用的完整信息
   *
   * @param className 类名
   * @param methodName 方法名
   * @param args 参数
   * @return 格式化的方法调用信息
   */
  public static String getMethodCallInfo(
      final String className, final String methodName, final Object[] args) {
    StringBuilder sb = new StringBuilder();
    sb.append(className).append("#").append(methodName);
    if (args != null && args.length > 0) {
      sb.append("(");
      for (int i = 0; i < args.length; i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(formatArg(args[i]));
      }
      sb.append(")");
    }
    return sb.toString();
  }

  /**
   * 格式化参数值（用于日志输出）
   *
   * @param arg 参数
   * @return 格式化后的字符串
   */
  public static String formatArg(final Object arg) {
    if (arg == null) {
      return "null";
    }
    if (arg instanceof String) {
      String str = (String) arg;
      if (str.length() > 100) {
        return "\"" + str.substring(0, 100) + "...\"";
      }
      return "\"" + str + "\"";
    }
    if (arg instanceof Collection) {
      return "[" + ((Collection<?>) arg).size() + " items]";
    }
    if (arg instanceof Map) {
      return "{" + ((Map<?, ?>) arg).size() + " entries}";
    }
    if (arg.getClass().isArray()) {
      if (arg instanceof Object[]) {
        return Arrays.toString((Object[]) arg);
      }
      return "[array]";
    }
    try {
      String json = OBJECT_MAPPER.writeValueAsString(arg);
      if (json.length() > MAX_JSON_LENGTH) {
        return json.substring(0, MAX_JSON_LENGTH) + "...";
      }
      return json;
    } catch (JsonProcessingException e) {
      return arg.toString();
    }
  }

  /**
   * 格式化异常堆栈（简化版）
   *
   * @param e 异常
   * @return 格式化的异常信息
   */
  public static String formatException(final Throwable e) {
    if (e == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(e.toString());
    StackTraceElement[] stackTrace = e.getStackTrace();
    int limit = Math.min(stackTrace.length, 10); // 只显示前10行
    for (int i = 0; i < limit; i++) {
      sb.append("\n\tat ").append(stackTrace[i]);
    }
    if (stackTrace.length > 10) {
      sb.append("\n\t... ").append(stackTrace.length - 10).append(" more");
    }
    return sb.toString();
  }

  /**
   * 日志内容脱敏（使用默认敏感字段）
   *
   * @param content JSON格式的内容
   * @return 脱敏后的内容
   */
  public static String desensitize(final String content) {
    return desensitize(content, DEFAULT_SENSITIVE_FIELDS);
  }

  /**
   * 日志内容脱敏
   *
   * @param content JSON格式的内容
   * @param sensitiveFields 敏感字段列表
   * @return 脱敏后的内容
   */
  public static String desensitize(final String content, final String[] sensitiveFields) {
    if (!StringUtils.hasText(content) || sensitiveFields == null || sensitiveFields.length == 0) {
      return content;
    }
    String result = content;
    for (String field : sensitiveFields) {
      // 匹配 "fieldName":"value" 或 "fieldName": "value" 格式
      result =
          result.replaceAll("\"" + field + "\"\\s*:\\s*\"[^\"]*\"", "\"" + field + "\":\"***\"");
      // 匹配 "fieldName":value（非字符串值）
      result =
          result.replaceAll("\"" + field + "\"\\s*:\\s*([^,}\\]]+)", "\"" + field + "\":\"***\"");
    }
    return result;
  }

  /**
   * 条件日志 - DEBUG级别
   *
   * @param condition 条件
   * @param msgSupplier 消息提供者
   */
  public static void debugIf(final boolean condition, final Supplier<String> msgSupplier) {
    if (condition && log.isDebugEnabled()) {
      log.debug(msgSupplier.get());
    }
  }

  /**
   * 条件日志 - INFO级别
   *
   * @param condition 条件
   * @param msgSupplier 消息提供者
   */
  public static void infoIf(final boolean condition, final Supplier<String> msgSupplier) {
    if (condition && log.isInfoEnabled()) {
      log.info(msgSupplier.get());
    }
  }

  /**
   * 条件日志 - WARN级别
   *
   * @param condition 条件
   * @param msgSupplier 消息提供者
   */
  public static void warnIf(final boolean condition, final Supplier<String> msgSupplier) {
    if (condition && log.isWarnEnabled()) {
      log.warn(msgSupplier.get());
    }
  }

  /**
   * 记录业务操作日志
   *
   * @param module 模块
   * @param action 操作
   * @param detail 详情
   */
  public static void logBusiness(final String module, final String action, final String detail) {
    log.info("[业务] [{}] {} - {}", module, action, detail);
  }

  /**
   * 记录安全相关日志
   *
   * @param event 安全事件
   * @param detail 详情
   */
  public static void logSecurity(final String event, final String detail) {
    log.warn("[安全] {} - {}", event, detail);
  }

  /**
   * 记录审计日志
   *
   * @param userId 用户ID
   * @param action 操作
   * @param resource 资源
   * @param result 结果
   */
  public static void logAudit(
      final Long userId, final String action, final String resource, final String result) {
    log.info("[审计] 用户:{} 操作:{} 资源:{} 结果:{}", userId, action, resource, result);
  }

  /**
   * 获取调用者信息（类名#方法名）
   *
   * @return 调用者信息
   */
  public static String getCallerInfo() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    if (stackTrace.length > 3) {
      StackTraceElement caller = stackTrace[3];
      return caller.getClassName() + "#" + caller.getMethodName();
    }
    return "unknown";
  }
}
