package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 数据交接状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class DataHandoverStatus {

    private DataHandoverStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 交接单状态常量 ============
    /** 待审批 */
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    /** 审批通过待执行 */
    public static final String APPROVED = "APPROVED";
    /** 已拒绝 */
    public static final String REJECTED = "REJECTED";
    /** 已确认 */
    public static final String CONFIRMED = "CONFIRMED";
    /** 已取消 */
    public static final String CANCELLED = "CANCELLED";

    // ============ 交接明细状态常量 ============
    /** 待处理 */
    public static final String DETAIL_PENDING = "PENDING";
    /** 已完成 */
    public static final String DETAIL_DONE = "DONE";
    /** 失败 */
    public static final String DETAIL_FAILED = "FAILED";

    // ============ 交接类型常量 ============
    /** 离职交接 */
    public static final String TYPE_RESIGNATION = "RESIGNATION";
    /** 项目移交 */
    public static final String TYPE_PROJECT = "PROJECT";
    /** 客户移交 */
    public static final String TYPE_CLIENT = "CLIENT";
    /** 案源移交 */
    public static final String TYPE_LEAD = "LEAD";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();
    private static final Map<String, String> DETAIL_STATUS_NAME_MAP = new HashMap<>();
    private static final Map<String, String> TYPE_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(PENDING_APPROVAL, "待审批");
        STATUS_NAME_MAP.put(APPROVED, "审批通过待执行");
        STATUS_NAME_MAP.put(REJECTED, "已拒绝");
        STATUS_NAME_MAP.put(CONFIRMED, "已确认");
        STATUS_NAME_MAP.put(CANCELLED, "已取消");

        DETAIL_STATUS_NAME_MAP.put(DETAIL_PENDING, "待处理");
        DETAIL_STATUS_NAME_MAP.put(DETAIL_DONE, "已完成");
        DETAIL_STATUS_NAME_MAP.put(DETAIL_FAILED, "失败");

        TYPE_NAME_MAP.put(TYPE_RESIGNATION, "离职交接");
        TYPE_NAME_MAP.put(TYPE_PROJECT, "项目移交");
        TYPE_NAME_MAP.put(TYPE_CLIENT, "客户移交");
        TYPE_NAME_MAP.put(TYPE_LEAD, "案源移交");
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
     * 获取明细状态名称
     */
    public static String getDetailStatusName(String status) {
        if (status == null) {
            return null;
        }
        return DETAIL_STATUS_NAME_MAP.getOrDefault(status, status);
    }

    /**
     * 获取交接类型名称
     */
    public static String getTypeName(String type) {
        if (type == null) {
            return null;
        }
        return TYPE_NAME_MAP.getOrDefault(type, type);
    }

    /**
     * 检查状态是否有效
     */
    public static boolean isValid(String status) {
        return STATUS_NAME_MAP.containsKey(status);
    }

    /**
     * 检查是否可以确认执行
     */
    public static boolean canConfirm(String status) {
        return APPROVED.equals(status);
    }

    /**
     * 检查是否可以取消
     */
    public static boolean canCancel(String status) {
        return PENDING_APPROVAL.equals(status) || APPROVED.equals(status);
    }
}

