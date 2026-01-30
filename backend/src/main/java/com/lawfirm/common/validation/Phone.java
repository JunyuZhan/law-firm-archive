package com.lawfirm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 手机号校验注解
 *
 * <p>支持中国大陆手机号格式（11位，1开头）
 *
 * <p>使用示例：
 *
 * <pre>
 * public class ClientDTO {
 *     @Phone(message = "手机号格式不正确")
 *     private String phone;
 * }
 * </pre>
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {

  /**
   * 错误消息
   *
   * @return 错误消息
   */
  String message() default "手机号格式不正确";

  /**
   * 验证组
   *
   * @return 验证组
   */
  Class<?>[] groups() default {};

  /**
   * 负载
   *
   * @return 负载
   */
  Class<? extends Payload>[] payload() default {};

  /**
   * 是否允许为空
   *
   * @return 是否允许为空
   */
  boolean nullable() default true;
}
