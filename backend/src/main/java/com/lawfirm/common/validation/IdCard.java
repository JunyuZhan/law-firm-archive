package com.lawfirm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 身份证号校验注解
 * 
 * 支持18位身份证号，包含校验位验证
 * 
 * 使用示例：
 * <pre>
 * public class ClientDTO {
 *     @IdCard(message = "身份证号格式不正确")
 *     private String idCard;
 * }
 * </pre>
 * 
 * @author Kiro-1
 * @since 2026-01-10
 */
@Documented
@Constraint(validatedBy = IdCardValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdCard {
    
    String message() default "身份证号格式不正确";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 是否允许为空
     */
    boolean nullable() default true;
    
    /**
     * 是否验证校验位（严格模式）
     */
    boolean strict() default true;
}

