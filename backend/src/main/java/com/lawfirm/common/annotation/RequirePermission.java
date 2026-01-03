package com.lawfirm.common.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 
 * 使用示例：
 * @RequirePermission("sys:user:create")
 * @RequirePermission(value = {"sys:user:edit", "sys:user:delete"}, logical = Logical.OR)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限编码
     */
    String[] value();

    /**
     * 多个权限的逻辑关系
     */
    Logical logical() default Logical.AND;

    enum Logical {
        AND, OR
    }
}

