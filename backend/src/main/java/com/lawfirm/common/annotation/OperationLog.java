package com.lawfirm.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 
 * 使用示例：
 * @OperationLog(module = "用户管理", action = "创建用户")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 模块名称
     */
    String module();

    /**
     * 操作动作
     */
    String action();

    /**
     * 是否记录请求参数
     */
    boolean saveParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean saveResult() default false;
}

