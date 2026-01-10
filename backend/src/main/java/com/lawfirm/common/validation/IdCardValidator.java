package com.lawfirm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 身份证号校验器
 * 
 * 支持18位身份证号，包含：
 * - 格式验证（6位地区码 + 8位生日 + 3位顺序码 + 1位校验码）
 * - 校验位验证（使用ISO 7064:1983, MOD 11-2算法）
 * 
 * @author Kiro-1
 * @since 2026-01-10
 */
public class IdCardValidator implements ConstraintValidator<IdCard, String> {

    /**
     * 18位身份证正则
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    /**
     * 加权因子
     */
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /**
     * 校验码对照表
     */
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private boolean nullable;
    private boolean strict;

    @Override
    public void initialize(IdCard constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
        this.strict = constraintAnnotation.strict();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 允许为空
        if (!StringUtils.hasText(value)) {
            return nullable;
        }

        String idCard = value.trim().toUpperCase();

        // 基本格式验证
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return false;
        }

        // 严格模式：验证校验位
        if (strict) {
            return validateCheckCode(idCard);
        }

        return true;
    }

    /**
     * 验证校验位
     */
    private boolean validateCheckCode(String idCard) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHTS[i];
        }
        int mod = sum % 11;
        char expectedCheckCode = CHECK_CODES[mod];
        char actualCheckCode = idCard.charAt(17);
        
        return expectedCheckCode == actualCheckCode;
    }
}

