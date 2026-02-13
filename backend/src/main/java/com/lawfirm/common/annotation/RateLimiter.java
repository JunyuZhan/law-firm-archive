package com.lawfirm.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 *
 * <p>功能： - 方法级限流 - 支持自定义限流键（SpEL表达式） - 支持自定义限流速率和时间窗口
 *
 * <p>使用示例：
 *
 * <pre>
 * &#64;RateLimiter(key = "'login:ip:' + #ip", rate = 10, interval = 900)
 * public LoginResult login(String username, String password, String ip) { ... }
 * </pre>
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

  /**
   * 限流键 支持SpEL表达式，如：'login:ip:' + #ip 为空时使用 类名:方法名 作为默认键
   *
   * @return 属性值
   */
  String key() default "";

  /**
   * 时间窗口内允许的最大请求数
   *
   * @return 属性值
   */
  int rate() default 10;

  /**
   * 时间窗口（秒）
   *
   * @return 属性值
   */
  int interval() default 60;

  /**
   * 限流提示消息
   *
   * @return 属性值
   */
  String message() default "请求过于频繁，请稍后重试";

  /**
   * 限流类型
   *
   * @return 属性值
   */
  LimitType limitType() default LimitType.DEFAULT;

  /** 限流类型枚举 */
  enum LimitType {
    /** 默认：按键限流 */
    DEFAULT,
    /** 按IP限流 */
    IP,
    /** 按用户限流 */
    USER,
    /** 全局限流 */
    GLOBAL
  }
}
