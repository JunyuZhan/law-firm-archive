package com.lawfirm.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交注解
 * 
 * 功能：
 * - 防止表单重复提交
 * - 可配置间隔时间
 * - 支持自定义提示消息
 * 
 * 使用示例：
 * <pre>
 * &#64;RepeatSubmit(interval = 5)
 * &#64;PostMapping("/create")
 * public Result&lt;ContractDTO&gt; createContract(@RequestBody CreateContractCommand cmd) { ... }
 * </pre>
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 间隔时间
     * 在此时间内不允许重复提交
     */
    long interval() default 3;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 提示消息
     */
    String message() default "请勿重复提交";

    /**
     * 去重键的生成策略
     */
    KeyStrategy keyStrategy() default KeyStrategy.TOKEN;

    /**
     * 自定义键（当keyStrategy为CUSTOM时使用，支持SpEL表达式）
     */
    String key() default "";

    /**
     * 去重键生成策略
     */
    enum KeyStrategy {
        /**
         * 使用请求Token（从Header获取）
         */
        TOKEN,
        /**
         * 使用请求参数的哈希值
         */
        PARAMS,
        /**
         * 使用用户ID + 方法名
         */
        USER_METHOD,
        /**
         * 自定义（使用key属性的SpEL表达式）
         */
        CUSTOM
    }
}
