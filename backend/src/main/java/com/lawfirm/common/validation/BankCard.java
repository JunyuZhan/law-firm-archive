package com.lawfirm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 银行卡号校验注解
 * 
 * 支持16-19位银行卡号，可选Luhn算法校验
 * 
 * @author Kiro-1
 * @since 2026-01-10
 */
@Documented
@Constraint(validatedBy = BankCardValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BankCard {
    
    String message() default "银行卡号格式不正确";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 是否允许为空
     */
    boolean nullable() default true;
    
    /**
     * 是否验证Luhn校验位
     */
    boolean luhn() default false;
}

