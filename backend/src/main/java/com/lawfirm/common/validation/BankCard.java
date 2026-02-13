package com.lawfirm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 银行卡号校验注解
 *
 * <p>支持16-19位银行卡号，可选Luhn算法校验
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Documented
@Constraint(validatedBy = BankCardValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BankCard {

  /**
   * 错误消息
   *
   * @return 错误消息
   */
  String message() default "银行卡号格式不正确";

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
   * 是否验证Luhn校验位
   *
   * @return 是否验证Luhn校验位
   */
  boolean luhn() default false;
}
