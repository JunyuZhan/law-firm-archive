package com.lawfirm.common.annotation;

import java.lang.annotation.*;

/**
 * API速率限制注解
 * 用于防止接口被恶意频繁调用
 *
 * 使用示例：
 * @RateLimit(key = "report:generate", limit = 10, period = 60) // 每分钟最多10次
 * @RateLimit(key = "sms:send", limit = 5, period = 300) // 5分钟最多5次
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流的key标识，会和用户ID组合使用
     * 格式：rate_limit:{key}:{userId}
     */
    String key();

    /**
     * 时间周期内允许的最大请求次数
     */
    int limit() default 100;

    /**
     * 时间周期（秒）
     */
    int period() default 60;

    /**
     * 限流提示消息
     */
    String message() default "操作过于频繁，请稍后再试";

    /**
     * 限流类型
     */
    Type type() default Type.USER;

    /**
     * 限流类型枚举
     */
    enum Type {
        /**
         * 按用户限流
         */
        USER,
        /**
         * 按IP限流
         */
        IP,
        /**
         * 全局限流
         */
        GLOBAL
    }
}

