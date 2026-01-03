package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.UserSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户会话 Mapper
 */
@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {

    /**
     * 分页查询用户会话
     */
    @Select("""
            <script>
            SELECT s.*, u.real_name as user_real_name
            FROM sys_user_session s
            LEFT JOIN sys_user u ON s.user_id = u.id
            WHERE s.deleted = false
            <if test='query.userId != null'>
                AND s.user_id = #{query.userId}
            </if>
            <if test='query.username != null and query.username != ""'>
                AND s.username LIKE CONCAT('%', #{query.username}, '%')
            </if>
            <if test='query.status != null and query.status != ""'>
                AND s.status = #{query.status}
            </if>
            <if test='query.ipAddress != null and query.ipAddress != ""'>
                AND s.ip_address = #{query.ipAddress}
            </if>
            ORDER BY s.login_time DESC
            </script>
            """)
    IPage<UserSession> selectSessionPage(Page<UserSession> page, @Param("query") com.lawfirm.application.system.dto.UserSessionQueryDTO query);

    /**
     * 根据Token查询会话
     */
    @Select("SELECT * FROM sys_user_session WHERE token = #{token} AND deleted = false AND status = 'ACTIVE'")
    UserSession selectByToken(@Param("token") String token);

    /**
     * 根据用户ID查询活跃会话
     */
    @Select("SELECT * FROM sys_user_session WHERE user_id = #{userId} AND status = 'ACTIVE' AND deleted = false ORDER BY login_time DESC")
    List<UserSession> selectActiveSessionsByUserId(@Param("userId") Long userId);

    /**
     * 更新最后访问时间
     */
    @Update("UPDATE sys_user_session SET last_access_time = #{lastAccessTime} WHERE id = #{id}")
    void updateLastAccessTime(@Param("id") Long id, @Param("lastAccessTime") LocalDateTime lastAccessTime);

    /**
     * 批量更新会话状态为过期
     */
    @Update("UPDATE sys_user_session SET status = 'EXPIRED' WHERE expire_time < #{now} AND status = 'ACTIVE'")
    int updateExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * 将用户的其他会话标记为非当前会话
     */
    @Update("UPDATE sys_user_session SET is_current = false WHERE user_id = #{userId} AND id != #{excludeId} AND deleted = false")
    void markOtherSessionsAsNotCurrent(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
}

