package com.lawfirm.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 *
 * <p>使用示例： @RequirePermission("sys:user:create") @RequirePermission(value = {"sys:user:update",
 * "sys:user:delete"}, logical = Logical.OR)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

  /**
   * 权限编码
   *
   * @return 属性值
   */
  String[] value();

  /**
   * 多个权限的逻辑关系
   *
   * @return 属性值
   */
  Logical logical() default Logical.AND;

  /** 逻辑关系枚举 */
  enum Logical {
    /** 与 */
    AND,
    /** 或 */
    OR
  }
}
