package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 档案借阅状态常量类 解决P3问题：状态硬编码字符串 */
public final class ArchiveBorrowStatus {

  private ArchiveBorrowStatus() {
    // 私有构造函数，防止实例化
  }

  // ============ 借阅状态常量 ============
  /** 待审批 */
  public static final String PENDING = "PENDING";

  /** 已批准 */
  public static final String APPROVED = "APPROVED";

  /** 已拒绝 */
  public static final String REJECTED = "REJECTED";

  /** 借出中 */
  public static final String BORROWED = "BORROWED";

  /** 已归还 */
  public static final String RETURNED = "RETURNED";

  /** 逾期 */
  public static final String OVERDUE = "OVERDUE";

  // ============ 档案状态常量 ============
  /** 已入库 */
  public static final String ARCHIVE_STORED = "STORED";

  /** 借出中 */
  public static final String ARCHIVE_BORROWED = "BORROWED";

  // ============ 归还状态常量 ============
  /** 完好 */
  public static final String CONDITION_GOOD = "GOOD";

  /** 损坏 */
  public static final String CONDITION_DAMAGED = "DAMAGED";

  /** 遗失 */
  public static final String CONDITION_LOST = "LOST";

  // ============ 状态名称映射 ============
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  /** 条件名称映射 */
  private static final Map<String, String> CONDITION_NAME_MAP = new HashMap<>();

  static {
    STATUS_NAME_MAP.put(PENDING, "待审批");
    STATUS_NAME_MAP.put(APPROVED, "已批准");
    STATUS_NAME_MAP.put(REJECTED, "已拒绝");
    STATUS_NAME_MAP.put(BORROWED, "借出中");
    STATUS_NAME_MAP.put(RETURNED, "已归还");
    STATUS_NAME_MAP.put(OVERDUE, "逾期");

    CONDITION_NAME_MAP.put(CONDITION_GOOD, "完好");
    CONDITION_NAME_MAP.put(CONDITION_DAMAGED, "损坏");
    CONDITION_NAME_MAP.put(CONDITION_LOST, "遗失");
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
   * 获取归还状态名称
   *
   * @param condition 归还状态值
   * @return 归还状态名称，如果状态为null则返回null
   */
  public static String getConditionName(final String condition) {
    if (condition == null) {
      return null;
    }
    return CONDITION_NAME_MAP.getOrDefault(condition, condition);
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
   * 检查是否可以审批
   *
   * @param status 状态值
   * @return 如果可以审批返回true，否则返回false
   */
  public static boolean canApprove(final String status) {
    return PENDING.equals(status);
  }

  /**
   * 检查是否可以借出
   *
   * @param status 状态值
   * @return 如果可以借出返回true，否则返回false
   */
  public static boolean canBorrow(final String status) {
    return APPROVED.equals(status);
  }

  /**
   * 检查是否可以归还
   *
   * @param status 状态值
   * @return 如果可以归还返回true，否则返回false
   */
  public static boolean canReturn(final String status) {
    return BORROWED.equals(status) || OVERDUE.equals(status);
  }
}
