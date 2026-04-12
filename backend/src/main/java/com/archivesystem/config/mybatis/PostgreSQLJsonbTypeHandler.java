package com.archivesystem.config.mybatis;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreSQL jsonb 类型处理器.
 * 使用 PGobject 避免 MyBatis-Plus 默认按 varchar 写入 jsonb 列。
 * @author junyuzhan
 */
public class PostgreSQLJsonbTypeHandler extends JacksonTypeHandler {

    public PostgreSQLJsonbTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, toJson(parameter), Types.OTHER);
    }
}
