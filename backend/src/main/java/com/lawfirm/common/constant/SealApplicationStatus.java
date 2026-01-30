package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 用印申请状态常量类 解决P3问题：状态硬编码字符串 */
public final class SealApplicationStatus {

  private SealApplicationStatus() {
    // 私有构造函数，防止实例化
  }

  // ============ 状态常量 ============
  /** 待审批 */
  public static final String PENDING = "PENDING";

  /** 已批准 */
  public static final String APPROVED = "APPROVED";

  /** 已拒绝 */
  public static final String REJECTED = "REJECTED";

  /** 已用印 */
  public static final String USED = "USED";

  /** 已取消 */
  public static final String CANCELLED = "CANCELLED";

  // ============ 印章状态常量 ============
  /** 可用 */
  public static final String SEAL_ACTIVE = "ACTIVE";

  /** 停用 */
  public static final String SEAL_INACTIVE = "INACTIVE";

  // ============ 状态名称映射 ============
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  static {
    STATUS_NAME_MAP.put(PENDING, "待审批");
    STATUS_NAME_MAP.put(APPROVED, "已批准");
    STATUS_NAME_MAP.put(REJECTED, "已拒绝");
    STATUS_NAME_MAP.put(USED, "已用印");
    STATUS_NAME_MAP.put(CANCELLED, "已取消");
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
   * 检查是否可以用印
   *
   * @param status 当前状态
   * @return 是否可以用印
   */
  public static boolean canUse(final String status) {
    return APPROVED.equals(status);
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
}
