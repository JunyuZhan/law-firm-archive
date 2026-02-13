package com.archivesystem.common;

import com.archivesystem.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 参数校验工具类.
 */
public final class ValidationUtils {

    private static final Pattern ARCHIVE_NO_PATTERN = Pattern.compile("^[A-Z0-9-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private ValidationUtils() {
    }

    /**
     * 校验非空.
     */
    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验字符串非空.
     */
    public static void notBlank(String str, String message) {
        if (!StringUtils.hasText(str)) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验集合非空.
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验条件为真.
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验条件为假.
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验档案号格式.
     */
    public static void validArchiveNo(String archiveNo, String message) {
        notBlank(archiveNo, message);
        if (!ARCHIVE_NO_PATTERN.matcher(archiveNo).matches()) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验邮箱格式.
     */
    public static void validEmail(String email, String message) {
        if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验手机号格式.
     */
    public static void validPhone(String phone, String message) {
        if (StringUtils.hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验数值范围.
     */
    public static void inRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new BusinessException("400", message);
        }
    }

    /**
     * 校验字符串长度.
     */
    public static void maxLength(String str, int maxLength, String message) {
        if (str != null && str.length() > maxLength) {
            throw new BusinessException("400", message);
        }
    }
}
