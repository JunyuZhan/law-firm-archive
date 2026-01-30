package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 报表状态常量类 解决P3问题：状态硬编码字符串（问题572-573） */
public final class ReportStatus {

  private ReportStatus() {
    // 私有构造函数，防止实例化
  }

  // ============ 状态常量 ============
  /** 待生成 */
  public static final String PENDING = "PENDING";

  /** 生成中 */
  public static final String GENERATING = "GENERATING";

  /** 已完成 */
  public static final String COMPLETED = "COMPLETED";

  /** 失败 */
  public static final String FAILED = "FAILED";

  // ============ 报表类型常量 ============
  /** 收入报表 */
  public static final String TYPE_REVENUE = "REVENUE";

  /** 案件报表 */
  public static final String TYPE_MATTER = "MATTER";

  /** 律师业绩报表 */
  public static final String TYPE_LAWYER_PERFORMANCE = "LAWYER_PERFORMANCE";

  /** 客户分析报表 */
  public static final String TYPE_CLIENT_ANALYSIS = "CLIENT_ANALYSIS";

  /** 工时统计报表 */
  public static final String TYPE_TIMESHEET = "TIMESHEET";

  /** 费用报表 */
  public static final String TYPE_EXPENSE = "EXPENSE";

  /** 合同报表 */
  public static final String TYPE_CONTRACT = "CONTRACT";

  /** 应收款报表 */
  public static final String TYPE_RECEIVABLE = "RECEIVABLE";

  /** 提成报表 */
  public static final String TYPE_COMMISSION = "COMMISSION";

  /** 部门业绩报表 */
  public static final String TYPE_DEPARTMENT_PERFORMANCE = "DEPARTMENT_PERFORMANCE";

  // ============ 名称映射 ============
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  /** 类型名称映射 */
  private static final Map<String, String> TYPE_NAME_MAP = new HashMap<>();

  static {
    STATUS_NAME_MAP.put(PENDING, "待生成");
    STATUS_NAME_MAP.put(GENERATING, "生成中");
    STATUS_NAME_MAP.put(COMPLETED, "已完成");
    STATUS_NAME_MAP.put(FAILED, "失败");

    TYPE_NAME_MAP.put(TYPE_REVENUE, "收入报表");
    TYPE_NAME_MAP.put(TYPE_MATTER, "案件报表");
    TYPE_NAME_MAP.put(TYPE_LAWYER_PERFORMANCE, "律师业绩报表");
    TYPE_NAME_MAP.put(TYPE_CLIENT_ANALYSIS, "客户分析报表");
    TYPE_NAME_MAP.put(TYPE_TIMESHEET, "工时统计报表");
    TYPE_NAME_MAP.put(TYPE_EXPENSE, "费用报表");
    TYPE_NAME_MAP.put(TYPE_CONTRACT, "合同报表");
    TYPE_NAME_MAP.put(TYPE_RECEIVABLE, "应收款报表");
    TYPE_NAME_MAP.put(TYPE_COMMISSION, "提成报表");
    TYPE_NAME_MAP.put(TYPE_DEPARTMENT_PERFORMANCE, "部门业绩报表");
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
   * 获取报表类型名称
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
   * 检查状态是否有效
   *
   * @param status 状态代码
   * @return 是否有效
   */
  public static boolean isValidStatus(final String status) {
    return STATUS_NAME_MAP.containsKey(status);
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

  /**
   * 检查报表是否可以下载
   *
   * @param status 当前状态
   * @return 是否可以下载
   */
  public static boolean canDownload(final String status) {
    return COMPLETED.equals(status);
  }

  /**
   * 检查报表是否可以删除
   *
   * @param status 当前状态
   * @return 是否可以删除
   */
  public static boolean canDelete(final String status) {
    return COMPLETED.equals(status) || FAILED.equals(status);
  }
}
