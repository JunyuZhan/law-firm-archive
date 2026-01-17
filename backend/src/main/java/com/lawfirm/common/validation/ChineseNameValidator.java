package com.lawfirm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 中文姓名校验器
 * 
 * 支持：
 * - 中文汉字
 * - 少数民族名字中的点号（·）
 * - 繁体字
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
public class ChineseNameValidator implements ConstraintValidator<ChineseName, String> {

    /**
     * 中文姓名正则
     * 支持汉字和中间的点号（用于少数民族名字，如：买买提·艾力）
     */
    private static final Pattern CHINESE_NAME_PATTERN = 
            Pattern.compile("^[\\u4e00-\\u9fa5]+(·[\\u4e00-\\u9fa5]+)*$");

    private boolean nullable;
    private int min;
    private int max;

    @Override
    public void initialize(ChineseName constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 允许为空
        if (!StringUtils.hasText(value)) {
            return nullable;
        }

        String name = value.trim();

        // 长度校验（不计算点号）
        String nameWithoutDot = name.replace("·", "");
        if (nameWithoutDot.length() < min || nameWithoutDot.length() > max) {
            return false;
        }

        // 格式校验
        return CHINESE_NAME_PATTERN.matcher(name).matches();
    }
}

