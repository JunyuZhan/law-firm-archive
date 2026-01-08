package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.PermissionChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限变更历史Mapper
 */
@Mapper
public interface PermissionChangeLogMapper extends BaseMapper<PermissionChangeLog> {

    /**
     * 根据角色ID查询权限变更历史
     */
    @Select("SELECT * FROM sys_permission_change_log WHERE role_id = #{roleId} ORDER BY changed_at DESC")
    List<PermissionChangeLog> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据变更类型查询权限变更历史
     */
    @Select("SELECT * FROM sys_permission_change_log WHERE change_type = #{changeType} ORDER BY changed_at DESC")
    List<PermissionChangeLog> selectByChangeType(@Param("changeType") String changeType);

    /**
     * 根据权限代码查询权限变更历史
     */
    @Select("SELECT * FROM sys_permission_change_log WHERE permission_code = #{permissionCode} ORDER BY changed_at DESC")
    List<PermissionChangeLog> selectByPermissionCode(@Param("permissionCode") String permissionCode);
}

