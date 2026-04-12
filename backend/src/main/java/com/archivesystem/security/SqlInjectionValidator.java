package com.archivesystem.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * SQL注入验证器.
 * 用于检测和防止SQL注入攻击
 * @author junyuzhan
 */
@Slf4j
@Component
public class SqlInjectionValidator {

    // SQL注入关键字模式
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
            // 注释
            Pattern.compile("--.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/\\*.*?\\*/", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            // UNION注入
            Pattern.compile("\\bunion\\b.*\\bselect\\b", Pattern.CASE_INSENSITIVE),
            // OR/AND注入
            Pattern.compile("\\bor\\b\\s+\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\band\\b\\s+\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bor\\b\\s+'[^']*'\\s*=\\s*'[^']*'", Pattern.CASE_INSENSITIVE),
            // 时间延迟注入
            Pattern.compile("\\bsleep\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bbenchmark\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwaitfor\\b\\s+\\bdelay\\b", Pattern.CASE_INSENSITIVE),
            // 堆叠查询
            Pattern.compile(";\\s*(?:select|insert|update|delete|drop|truncate|alter|create)", Pattern.CASE_INSENSITIVE),
            // 系统命令执行
            Pattern.compile("\\bexec\\b.*\\bxp_", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bexec\\b.*\\bsp_", Pattern.CASE_INSENSITIVE),
            // 信息泄露函数
            Pattern.compile("\\b(?:database|user|version)\\s*\\(\\s*\\)", Pattern.CASE_INSENSITIVE),
            // DROP/TRUNCATE等危险操作
            Pattern.compile("\\b(?:drop|truncate|alter)\\b\\s+\\b(?:table|database|schema)\\b", Pattern.CASE_INSENSITIVE),
            // INTO OUTFILE/DUMPFILE
            Pattern.compile("\\binto\\b\\s+(?:outfile|dumpfile)", Pattern.CASE_INSENSITIVE),
            // LOAD_FILE
            Pattern.compile("\\bload_file\\s*\\(", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 验证输入是否安全.
     * @param input 输入字符串
     * @return true表示安全，false表示可能存在SQL注入
     */
    public boolean isSafe(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        // 检查危险字符组合
        if (containsDangerousPatterns(input)) {
            log.warn("检测到可能的SQL注入攻击: {}", maskSensitiveContent(input));
            return false;
        }

        return true;
    }

    /**
     * 检查是否包含危险模式.
     */
    private boolean containsDangerousPatterns(String input) {
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理输入，移除潜在危险内容.
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }

        String result = input;
        
        // 移除SQL注释
        result = result.replaceAll("--.*", "");
        result = result.replaceAll("/\\*.*?\\*/", "");
        
        // 转义单引号
        result = result.replace("'", "''");
        
        // 移除分号（防止堆叠查询）
        result = result.replace(";", "");
        
        return result;
    }

    /**
     * 验证排序字段名（防止ORDER BY注入）.
     */
    public boolean isValidOrderByField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        
        // 只允许字母、数字、下划线和点
        return fieldName.matches("^[a-zA-Z0-9_.]+$");
    }

    /**
     * 验证排序方向.
     */
    public boolean isValidOrderDirection(String direction) {
        if (direction == null || direction.isEmpty()) {
            return true; // 允许空值（使用默认排序）
        }
        
        String upper = direction.toUpperCase().trim();
        return "ASC".equals(upper) || "DESC".equals(upper);
    }

    /**
     * 遮蔽敏感内容用于日志记录.
     */
    private String maskSensitiveContent(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() > 100) {
            return input.substring(0, 100) + "...(truncated)";
        }
        return input;
    }
}
