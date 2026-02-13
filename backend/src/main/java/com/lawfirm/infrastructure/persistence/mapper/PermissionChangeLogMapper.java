package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.PermissionChangeLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 权限变更历史Mapper */
@Mapper
public interface PermissionChangeLogMapper extends BaseMapper<PermissionChangeLog> {

  /**
   * 根据角色ID查询权限变更历史.
   *
   * @param roleId 角色ID
   * @return 权限变更历史列表
   */
  @Select(
      "SELECT * FROM sys_permission_change_log WHERE role_id = #{roleId} ORDER BY changed_at DESC")
  List<PermissionChangeLog> selectByRoleId(@Param("roleId") Long roleId);

  /**
   * 根据变更类型查询权限变更历史.
   *
   * @param changeType 变更类型
   * @return 权限变更历史列表
   */
  @Select(
      "SELECT * FROM sys_permission_change_log WHERE change_type = #{changeType} ORDER BY changed_at DESC")
  List<PermissionChangeLog> selectByChangeType(@Param("changeType") String changeType);

  /**
   * 根据权限代码查询权限变更历史.
   *
   * @param permissionCode 权限代码
   * @return 权限变更历史列表
   */
  @Select(
      "SELECT * FROM sys_permission_change_log WHERE permission_code = #{permissionCode} ORDER BY changed_at DESC")
  List<PermissionChangeLog> selectByPermissionCode(@Param("permissionCode") String permissionCode);
}
