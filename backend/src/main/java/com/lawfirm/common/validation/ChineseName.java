package com.lawfirm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 中文姓名校验注解
 *
 * <p>校验规则： - 2-30个字符 - 支持中文、少数民族名字中的点号 - 不支持数字和特殊字符
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Documented
@Constraint(validatedBy = ChineseNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChineseName {

  /**
   * 错误消息
   *
   * @return 错误消息
   */
  String message() default "姓名格式不正确";

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
   * 最小长度
   *
   * @return 最小长度
   */
  int min() default 2;

  /**
   * 最大长度
   *
   * @return 最大长度
   */
  int max() default 30;
}
