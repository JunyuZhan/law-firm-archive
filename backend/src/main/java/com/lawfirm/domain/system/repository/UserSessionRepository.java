package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.UserSession;
import com.lawfirm.infrastructure.persistence.mapper.UserSessionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户会话 Repository
 */
@Repository
public class UserSessionRepository extends AbstractRepository<UserSessionMapper, UserSession> {

    /**
     * 根据Token查询会话
     */
    public Optional<UserSession> findByToken(String token) {
        return Optional.ofNullable(baseMapper.selectByToken(token));
    }

    /**
     * 根据用户ID查询活跃会话
     */
    public List<UserSession> findActiveSessionsByUserId(Long userId) {
        return baseMapper.selectActiveSessionsByUserId(userId);
    }

    /**
     * 更新最后访问时间
     */
    public void updateLastAccessTime(Long id, java.time.LocalDateTime lastAccessTime) {
        baseMapper.updateLastAccessTime(id, lastAccessTime);
    }

    /**
     * 批量更新过期会话状态
     */
    public int updateExpiredSessions(java.time.LocalDateTime now) {
        return baseMapper.updateExpiredSessions(now);
    }

    /**
     * 将用户的其他会话标记为非当前会话
     */
    public void markOtherSessionsAsNotCurrent(Long userId, Long excludeId) {
        baseMapper.markOtherSessionsAsNotCurrent(userId, excludeId);
    }
}

