package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 工资表状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class PayrollStatus {

    private PayrollStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 工资表状态常量 ============
    /** 草稿 */
    public static final String DRAFT = "DRAFT";
    /** 待确认（员工确认） */
    public static final String PENDING_CONFIRM = "PENDING_CONFIRM";
    /** 待审批 */
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    /** 已确认 */
    public static final String CONFIRMED = "CONFIRMED";
    /** 财务已确认 */
    public static final String FINANCE_CONFIRMED = "FINANCE_CONFIRMED";
    /** 已审批 */
    public static final String APPROVED = "APPROVED";
    /** 已拒绝 */
    public static final String REJECTED = "REJECTED";
    /** 已发放 */
    public static final String ISSUED = "ISSUED";

    // ============ 工资明细确认状态常量 ============
    /** 待确认 */
    public static final String ITEM_PENDING = "PENDING";
    /** 已确认 */
    public static final String ITEM_CONFIRMED = "CONFIRMED";
    /** 已拒绝 */
    public static final String ITEM_REJECTED = "REJECTED";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();
    private static final Map<String, String> CONFIRM_STATUS_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(DRAFT, "草稿");
        STATUS_NAME_MAP.put(PENDING_CONFIRM, "待确认");
        STATUS_NAME_MAP.put(PENDING_APPROVAL, "待审批");
        STATUS_NAME_MAP.put(CONFIRMED, "已确认");
        STATUS_NAME_MAP.put(FINANCE_CONFIRMED, "财务已确认");
        STATUS_NAME_MAP.put(APPROVED, "已审批");
        STATUS_NAME_MAP.put(REJECTED, "已拒绝");
        STATUS_NAME_MAP.put(ISSUED, "已发放");

        CONFIRM_STATUS_NAME_MAP.put(ITEM_PENDING, "待确认");
        CONFIRM_STATUS_NAME_MAP.put(ITEM_CONFIRMED, "已确认");
        CONFIRM_STATUS_NAME_MAP.put(ITEM_REJECTED, "已拒绝");
    }

    /**
     * 获取工资表状态名称
     */
    public static String getStatusName(String status) {
        if (status == null) {
            return null;
        }
        return STATUS_NAME_MAP.getOrDefault(status, status);
    }

    /**
     * 获取确认状态名称
     */
    public static String getConfirmStatusName(String status) {
        if (status == null) {
            return null;
        }
        return CONFIRM_STATUS_NAME_MAP.getOrDefault(status, status);
    }

    /**
     * 检查工资表状态是否有效
     */
    public static boolean isValidStatus(String status) {
        return STATUS_NAME_MAP.containsKey(status);
    }

    /**
     * 检查工资表是否可编辑
     */
    public static boolean canEdit(String status) {
        return DRAFT.equals(status) || PENDING_CONFIRM.equals(status) || REJECTED.equals(status);
    }

    /**
     * 检查工资表是否可提交审批
     */
    public static boolean canSubmitApproval(String status) {
        return DRAFT.equals(status);
    }
}

