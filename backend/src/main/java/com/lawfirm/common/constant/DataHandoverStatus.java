package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 数据交接状态常量类 解决P3问题：状态硬编码字符串 */
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
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  /** 详细状态名称映射 */
  private static final Map<String, String> DETAIL_STATUS_NAME_MAP = new HashMap<>();

  /** 类型名称映射 */
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
   *
   * @param status 状态值
   * @return 状态名称，如果状态为null则返回null
   */
  public static String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return STATUS_NAME_MAP.getOrDefault(status, status);
  }

  /**
   * 获取明细状态名称
   *
   * @param status 明细状态值
   * @return 明细状态名称，如果状态为null则返回null
   */
  public static String getDetailStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return DETAIL_STATUS_NAME_MAP.getOrDefault(status, status);
  }

  /**
   * 获取交接类型名称
   *
   * @param type 交接类型值
   * @return 交接类型名称，如果类型为null则返回null
   */
  public static String getTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return TYPE_NAME_MAP.getOrDefault(type, type);
  }

  /**
   * 检查状态是否有效
   *
   * @param status 状态值
   * @return 如果状态有效返回true，否则返回false
   */
  public static boolean isValid(final String status) {
    return STATUS_NAME_MAP.containsKey(status);
  }

  /**
   * 检查是否可以确认执行
   *
   * @param status 状态值
   * @return 如果可以确认执行返回true，否则返回false
   */
  public static boolean canConfirm(final String status) {
    return APPROVED.equals(status);
  }

  /**
   * 检查是否可以取消
   *
   * @param status 状态值
   * @return 如果可以取消返回true，否则返回false
   */
  public static boolean canCancel(final String status) {
    return PENDING_APPROVAL.equals(status) || APPROVED.equals(status);
  }
}
