package com.lawfirm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 身份证号校验注解
 *
 * <p>支持18位身份证号，包含校验位验证
 *
 * <p>使用示例：
 *
 * <pre>
 * public class ClientDTO {
 *     @IdCard(message = "身份证号格式不正确")
 *     private String idCard;
 * }
 * </pre>
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Documented
@Constraint(validatedBy = IdCardValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdCard {

  /**
   * 错误消息
   *
   * @return 错误消息
   */
  String message() default "身份证号格式不正确";

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

  /**
   * 是否验证校验位（严格模式）
   *
   * @return 是否验证校验位
   */
  boolean strict() default true;
}
