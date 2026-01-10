package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 提成状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class CommissionStatus {

    private CommissionStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 状态常量 ============
    /** 已计算 */
    public static final String CALCULATED = "CALCULATED";
    /** 已审批 */
    public static final String APPROVED = "APPROVED";
    /** 已发放 */
    public static final String PAID = "PAID";
    /** 授薪豁免 */
    public static final String SALARIED_EXEMPT = "SALARIED_EXEMPT";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(CALCULATED, "已计算");
        STATUS_NAME_MAP.put(APPROVED, "已审批");
        STATUS_NAME_MAP.put(PAID, "已发放");
        STATUS_NAME_MAP.put(SALARIED_EXEMPT, "授薪豁免");
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
     * 检查是否可以审批
     * @param status 当前状态
     * @return 是否可以审批
     */
    public static boolean canApprove(String status) {
        return CALCULATED.equals(status);
    }

    /**
     * 检查是否可以发放
     * @param status 当前状态
     * @return 是否可以发放
     */
    public static boolean canPay(String status) {
        return APPROVED.equals(status);
    }

    /**
     * 检查是否已发放
     * @param status 当前状态
     * @return 是否已发放
     */
    public static boolean isPaid(String status) {
        return PAID.equals(status);
    }

    /**
     * 检查是否授薪豁免
     * @param status 当前状态
     * @return 是否授薪豁免
     */
    public static boolean isSalariedExempt(String status) {
        return SALARIED_EXEMPT.equals(status);
    }
}

