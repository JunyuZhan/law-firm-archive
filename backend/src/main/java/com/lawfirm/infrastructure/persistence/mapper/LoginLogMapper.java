package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志 Mapper
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    /**
     * 查询用户登录日志
     */
    @Select("""
        SELECT * FROM sys_login_log
        WHERE user_id = #{userId}
        ORDER BY login_time DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<LoginLog> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询指定时间范围内的登录日志
     */
    @Select("""
        SELECT * FROM sys_login_log
        WHERE login_time >= #{startTime} AND login_time <= #{endTime}
        AND (#{username} IS NULL OR username = #{username})
        AND (#{status} IS NULL OR status = #{status})
        ORDER BY login_time DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<LoginLog> selectByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("username") String username,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 统计登录失败次数
     */
    @Select("""
        SELECT COUNT(*) FROM sys_login_log
        WHERE username = #{username}
        AND status = 'FAILURE'
        AND login_time >= #{startTime}
        """)
    int countFailureByUsername(@Param("username") String username, @Param("startTime") LocalDateTime startTime);
}

