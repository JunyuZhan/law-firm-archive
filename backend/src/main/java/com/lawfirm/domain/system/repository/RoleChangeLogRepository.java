package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.system.entity.RoleChangeLog;
import com.lawfirm.infrastructure.persistence.mapper.RoleChangeLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色变更历史仓储
 */
@Repository
public class RoleChangeLogRepository extends ServiceImpl<RoleChangeLogMapper, RoleChangeLog> {

    /**
     * 根据用户ID查询角色变更历史
     */
    public List<RoleChangeLog> findByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    /**
     * 根据变更类型查询角色变更历史
     */
    public List<RoleChangeLog> findByChangeType(String changeType) {
        return baseMapper.selectByChangeType(changeType);
    }
}

