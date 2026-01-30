package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 离职申请状态常量类 解决P3问题：状态硬编码字符串 */
public final class ResignationStatus {

  private ResignationStatus() {
    // 私有构造函数，防止实例化
  }

  // ============ 申请状态常量 ============
  /** 待审批 */
  public static final String PENDING = "PENDING";

  /** 已通过 */
  public static final String APPROVED = "APPROVED";

  /** 已拒绝 */
  public static final String REJECTED = "REJECTED";

  /** 已完成 */
  public static final String COMPLETED = "COMPLETED";

  // ============ 交接状态常量 ============
  /** 待交接 */
  public static final String HANDOVER_PENDING = "PENDING";

  /** 交接中 */
  public static final String HANDOVER_IN_PROGRESS = "IN_PROGRESS";

  /** 已完成 */
  public static final String HANDOVER_COMPLETED = "COMPLETED";

  // ============ 离职类型常量 ============
  /** 主动离职 */
  public static final String TYPE_VOLUNTARY = "VOLUNTARY";

  /** 辞退 */
  public static final String TYPE_DISMISSED = "DISMISSED";

  /** 退休 */
  public static final String TYPE_RETIREMENT = "RETIREMENT";

  /** 合同到期 */
  public static final String TYPE_CONTRACT_EXPIRED = "CONTRACT_EXPIRED";

  // ============ 状态名称映射 ============
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  /** 交接状态名称映射 */
  private static final Map<String, String> HANDOVER_STATUS_NAME_MAP = new HashMap<>();

  /** 类型名称映射 */
  private static final Map<String, String> TYPE_NAME_MAP = new HashMap<>();

  static {
    STATUS_NAME_MAP.put(PENDING, "待审批");
    STATUS_NAME_MAP.put(APPROVED, "已通过");
    STATUS_NAME_MAP.put(REJECTED, "已拒绝");
    STATUS_NAME_MAP.put(COMPLETED, "已完成");

    HANDOVER_STATUS_NAME_MAP.put(HANDOVER_PENDING, "待交接");
    HANDOVER_STATUS_NAME_MAP.put(HANDOVER_IN_PROGRESS, "交接中");
    HANDOVER_STATUS_NAME_MAP.put(HANDOVER_COMPLETED, "已完成");

    TYPE_NAME_MAP.put(TYPE_VOLUNTARY, "主动离职");
    TYPE_NAME_MAP.put(TYPE_DISMISSED, "辞退");
    TYPE_NAME_MAP.put(TYPE_RETIREMENT, "退休");
    TYPE_NAME_MAP.put(TYPE_CONTRACT_EXPIRED, "合同到期");
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
   * 获取交接状态名称
   *
   * @param status 交接状态代码
   * @return 交接状态名称，如果不存在则返回原值
   */
  public static String getHandoverStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return HANDOVER_STATUS_NAME_MAP.getOrDefault(status, status);
  }

  /**
   * 获取离职类型名称
   *
   * @param type 离职类型代码
   * @return 离职类型名称，如果不存在则返回原值
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
   * 检查是否可以完成交接
   *
   * @param status 当前状态
   * @return 是否可以完成交接
   */
  public static boolean canCompleteHandover(final String status) {
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
}
