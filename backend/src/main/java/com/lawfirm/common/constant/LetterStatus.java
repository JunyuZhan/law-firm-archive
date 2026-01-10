package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 介绍信状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class LetterStatus {

    private LetterStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 模板状态常量 ============
    /** 启用 */
    public static final String TEMPLATE_ACTIVE = "ACTIVE";
    /** 停用 */
    public static final String TEMPLATE_DISABLED = "DISABLED";

    // ============ 申请状态常量 ============
    /** 待审批 */
    public static final String PENDING = "PENDING";
    /** 已通过 */
    public static final String APPROVED = "APPROVED";
    /** 已拒绝 */
    public static final String REJECTED = "REJECTED";
    /** 已退回 */
    public static final String RETURNED = "RETURNED";
    /** 已签发 */
    public static final String ISSUED = "ISSUED";
    /** 已打印 */
    public static final String PRINTED = "PRINTED";
    /** 已领取 */
    public static final String RECEIVED = "RECEIVED";
    /** 已取消 */
    public static final String CANCELLED = "CANCELLED";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();
    private static final Map<String, String> TEMPLATE_STATUS_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(PENDING, "待审批");
        STATUS_NAME_MAP.put(APPROVED, "已批准");
        STATUS_NAME_MAP.put(REJECTED, "已拒绝");
        STATUS_NAME_MAP.put(RETURNED, "已退回");
        STATUS_NAME_MAP.put(ISSUED, "已签发");
        STATUS_NAME_MAP.put(PRINTED, "已打印");
        STATUS_NAME_MAP.put(RECEIVED, "已领取");
        STATUS_NAME_MAP.put(CANCELLED, "已取消");

        TEMPLATE_STATUS_NAME_MAP.put(TEMPLATE_ACTIVE, "启用");
        TEMPLATE_STATUS_NAME_MAP.put(TEMPLATE_DISABLED, "停用");
    }

    /**
     * 获取申请状态名称
     */
    public static String getStatusName(String status) {
        if (status == null) {
            return null;
        }
        return STATUS_NAME_MAP.getOrDefault(status, status);
    }

    /**
     * 获取模板状态名称
     */
    public static String getTemplateStatusName(String status) {
        if (status == null) {
            return null;
        }
        return TEMPLATE_STATUS_NAME_MAP.getOrDefault(status, status);
    }

    /**
     * 检查是否可以审批
     */
    public static boolean canApprove(String status) {
        return PENDING.equals(status);
    }

    /**
     * 检查是否可以重新提交
     */
    public static boolean canResubmit(String status) {
        return RETURNED.equals(status) || REJECTED.equals(status);
    }
}

