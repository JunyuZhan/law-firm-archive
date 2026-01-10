package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 任务状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class TaskStatus {

    private TaskStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 状态常量 ============
    /** 待办 (TODO) */
    public static final String TODO = "TODO";
    /** 待处理 */
    public static final String PENDING = "PENDING";
    /** 进行中 */
    public static final String IN_PROGRESS = "IN_PROGRESS";
    /** 待验收 */
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    /** 已完成 */
    public static final String COMPLETED = "COMPLETED";
    /** 已取消 */
    public static final String CANCELLED = "CANCELLED";
    /** 已逾期 */
    public static final String OVERDUE = "OVERDUE";

    // ============ 优先级常量 ============
    /** 低优先级 */
    public static final String PRIORITY_LOW = "LOW";
    /** 中优先级 */
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    /** 高优先级 */
    public static final String PRIORITY_HIGH = "HIGH";
    /** 紧急 */
    public static final String PRIORITY_URGENT = "URGENT";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();
    private static final Map<String, String> PRIORITY_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(TODO, "待办");
        STATUS_NAME_MAP.put(PENDING, "待处理");
        STATUS_NAME_MAP.put(IN_PROGRESS, "进行中");
        STATUS_NAME_MAP.put(PENDING_REVIEW, "待验收");
        STATUS_NAME_MAP.put(COMPLETED, "已完成");
        STATUS_NAME_MAP.put(CANCELLED, "已取消");
        STATUS_NAME_MAP.put(OVERDUE, "已逾期");

        PRIORITY_NAME_MAP.put(PRIORITY_LOW, "低");
        PRIORITY_NAME_MAP.put(PRIORITY_MEDIUM, "中");
        PRIORITY_NAME_MAP.put(PRIORITY_HIGH, "高");
        PRIORITY_NAME_MAP.put(PRIORITY_URGENT, "紧急");
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
     * 获取优先级名称
     * @param priority 优先级代码
     * @return 优先级名称，如果不存在则返回原值
     */
    public static String getPriorityName(String priority) {
        if (priority == null) {
            return null;
        }
        return PRIORITY_NAME_MAP.getOrDefault(priority, priority);
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
     * 检查任务是否活跃（未完成、未取消）
     * @param status 当前状态
     * @return 是否活跃
     */
    public static boolean isActive(String status) {
        return PENDING.equals(status) || IN_PROGRESS.equals(status) || OVERDUE.equals(status);
    }

    /**
     * 检查是否可以完成
     * @param status 当前状态
     * @return 是否可以完成
     */
    public static boolean canComplete(String status) {
        return PENDING.equals(status) || IN_PROGRESS.equals(status) || OVERDUE.equals(status);
    }

    /**
     * 检查是否可以取消
     * @param status 当前状态
     * @return 是否可以取消
     */
    public static boolean canCancel(String status) {
        return PENDING.equals(status) || IN_PROGRESS.equals(status);
    }
}

