package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.system.entity.PermissionChangeLog;
import com.lawfirm.infrastructure.persistence.mapper.PermissionChangeLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限变更历史仓储
 */
@Repository
public class PermissionChangeLogRepository extends ServiceImpl<PermissionChangeLogMapper, PermissionChangeLog> {

    /**
     * 根据角色ID查询权限变更历史
     */
    public List<PermissionChangeLog> findByRoleId(Long roleId) {
        return baseMapper.selectByRoleId(roleId);
    }

    /**
     * 根据变更类型查询权限变更历史
     */
    public List<PermissionChangeLog> findByChangeType(String changeType) {
        return baseMapper.selectByChangeType(changeType);
    }

    /**
     * 根据权限代码查询权限变更历史
     */
    public List<PermissionChangeLog> findByPermissionCode(String permissionCode) {
        return baseMapper.selectByPermissionCode(permissionCode);
    }
}

