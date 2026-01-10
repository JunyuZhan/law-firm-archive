package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 合同状态常量类
 * 解决P3问题：状态硬编码字符串
 */
public final class ContractStatus {

    private ContractStatus() {
        // 私有构造函数，防止实例化
    }

    // ============ 状态常量 ============
    /** 草稿 */
    public static final String DRAFT = "DRAFT";
    /** 待审批 */
    public static final String PENDING = "PENDING";
    /** 生效中 */
    public static final String ACTIVE = "ACTIVE";
    /** 已拒绝 */
    public static final String REJECTED = "REJECTED";
    /** 已终止 */
    public static final String TERMINATED = "TERMINATED";
    /** 已完成 */
    public static final String COMPLETED = "COMPLETED";
    /** 已过期 */
    public static final String EXPIRED = "EXPIRED";

    // ============ 状态名称映射 ============
    private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

    static {
        STATUS_NAME_MAP.put(DRAFT, "草稿");
        STATUS_NAME_MAP.put(PENDING, "待审批");
        STATUS_NAME_MAP.put(ACTIVE, "生效中");
        STATUS_NAME_MAP.put(REJECTED, "已拒绝");
        STATUS_NAME_MAP.put(TERMINATED, "已终止");
        STATUS_NAME_MAP.put(COMPLETED, "已完成");
        STATUS_NAME_MAP.put(EXPIRED, "已过期");
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
     * 检查是否可以修改
     * @param status 当前状态
     * @return 是否可以修改
     */
    public static boolean canModify(String status) {
        return DRAFT.equals(status) || REJECTED.equals(status);
    }

    /**
     * 检查是否可以提交审批
     * @param status 当前状态
     * @return 是否可以提交
     */
    public static boolean canSubmit(String status) {
        return DRAFT.equals(status) || REJECTED.equals(status);
    }

    /**
     * 检查是否可以终止
     * @param status 当前状态
     * @return 是否可以终止
     */
    public static boolean canTerminate(String status) {
        return ACTIVE.equals(status);
    }
}

