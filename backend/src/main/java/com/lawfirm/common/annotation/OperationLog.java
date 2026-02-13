package com.lawfirm.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 *
 * <p>使用示例： @OperationLog(module = "用户管理", action = "创建用户")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

  /**
   * 模块名称
   *
   * @return 属性值
   */
  String module();

  /**
   * 操作动作
   *
   * @return 属性值
   */
  String action();

  /**
   * 是否记录请求参数
   *
   * @return 属性值
   */
  boolean saveParams() default true;

  /**
   * 是否记录响应结果
   *
   * @return 属性值
   */
  boolean saveResult() default false;
}
