package com.lawfirm.common.annotation;

import java.lang.annotation.*;

/**
 * 缓存预热注解
 * 
 * 标记在方法上，系统启动时自动调用该方法加载数据到缓存
 * 
 * 使用示例：
 * @CacheWarmUp(keyPrefix = "dict:", order = 1)
 * public List<DictItem> loadAllDictItems() { ... }
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheWarmUp {

    /**
     * 缓存key前缀
     */
    String keyPrefix() default "";

    /**
     * 预热顺序（数字越小越先执行）
     */
    int order() default 100;

    /**
     * 描述信息
     */
    String description() default "";

    /**
     * 是否异步执行
     */
    boolean async() default false;

    /**
     * 失败是否继续（true=继续预热其他缓存，false=中断启动）
     */
    boolean continueOnError() default true;
}
