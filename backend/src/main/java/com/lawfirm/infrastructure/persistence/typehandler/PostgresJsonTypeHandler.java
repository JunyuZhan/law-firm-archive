package com.lawfirm.infrastructure.persistence.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

/**
 * PostgreSQL JSONB 类型处理器.
 *
 * <p>用于处理 PostgreSQL 的 JSONB 类型与 Java Map 的转换
 *
 * <p>注意：移除了 @MappedTypes(Map.class) 注解，避免所有 Map 类型的结果都被此 TypeHandler 处理 只有在显式指定 typeHandler
 * 时才会使用此处理器
 *
 * @author system
 * @since 2026-01-17
 */
@MappedJdbcTypes(JdbcType.OTHER)
public class PostgresJsonTypeHandler extends BaseTypeHandler<Map<String, Object>> {

  /** JSON对象映射器 */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * 设置非空参数.
   *
   * @param ps PreparedStatement
   * @param i 参数索引
   * @param parameter 参数值
   * @param jdbcType JDBC类型
   * @throws SQLException SQL异常
   */
  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int i,
      final Map<String, Object> parameter,
      final JdbcType jdbcType)
      throws SQLException {
    try {
      String json = OBJECT_MAPPER.writeValueAsString(parameter);
      // 使用 setObject 设置 JSONB 类型，PostgreSQL JDBC 驱动会自动处理
      ps.setObject(i, json, Types.OTHER);
    } catch (JsonProcessingException e) {
      throw new SQLException("Error converting Map to JSON string", e);
    }
  }

  /**
   * 从ResultSet获取可空结果（按列名）.
   *
   * @param rs ResultSet
   * @param columnName 列名
   * @return Map对象
   * @throws SQLException SQL异常
   */
  @Override
  public Map<String, Object> getNullableResult(final ResultSet rs, final String columnName)
      throws SQLException {
    return parseJson(rs.getString(columnName));
  }

  /**
   * 从ResultSet获取可空结果（按列索引）.
   *
   * @param rs ResultSet
   * @param columnIndex 列索引
   * @return Map对象
   * @throws SQLException SQL异常
   */
  @Override
  public Map<String, Object> getNullableResult(final ResultSet rs, final int columnIndex)
      throws SQLException {
    return parseJson(rs.getString(columnIndex));
  }

  /**
   * 从CallableStatement获取可空结果.
   *
   * @param cs CallableStatement
   * @param columnIndex 列索引
   * @return Map对象
   * @throws SQLException SQL异常
   */
  @Override
  public Map<String, Object> getNullableResult(final CallableStatement cs, final int columnIndex)
      throws SQLException {
    return parseJson(cs.getString(columnIndex));
  }

  /**
   * 解析JSON字符串为Map.
   *
   * @param json JSON字符串
   * @return Map对象
   * @throws SQLException SQL异常
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> parseJson(final String json) throws SQLException {
    if (json == null || json.isEmpty()) {
      return null;
    }
    // 检查是否是有效的 JSON 对象格式（以 { 开头）
    String trimmed = json.trim();
    if (!trimmed.startsWith("{")) {
      // 不是 JSON 对象，返回 null 而不是抛出异常
      // 这样可以避免将普通字段值误解析为 JSON
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      // JSON 解析失败，可能不是有效的 JSON，返回 null
      return null;
    }
  }
}
