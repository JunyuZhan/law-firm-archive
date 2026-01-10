package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 员工状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class EmployeeStatus {

    private EmployeeStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 工作状态常量 ============
    /** 在职 */
    public static final String ACTIVE = "ACTIVE";
    /** 试用期 */
    public static final String PROBATION = "PROBATION";
    /** 离职 */
    public static final String RESIGNED = "RESIGNED";
    /** 停薪留职 */
    public static final String SUSPENDED = "SUSPENDED";
    /** 退休 */
    public static final String RETIRED = "RETIRED";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(ACTIVE, "在职");
        STATUS_NAME_MAP.put(PROBATION, "试用期");
        STATUS_NAME_MAP.put(RESIGNED, "离职");
        STATUS_NAME_MAP.put(SUSPENDED, "停薪留职");
        STATUS_NAME_MAP.put(RETIRED, "退休");
    }

    /**
     * 获取状态名称
     */
    public static String getStatusName(String status) {
        if (status == null) {
            return null;
        }
        return STATUS_NAME_MAP.getOrDefault(status, status);
    }

    /**
     * 检查状态是否有效
     */
    public static boolean isValid(String status) {
        return STATUS_NAME_MAP.containsKey(status);
    }

    /**
     * 检查员工是否在职（可发工资）
     */
    public static boolean isWorkable(String status) {
        return ACTIVE.equals(status) || PROBATION.equals(status);
    }
}

