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
 * 校验规则：
 * - 2-30个字符
 * - 支持中文、少数民族名字中的点号
 * - 不支持数字和特殊字符
 * 
 * @author Kiro-1
 * @since 2026-01-10
 */
@Documented
@Constraint(validatedBy = ChineseNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChineseName {
    
    String message() default "姓名格式不正确";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 是否允许为空
     */
    boolean nullable() default true;
    
    /**
     * 最小长度
     */
    int min() default 2;
    
    /**
     * 最大长度
     */
    int max() default 30;
}

