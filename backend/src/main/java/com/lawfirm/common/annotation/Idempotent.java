package com.lawfirm.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等性注解
 *
 * <p>与 @RepeatSubmit 的区别： - @RepeatSubmit：防止短时间内重复提交（基于时间窗口）
 * - @Idempotent：保证接口幂等性（基于业务唯一键，支持更长的有效期）
 *
 * <p>适用场景： - 支付接口（基于订单号保证幂等） - 创建订单（基于请求ID保证幂等） - 发送通知（基于消息ID保证幂等）
 *
 * <p>使用示例：
 *
 * <pre>
 * // 基于订单号保证幂等
 * &#64;Idempotent(key = "#command.orderNo", expireSeconds = 86400)
 * public void createPayment(CreatePaymentCommand command) { ... }
 *
 * // 基于请求头中的幂等键
 * &#64;Idempotent(keySource = KeySource.HEADER, headerName = "X-Idempotent-Key")
 * public void processOrder(OrderCommand command) { ... }
 *
 * // 基于参数组合
 * &#64;Idempotent(key = "#userId + ':' + #action")
 * public void doAction(Long userId, String action) { ... }
 * </pre>
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

  /**
   * 幂等键（SpEL表达式） 当 keySource 为 SPEL 时使用 支持引用方法参数，如：#command.orderNo, #userId
   *
   * @return 幂等键表达式
   */
  String key() default "";

  /**
   * 幂等键来源
   *
   * @return 键来源类型
   */
  KeySource keySource() default KeySource.SPEL;

  /**
   * 请求头名称（当 keySource 为 HEADER 时使用）
   *
   * @return 请求头名称
   */
  String headerName() default "X-Idempotent-Key";

  /**
   * 过期时间（秒） 默认24小时，根据业务场景调整
   *
   * @return 过期时间（秒）
   */
  long expireSeconds() default 86400;

  /**
   * 提示消息
   *
   * @return 提示消息内容
   */
  String message() default "请求已处理，请勿重复提交";

  /**
   * 是否在执行成功后才标记为已处理 true: 方法执行成功后才标记（适用于需要确保执行成功的场景） false: 方法执行前就标记（适用于需要严格防重的场景）
   *
   * @return 是否在执行成功后才标记
   */
  boolean markAfterSuccess() default false;

  /**
   * 是否返回上次执行的结果 仅当 markAfterSuccess = true 时有效
   *
   * @return 是否返回上次执行的结果
   */
  boolean returnPreviousResult() default false;

  /** 幂等键来源 */
  enum KeySource {
    /** 使用SpEL表达式从方法参数中提取 */
    SPEL,
    /** 从请求头中获取 */
    HEADER,
    /** 从请求参数中获取（参数名由key指定） */
    PARAM
  }
}
