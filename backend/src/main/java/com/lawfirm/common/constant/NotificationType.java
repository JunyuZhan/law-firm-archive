package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 通知类型常量类 解决P3问题：类型硬编码字符串（问题487） */
public final class NotificationType {

  private NotificationType() {
    // 私有构造函数，防止实例化
  }

  // ============ 类型常量 ============
  /** 系统通知 */
  public static final String SYSTEM = "SYSTEM";

  /** 审批通知 */
  public static final String APPROVAL = "APPROVAL";

  /** 任务通知 */
  public static final String TASK = "TASK";

  /** 提醒通知 */
  public static final String REMINDER = "REMINDER";

  /** 警告通知 */
  public static final String WARNING = "WARNING";

  /** 公告通知 */
  public static final String ANNOUNCEMENT = "ANNOUNCEMENT";

  // ============ 状态常量 ============
  /** 未读 */
  public static final String STATUS_UNREAD = "UNREAD";

  /** 已读 */
  public static final String STATUS_READ = "READ";

  /** 已处理 */
  public static final String STATUS_HANDLED = "HANDLED";

  // ============ 名称映射 ============
  /** 类型名称映射 */
  private static final Map<String, String> TYPE_NAME_MAP = new HashMap<>();

  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  static {
    TYPE_NAME_MAP.put(SYSTEM, "系统通知");
    TYPE_NAME_MAP.put(APPROVAL, "审批通知");
    TYPE_NAME_MAP.put(TASK, "任务通知");
    TYPE_NAME_MAP.put(REMINDER, "提醒通知");
    TYPE_NAME_MAP.put(WARNING, "警告通知");
    TYPE_NAME_MAP.put(ANNOUNCEMENT, "公告通知");

    STATUS_NAME_MAP.put(STATUS_UNREAD, "未读");
    STATUS_NAME_MAP.put(STATUS_READ, "已读");
    STATUS_NAME_MAP.put(STATUS_HANDLED, "已处理");
  }

  /**
   * 获取类型名称
   *
   * @param type 类型代码
   * @return 类型名称，如果不存在则返回原值
   */
  public static String getTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return TYPE_NAME_MAP.getOrDefault(type, type);
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
   * 检查类型是否有效
   *
   * @param type 类型代码
   * @return 是否有效
   */
  public static boolean isValidType(final String type) {
    return TYPE_NAME_MAP.containsKey(type);
  }
}
