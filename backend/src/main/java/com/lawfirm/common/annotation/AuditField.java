package com.lawfirm.common.annotation;

import java.lang.annotation.*;

/**
 * 字段审计注解
 * 
 * 用于标记需要记录字段变更的方法，自动对比变更前后的值并记录审计日志
 * 
 * 使用示例：
 * @AuditField(fields = {"status", "leadLawyerId", "actualFee"})
 * public void updateMatter(UpdateMatterCommand cmd) { ... }
 * 
 * @author system
 * @since 2026-01-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditField {

    /**
     * 需要审计的字段名列表
     * 为空时审计所有字段
     */
    String[] fields() default {};

    /**
     * 模块名称（用于日志记录）
     */
    String module() default "";

    /**
     * 操作描述
     */
    String action() default "更新";

    /**
     * 实体ID参数名（用于获取变更前的数据）
     * 默认从第一个参数的id字段获取
     */
    String idParam() default "id";

    /**
     * 实体类型（用于查询变更前的数据）
     */
    Class<?> entityClass() default Void.class;
}
