package com.lawfirm.common.util;

import java.util.Map;

/**
 * 报表参数工具类
 * 解决P3问题：query*Data方法中重复的参数解析逻辑（问题576）
 */
public final class ReportParameterUtils {

    private ReportParameterUtils() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取字符串参数
     * @param parameters 参数Map
     * @param key 参数键
     * @return 参数值，如果不存在则返回null
     */
    public static String getString(Map<String, Object> parameters, String key) {
        if (parameters == null || key == null) {
            return null;
        }
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取字符串参数，带默认值
     * @param parameters 参数Map
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public static String getString(Map<String, Object> parameters, String key, String defaultValue) {
        String value = getString(parameters, key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取Long参数
     * @param parameters 参数Map
     * @param key 参数键
     * @return 参数值，如果不存在则返回null
     */
    public static Long getLong(Map<String, Object> parameters, String key) {
        if (parameters == null || key == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取Long参数，带默认值
     * @param parameters 参数Map
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public static Long getLong(Map<String, Object> parameters, String key, Long defaultValue) {
        Long value = getLong(parameters, key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取Integer参数
     * @param parameters 参数Map
     * @param key 参数键
     * @return 参数值，如果不存在则返回null
     */
    public static Integer getInteger(Map<String, Object> parameters, String key) {
        if (parameters == null || key == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取Integer参数，带默认值
     * @param parameters 参数Map
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public static Integer getInteger(Map<String, Object> parameters, String key, Integer defaultValue) {
        Integer value = getInteger(parameters, key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取Boolean参数
     * @param parameters 参数Map
     * @param key 参数键
     * @return 参数值，如果不存在则返回null
     */
    public static Boolean getBoolean(Map<String, Object> parameters, String key) {
        if (parameters == null || key == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = value.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }

    /**
     * 获取Boolean参数，带默认值
     * @param parameters 参数Map
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public static Boolean getBoolean(Map<String, Object> parameters, String key, Boolean defaultValue) {
        Boolean value = getBoolean(parameters, key);
        return value != null ? value : defaultValue;
    }

    // ============ 常用参数名常量 ============

    /** 开始日期参数 */
    public static final String PARAM_START_DATE = "startDate";
    /** 结束日期参数 */
    public static final String PARAM_END_DATE = "endDate";
    /** 客户ID参数 */
    public static final String PARAM_CLIENT_ID = "clientId";
    /** 项目ID参数 */
    public static final String PARAM_MATTER_ID = "matterId";
    /** 用户ID参数 */
    public static final String PARAM_USER_ID = "userId";
    /** 部门ID参数 */
    public static final String PARAM_DEPARTMENT_ID = "departmentId";
    /** 状态参数 */
    public static final String PARAM_STATUS = "status";
    /** 类型参数 */
    public static final String PARAM_TYPE = "type";
    /** 限制数量参数 */
    public static final String PARAM_LIMIT = "limit";
}

