package com.lawfirm.common.util;

/**
 * SQL 工具类.
 *
 * <p>提供 SQL 相关的安全处理方法。
 */
public final class SqlUtils {

  private SqlUtils() {
    // 工具类不允许实例化
  }

  /**
   * 转义 LIKE 查询中的特殊字符.
   *
   * <p>转义 % 和 _ 字符，防止用户输入被当作通配符。
   *
   * @param value 原始字符串
   * @return 转义后的字符串，如果输入为 null 则返回 null
   */
  public static String escapeLike(final String value) {
    if (value == null) {
      return null;
    }
    // 先转义反斜杠，再转义 % 和 _
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  /**
   * 构建 LIKE 查询的模糊匹配值.
   *
   * <p>转义特殊字符后添加前后通配符。
   *
   * @param value 原始搜索词
   * @return 格式化后的 LIKE 值（如 "%keyword%"），如果输入为空则返回 null
   */
  public static String buildLikePattern(final String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return "%" + escapeLike(value.trim()) + "%";
  }

  /**
   * 构建 LIKE 查询的前缀匹配值.
   *
   * <p>转义特殊字符后添加后缀通配符。
   *
   * @param value 原始搜索词
   * @return 格式化后的 LIKE 值（如 "keyword%"），如果输入为空则返回 null
   */
  public static String buildPrefixLikePattern(final String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return escapeLike(value.trim()) + "%";
  }
}
