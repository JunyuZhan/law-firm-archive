package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 费用报销状态常量类 解决P3问题：状态硬编码字符串 */
public final class ExpenseStatus {

  private ExpenseStatus() {
    // 私有构造函数，防止实例化
  }

  // ============ 状态常量 ============
  /** 待审批 */
  public static final String PENDING = "PENDING";

  /** 已审批 */
  public static final String APPROVED = "APPROVED";

  /** 已驳回 */
  public static final String REJECTED = "REJECTED";

  /** 已支付 */
  public static final String PAID = "PAID";

  // ============ 状态名称映射 ============
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  static {
    STATUS_NAME_MAP.put(PENDING, "待审批");
    STATUS_NAME_MAP.put(APPROVED, "已审批");
    STATUS_NAME_MAP.put(REJECTED, "已驳回");
    STATUS_NAME_MAP.put(PAID, "已支付");
  }

  /**
   * 获取状态名称
   *
   * @param status 状态代码
   * @return 状态名称，如果不存在则返回原值
   */
  public static String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return STATUS_NAME_MAP.getOrDefault(status, status);
  }

  /**
   * 检查状态是否有效
   *
   * @param status 状态代码
   * @return 是否有效
   */
  public static boolean isValid(final String status) {
    return STATUS_NAME_MAP.containsKey(status);
  }

  /**
   * 检查是否可以审批
   *
   * @param status 当前状态
   * @return 是否可以审批
   */
  public static boolean canApprove(final String status) {
    return PENDING.equals(status);
  }

  /**
   * 检查是否可以支付
   *
   * @param status 当前状态
   * @return 是否可以支付
   */
  public static boolean canPay(final String status) {
    return APPROVED.equals(status);
  }

  /**
   * 检查是否可以删除
   *
   * @param status 当前状态
   * @return 是否可以删除
   */
  public static boolean canDelete(final String status) {
    return PENDING.equals(status);
  }

  /**
   * 检查是否待审批
   *
   * @param status 当前状态
   * @return 是否待审批
   */
  public static boolean isPending(final String status) {
    return PENDING.equals(status);
  }

  /**
   * 检查是否已审批
   *
   * @param status 当前状态
   * @return 是否已审批
   */
  public static boolean isApproved(final String status) {
    return APPROVED.equals(status);
  }
}
