package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.LoginLog;
import com.lawfirm.infrastructure.persistence.mapper.LoginLogMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志 Repository
 */
@Repository
public class LoginLogRepository extends AbstractRepository<LoginLogMapper, LoginLog> {

    /**
     * 查询用户登录日志
     */
    public List<LoginLog> findByUserId(Long userId, int offset, int limit) {
        return baseMapper.selectByUserId(userId, offset, limit);
    }

    /**
     * 查询指定时间范围内的登录日志
     */
    public List<LoginLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime,
                                          String username, String status, int offset, int limit) {
        return baseMapper.selectByTimeRange(startTime, endTime, username, status, offset, limit);
    }

    /**
     * 统计登录失败次数
     */
    public int countFailureByUsername(String username, LocalDateTime startTime) {
        return baseMapper.countFailureByUsername(username, startTime);
    }
}

