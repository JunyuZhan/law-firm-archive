package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.UserRole;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 用户角色关联 Mapper */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

  /**
   * 根据用户ID删除关联.
   *
   * @param userId 用户ID
   * @return 删除数量
   */
  @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
  int deleteByUserId(@Param("userId") Long userId);

  /**
   * 根据角色ID删除关联.
   *
   * @param roleId 角色ID
   * @return 删除数量
   */
  @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
  int deleteByRoleId(@Param("roleId") Long roleId);

  /**
   * 根据用户ID查询角色ID列表.
   *
   * @param userId 用户ID
   * @return 角色ID列表
   */
  @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
  List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

  /**
   * 根据角色ID查询用户ID列表.
   *
   * @param roleId 角色ID
   * @return 用户ID列表
   */
  @Select("SELECT user_id FROM sys_user_role WHERE role_id = #{roleId}")
  List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
