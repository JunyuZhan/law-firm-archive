package com.archivesystem.aspect;

import java.lang.annotation.*;

/**
 * 操作日志注解.
 * 用于标记需要记录操作日志的方法.
 * @author junyuzhan
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    
    /**
     * 对象类型.
     */
    String objectType() default "";
    
    /**
     * 操作类型.
     */
    String operationType();
    
    /**
     * 操作描述.
     */
    String description() default "";
}
