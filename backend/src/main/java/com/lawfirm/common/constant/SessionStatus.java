package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 会话状态常量类
 * 解决P3问题：状态硬编码字符串（问题527）
 */
public final class SessionStatus {

    private SessionStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 状态常量 ============
    /** 活跃 */
    public static final String ACTIVE = "ACTIVE";
    /** 已登出 */
    public static final String LOGGED_OUT = "LOGGED_OUT";
    /** 已过期 */
    public static final String EXPIRED = "EXPIRED";
    /** 强制登出 */
    public static final String FORCED_LOGOUT = "FORCED_LOGOUT";

    // ============ 设备类型常量 ============
    /** 网页 */
    public static final String DEVICE_WEB = "WEB";
    /** 移动端 */
    public static final String DEVICE_MOBILE = "MOBILE";
    /** 桌面端 */
    public static final String DEVICE_DESKTOP = "DESKTOP";
    /** API */
    public static final String DEVICE_API = "API";
    /** 未知 */
    public static final String DEVICE_UNKNOWN = "UNKNOWN";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();
    private static final Map<String, String> DEVICE_TYPE_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(ACTIVE, "活跃");
        STATUS_NAME_MAP.put(LOGGED_OUT, "已登出");
        STATUS_NAME_MAP.put(EXPIRED, "已过期");
        STATUS_NAME_MAP.put(FORCED_LOGOUT, "强制登出");

        DEVICE_TYPE_NAME_MAP.put(DEVICE_WEB, "网页");
        DEVICE_TYPE_NAME_MAP.put(DEVICE_MOBILE, "移动端");
        DEVICE_TYPE_NAME_MAP.put(DEVICE_DESKTOP, "桌面端");
        DEVICE_TYPE_NAME_MAP.put(DEVICE_API, "API");
        DEVICE_TYPE_NAME_MAP.put(DEVICE_UNKNOWN, "未知");
    }

    /**
     * 获取状态名称
     * @param status 状态代码
     * @return 状态名称，如果不存在则返回原值
     */
    public static String getStatusName(String status) {
        if (status == null) {
            return null;
        }
        return STATUS_NAME_MAP.getOrDefault(status, status);
    }

    /**
     * 获取设备类型名称
     * @param deviceType 设备类型代码
     * @return 设备类型名称，如果不存在则返回原值
     */
    public static String getDeviceTypeName(String deviceType) {
        if (deviceType == null) {
            return null;
        }
        return DEVICE_TYPE_NAME_MAP.getOrDefault(deviceType, deviceType);
    }

    /**
     * 检查会话是否活跃
     * @param status 当前状态
     * @return 是否活跃
     */
    public static boolean isActive(String status) {
        return ACTIVE.equals(status);
    }

    /**
     * 检查是否可以强制登出
     * @param status 当前状态
     * @return 是否可以强制登出
     */
    public static boolean canForceLogout(String status) {
        return ACTIVE.equals(status);
    }
}

