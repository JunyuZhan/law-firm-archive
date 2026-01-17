package com.lawfirm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 手机号校验器
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    /**
     * 中国大陆手机号正则
     * 1开头，第二位为3-9，共11位
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private boolean nullable;

    @Override
    public void initialize(Phone constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 允许为空
        if (!StringUtils.hasText(value)) {
            return nullable;
        }
        
        // 移除空格和连字符
        String cleanedPhone = value.replaceAll("[\\s-]", "");
        
        return PHONE_PATTERN.matcher(cleanedPhone).matches();
    }
}

