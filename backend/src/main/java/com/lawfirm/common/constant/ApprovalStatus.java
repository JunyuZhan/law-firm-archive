package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 审批状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class ApprovalStatus {

    private ApprovalStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 状态常量 ============
    /** 待审批 */
    public static final String PENDING = "PENDING";
    /** 已通过 */
    public static final String APPROVED = "APPROVED";
    /** 已拒绝 */
    public static final String REJECTED = "REJECTED";
    /** 已取消 */
    public static final String CANCELLED = "CANCELLED";
    /** 已撤回 */
    public static final String WITHDRAWN = "WITHDRAWN";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(PENDING, "待审批");
        STATUS_NAME_MAP.put(APPROVED, "已通过");
        STATUS_NAME_MAP.put(REJECTED, "已拒绝");
        STATUS_NAME_MAP.put(CANCELLED, "已取消");
        STATUS_NAME_MAP.put(WITHDRAWN, "已撤回");
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
     * 检查是否已完成（通过或拒绝）
     * @param status 当前状态
     * @return 是否已完成
     */
    public static boolean isCompleted(String status) {
        return APPROVED.equals(status) || REJECTED.equals(status);
    }

    /**
     * 检查是否可以审批
     * @param status 当前状态
     * @return 是否可以审批
     */
    public static boolean canApprove(String status) {
        return PENDING.equals(status);
    }

    /**
     * 检查是否可以撤回
     * @param status 当前状态
     * @return 是否可以撤回
     */
    public static boolean canWithdraw(String status) {
        return PENDING.equals(status);
    }
}

