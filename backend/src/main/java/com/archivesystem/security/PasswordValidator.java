package com.archivesystem.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码强度验证器.
 * @author junyuzhan
 */
@Component
public class PasswordValidator {

    // 最小长度
    private static final int MIN_LENGTH = 8;
    // 最大长度
    private static final int MAX_LENGTH = 50;
    
    // 常见弱密码列表
    private static final List<String> COMMON_WEAK_PASSWORDS = List.of(
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "1234567", "letmein", "trustno1", "dragon",
            "baseball", "iloveyou", "master", "sunshine", "ashley",
            "bailey", "passw0rd", "shadow", "123123", "654321",
            "password1", "admin123", "admin", "root", "test123"
    );

    // 模式
    private static final Pattern HAS_LETTER = Pattern.compile("[a-zA-Z]");
    private static final Pattern HAS_NUMBER = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>\\[\\]\\-_=+~`]");
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");

    /**
     * 验证密码.
     * @param password 密码
     * @return 验证结果
     */
    public ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.isEmpty()) {
            errors.add("密码不能为空");
            return new ValidationResult(false, errors, 0);
        }
        
        int score = 0;
        
        // 长度检查
        if (password.length() < MIN_LENGTH) {
            errors.add("密码长度至少" + MIN_LENGTH + "位");
        } else {
            score++;
            if (password.length() >= 12) {
                score++; // 额外加分
            }
        }
        
        if (password.length() > MAX_LENGTH) {
            errors.add("密码长度不能超过" + MAX_LENGTH + "位");
        }
        
        // 包含字母
        if (!HAS_LETTER.matcher(password).find()) {
            errors.add("密码必须包含字母");
        } else {
            score++;
        }
        
        // 包含数字
        if (!HAS_NUMBER.matcher(password).find()) {
            errors.add("密码必须包含数字");
        } else {
            score++;
        }
        
        // 包含特殊字符（推荐）
        if (HAS_SPECIAL.matcher(password).find()) {
            score++;
        }
        
        // 同时包含大小写字母（推荐）
        if (HAS_UPPERCASE.matcher(password).find() && HAS_LOWERCASE.matcher(password).find()) {
            score++;
        }
        
        // 弱密码检查
        if (isCommonWeakPassword(password)) {
            errors.add("密码过于简单，请使用更复杂的密码");
            score = Math.max(0, score - 2);
        }
        
        // 连续字符检查
        if (hasSequentialChars(password)) {
            score = Math.max(0, score - 1);
        }
        
        // 重复字符检查
        if (hasRepeatedChars(password)) {
            score = Math.max(0, score - 1);
        }
        
        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, score);
    }

    /**
     * 验证密码（严格模式，要求特殊字符）.
     */
    public ValidationResult validateStrict(String password) {
        ValidationResult result = validate(password);
        
        if (!HAS_SPECIAL.matcher(password).find()) {
            result.errors().add("密码必须包含特殊字符");
            return new ValidationResult(false, result.errors(), result.score());
        }
        
        return result;
    }

    /**
     * 检查是否是常见弱密码.
     */
    private boolean isCommonWeakPassword(String password) {
        String lower = password.toLowerCase();
        return COMMON_WEAK_PASSWORDS.contains(lower);
    }

    /**
     * 检查是否包含连续字符（如123, abc）.
     */
    private boolean hasSequentialChars(String password) {
        if (password.length() < 3) return false;
        
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);
            
            // 检查连续递增或递减
            if ((c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否包含重复字符（如aaa, 111）.
     */
    private boolean hasRepeatedChars(String password) {
        if (password.length() < 3) return false;
        
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) 
                && password.charAt(i + 1) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证结果记录.
     */
    public record ValidationResult(boolean isValid, List<String> errors, int score) {
        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }
        
        public String getAllErrors() {
            return String.join("; ", errors);
        }
    }
}
