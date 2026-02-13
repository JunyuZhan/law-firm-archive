package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 收款状态常量类 解决P3问题：状态硬编码字符串 */
public final class PaymentStatus {

  private PaymentStatus() {
    // 私有构造函数，防止实例化
  }

  // ============ 状态常量 ============
  /** 待付款 */
  public static final String PENDING = "PENDING";

  /** 已确认 */
  public static final String CONFIRMED = "CONFIRMED";

  /** 已取消 */
  public static final String CANCELLED = "CANCELLED";

  /** 已退款 */
  public static final String REFUNDED = "REFUNDED";

  /** 已付款 */
  public static final String PAID = "PAID";

  /** 部分付款 */
  public static final String PARTIAL = "PARTIAL";

  // ============ 状态名称映射 ============
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  static {
    STATUS_NAME_MAP.put(PENDING, "待付款");
    STATUS_NAME_MAP.put(CONFIRMED, "已确认");
    STATUS_NAME_MAP.put(CANCELLED, "已取消");
    STATUS_NAME_MAP.put(REFUNDED, "已退款");
    STATUS_NAME_MAP.put(PAID, "已付款");
    STATUS_NAME_MAP.put(PARTIAL, "部分付款");
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
   * 检查是否可以确认
   *
   * @param status 当前状态
   * @return 是否可以确认
   */
  public static boolean canConfirm(final String status) {
    return PENDING.equals(status);
  }

  /**
   * 检查是否可以取消
   *
   * @param status 当前状态
   * @return 是否可以取消
   */
  public static boolean canCancel(final String status) {
    return PENDING.equals(status);
  }

  /**
   * 检查是否可以退款
   *
   * @param status 当前状态
   * @return 是否可以退款
   */
  public static boolean canRefund(final String status) {
    return CONFIRMED.equals(status);
  }
}
