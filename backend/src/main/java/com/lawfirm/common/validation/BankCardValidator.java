package com.lawfirm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 银行卡号校验器
 * 
 * 支持：
 * - 格式验证（16-19位纯数字）
 * - Luhn算法校验（可选）
 * 
 * @author Kiro-1
 * @since 2026-01-10
 */
public class BankCardValidator implements ConstraintValidator<BankCard, String> {

    /**
     * 银行卡号正则（16-19位数字）
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");

    private boolean nullable;
    private boolean luhn;

    @Override
    public void initialize(BankCard constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
        this.luhn = constraintAnnotation.luhn();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 允许为空
        if (!StringUtils.hasText(value)) {
            return nullable;
        }

        // 移除空格和连字符
        String cleanedCard = value.replaceAll("[\\s-]", "");

        // 基本格式验证
        if (!BANK_CARD_PATTERN.matcher(cleanedCard).matches()) {
            return false;
        }

        // Luhn算法验证
        if (luhn) {
            return validateLuhn(cleanedCard);
        }

        return true;
    }

    /**
     * Luhn算法验证
     * 
     * 从右向左，偶数位数字乘2（如果结果>9则减9），然后所有数字求和，
     * 如果总和能被10整除，则卡号有效
     */
    private boolean validateLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = cardNumber.charAt(i) - '0';
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return sum % 10 == 0;
    }
}

