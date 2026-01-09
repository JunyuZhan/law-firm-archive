package com.lawfirm.infrastructure.persistence.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.Map;

/**
 * PostgreSQL JSONB 类型处理器
 * 用于处理 PostgreSQL 的 JSONB 类型与 Java Map 的转换
 * 
 * 注意：移除了 @MappedTypes(Map.class) 注解，避免所有 Map 类型的结果都被此 TypeHandler 处理
 * 只有在显式指定 typeHandler 时才会使用此处理器
 */
@MappedJdbcTypes(JdbcType.OTHER)
public class PostgresJsonTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            // 使用 setObject 设置 JSONB 类型，PostgreSQL JDBC 驱动会自动处理
            ps.setObject(i, json, Types.OTHER);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting Map to JSON string", e);
        }
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) throws SQLException {
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
