package com.lawfirm.common.constant;

import java.util.HashMap;
import java.util.Map;

/** 文档状态常量类 解决P3问题：状态硬编码字符串 */
public final class DocumentStatus {

  private DocumentStatus() {
    // 私有构造函数，防止实例化
  }

  // ============ 状态常量 ============
  /** 草稿 */
  public static final String DRAFT = "DRAFT";

  /** 待审核 */
  public static final String PENDING_REVIEW = "PENDING_REVIEW";

  /** 已发布 */
  public static final String PUBLISHED = "PUBLISHED";

  /** 已归档 */
  public static final String ARCHIVED = "ARCHIVED";

  /** 已删除 */
  public static final String DELETED = "DELETED";

  // ============ 文档类型常量 ============
  /** 合同文档 */
  public static final String TYPE_CONTRACT = "CONTRACT";

  /** 案件文档 */
  public static final String TYPE_MATTER = "MATTER";

  /** 模板文档 */
  public static final String TYPE_TEMPLATE = "TEMPLATE";

  /** 知识库文档 */
  public static final String TYPE_KNOWLEDGE = "KNOWLEDGE";

  // ============ 状态名称映射 ============
  /** 状态名称映射 */
  private static final Map<String, String> STATUS_NAME_MAP = new HashMap<>();

  /** 类型名称映射 */
  private static final Map<String, String> TYPE_NAME_MAP = new HashMap<>();

  static {
    STATUS_NAME_MAP.put(DRAFT, "草稿");
    STATUS_NAME_MAP.put(PENDING_REVIEW, "待审核");
    STATUS_NAME_MAP.put(PUBLISHED, "已发布");
    STATUS_NAME_MAP.put(ARCHIVED, "已归档");
    STATUS_NAME_MAP.put(DELETED, "已删除");

    TYPE_NAME_MAP.put(TYPE_CONTRACT, "合同文档");
    TYPE_NAME_MAP.put(TYPE_MATTER, "案件文档");
    TYPE_NAME_MAP.put(TYPE_TEMPLATE, "模板文档");
    TYPE_NAME_MAP.put(TYPE_KNOWLEDGE, "知识库文档");
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
   * 检查状态是否有效
   *
   * @param status 状态代码
   * @return 是否有效
   */
  public static boolean isValid(final String status) {
    return STATUS_NAME_MAP.containsKey(status);
  }

  /**
   * 检查是否可以编辑
   *
   * @param status 当前状态
   * @return 是否可以编辑
   */
  public static boolean canEdit(final String status) {
    return DRAFT.equals(status);
  }

  /**
   * 检查是否可以发布
   *
   * @param status 当前状态
   * @return 是否可以发布
   */
  public static boolean canPublish(final String status) {
    return DRAFT.equals(status) || PENDING_REVIEW.equals(status);
  }
}
