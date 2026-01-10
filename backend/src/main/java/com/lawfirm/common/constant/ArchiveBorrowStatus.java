package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 档案借阅状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class ArchiveBorrowStatus {

    private ArchiveBorrowStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 借阅状态常量 ============
    /** 待审批 */
    public static final String PENDING = "PENDING";
    /** 已批准 */
    public static final String APPROVED = "APPROVED";
    /** 已拒绝 */
    public static final String REJECTED = "REJECTED";
    /** 借出中 */
    public static final String BORROWED = "BORROWED";
    /** 已归还 */
    public static final String RETURNED = "RETURNED";
    /** 逾期 */
    public static final String OVERDUE = "OVERDUE";

    // ============ 档案状态常量 ============
    /** 已入库 */
    public static final String ARCHIVE_STORED = "STORED";
    /** 借出中 */
    public static final String ARCHIVE_BORROWED = "BORROWED";

    // ============ 归还状态常量 ============
    /** 完好 */
    public static final String CONDITION_GOOD = "GOOD";
    /** 损坏 */
    public static final String CONDITION_DAMAGED = "DAMAGED";
    /** 遗失 */
    public static final String CONDITION_LOST = "LOST";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();
    private static final Map<String, String> CONDITION_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(PENDING, "待审批");
        STATUS_NAME_MAP.put(APPROVED, "已批准");
        STATUS_NAME_MAP.put(REJECTED, "已拒绝");
        STATUS_NAME_MAP.put(BORROWED, "借出中");
        STATUS_NAME_MAP.put(RETURNED, "已归还");
        STATUS_NAME_MAP.put(OVERDUE, "逾期");

        CONDITION_NAME_MAP.put(CONDITION_GOOD, "完好");
        CONDITION_NAME_MAP.put(CONDITION_DAMAGED, "损坏");
        CONDITION_NAME_MAP.put(CONDITION_LOST, "遗失");
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
     * 获取归还状态名称
     */
    public static String getConditionName(String condition) {
        if (condition == null) {
            return null;
        }
        return CONDITION_NAME_MAP.getOrDefault(condition, condition);
    }

    /**
     * 检查状态是否有效
     */
    public static boolean isValid(String status) {
        return STATUS_NAME_MAP.containsKey(status);
    }

    /**
     * 检查是否可以审批
     */
    public static boolean canApprove(String status) {
        return PENDING.equals(status);
    }

    /**
     * 检查是否可以借出
     */
    public static boolean canBorrow(String status) {
        return APPROVED.equals(status);
    }

    /**
     * 检查是否可以归还
     */
    public static boolean canReturn(String status) {
        return BORROWED.equals(status) || OVERDUE.equals(status);
    }
}

