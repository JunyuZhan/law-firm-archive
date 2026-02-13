package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 工时状态常量类 解决P3问题：状态硬编码字符串 */
public final class TimesheetStatus {

  private TimesheetStatus() {
    // 私有构造函数，防止实例化
  }

  // ============ 状态常量 ============
  /** 草稿 */
  public static final String DRAFT = "DRAFT";

  /** 已提交 */
  public static final String SUBMITTED = "SUBMITTED";

  /** 已批准 */
  public static final String APPROVED = "APPROVED";

  /** 已拒绝 */
  public static final String REJECTED = "REJECTED";

  // ============ 状态名称映射 ============
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  static {
    STATUS_NAME_MAP.put(DRAFT, "草稿");
    STATUS_NAME_MAP.put(SUBMITTED, "已提交");
    STATUS_NAME_MAP.put(APPROVED, "已批准");
    STATUS_NAME_MAP.put(REJECTED, "已拒绝");
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
   * 检查是否可以修改
   *
   * @param status 当前状态
   * @return 是否可以修改
   */
  public static boolean canModify(final String status) {
    return DRAFT.equals(status);
  }

  /**
   * 检查是否可以提交
   *
   * @param status 当前状态
   * @return 是否可以提交
   */
  public static boolean canSubmit(final String status) {
    return DRAFT.equals(status);
  }

  /**
   * 检查是否可以审批
   *
   * @param status 当前状态
   * @return 是否可以审批
   */
  public static boolean canApprove(final String status) {
    return SUBMITTED.equals(status);
  }

  /**
   * 检查是否已批准
   *
   * @param status 当前状态
   * @return 是否已批准
   */
  public static boolean isApproved(final String status) {
    return APPROVED.equals(status);
  }

  /**
   * 检查是否已拒绝
   *
   * @param status 当前状态
   * @return 是否已拒绝
   */
  public static boolean isRejected(final String status) {
    return REJECTED.equals(status);
  }
}
