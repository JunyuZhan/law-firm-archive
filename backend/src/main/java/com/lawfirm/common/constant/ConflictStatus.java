package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 冲突检查状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class ConflictStatus {

    private ConflictStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 状态常量 ============
    /** 待检查 */
    public static final String PENDING = "PENDING";
    /** 检查中 */
    public static final String CHECKING = "CHECKING";
    /** 已通过 */
    public static final String PASSED = "PASSED";
    /** 存在冲突 */
    public static final String CONFLICT = "CONFLICT";
    /** 已豁免 */
    public static final String WAIVED = "WAIVED";
    /** 豁免待审批 */
    public static final String EXEMPTION_PENDING = "EXEMPTION_PENDING";
    /** 已拒绝 */
    public static final String REJECTED = "REJECTED";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(PENDING, "待检查");
        STATUS_NAME_MAP.put(CHECKING, "检查中");
        STATUS_NAME_MAP.put(PASSED, "已通过");
        STATUS_NAME_MAP.put(CONFLICT, "存在冲突");
        STATUS_NAME_MAP.put(WAIVED, "已豁免");
        STATUS_NAME_MAP.put(EXEMPTION_PENDING, "豁免待审批");
        STATUS_NAME_MAP.put(REJECTED, "已拒绝");
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
     * 检查状态是否有效
     * @param status 状态代码
     * @return 是否有效
     */
    public static boolean isValid(String status) {
        return STATUS_NAME_MAP.containsKey(status);
    }

    /**
     * 检查是否可以申请豁免
     * @param status 当前状态
     * @return 是否可以申请豁免
     */
    public static boolean canApplyExemption(String status) {
        return CONFLICT.equals(status) || PENDING.equals(status);
    }

    /**
     * 检查是否存在冲突
     * @param status 当前状态
     * @return 是否存在冲突
     */
    public static boolean hasConflict(String status) {
        return CONFLICT.equals(status);
    }

    /**
     * 检查是否已通过（无冲突或已豁免）
     * @param status 当前状态
     * @return 是否已通过
     */
    public static boolean isPassed(String status) {
        return PASSED.equals(status) || WAIVED.equals(status);
    }

    /**
     * 检查是否待审批
     * @param status 当前状态
     * @return 是否待审批
     */
    public static boolean isPending(String status) {
        return PENDING.equals(status);
    }

    /**
     * 检查是否豁免待审批
     * @param status 当前状态
     * @return 是否豁免待审批
     */
    public static boolean isExemptionPending(String status) {
        return EXEMPTION_PENDING.equals(status);
    }
}

