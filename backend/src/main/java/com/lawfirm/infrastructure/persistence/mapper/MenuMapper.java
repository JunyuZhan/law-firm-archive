package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.Menu;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 菜单Mapper */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

  /**
   * 查询所有菜单。
   *
   * @return 菜单列表
   */
  @Select("SELECT * FROM sys_menu WHERE deleted = false ORDER BY sort_order, id")
  List<Menu> selectAllMenus();

  /**
   * 查询用户菜单.
   *
   * @param userId 用户ID
   * @return 菜单列表
   */
  @Select(
      "SELECT DISTINCT m.* FROM sys_menu m "
          + "JOIN sys_role_menu rm ON m.id = rm.menu_id "
          + "JOIN sys_user_role ur ON rm.role_id = ur.role_id "
          + "WHERE ur.user_id = #{userId} AND m.deleted = false AND m.status = 'ENABLED' "
          + "ORDER BY m.sort_order, m.id")
  List<Menu> selectByUserId(@Param("userId") Long userId);

  /**
   * 查询角色菜单.
   *
   * @param roleId 角色ID
   * @return 菜单列表
   */
  @Select(
      "SELECT m.* FROM sys_menu m "
          + "JOIN sys_role_menu rm ON m.id = rm.menu_id "
          + "WHERE rm.role_id = #{roleId} AND m.deleted = false "
          + "ORDER BY m.sort_order, m.id")
  List<Menu> selectByRoleId(@Param("roleId") Long roleId);

  /**
   * 查询子菜单.
   *
   * @param parentId 父菜单ID
   * @return 子菜单列表
   */
  @Select(
      "SELECT * FROM sys_menu WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
  List<Menu> selectByParentId(@Param("parentId") Long parentId);

  /**
   * 删除角色菜单关联.
   *
   * @param roleId 角色ID
   * @return 删除数量
   */
  @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
  int deleteRoleMenus(@Param("roleId") Long roleId);

  /**
   * 插入角色菜单关联（如果已存在则忽略）.
   *
   * @param roleId 角色ID
   * @param menuId 菜单ID
   * @return 插入数量
   */
  @Insert(
      "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId}) "
          + "ON CONFLICT (role_id, menu_id) DO NOTHING")
  int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

  /**
   * 批量插入角色菜单关联（性能优化） 使用 INSERT ON CONFLICT 避免重复插入.
   *
   * @param roleId 角色ID
   * @param menuIds 菜单ID列表
   * @return 插入数量
   */
  @Insert(
      "<script>"
          + "INSERT INTO sys_role_menu (role_id, menu_id) VALUES "
          + "<foreach collection='menuIds' item='menuId' separator=','>"
          + "(#{roleId}, #{menuId})"
          + "</foreach>"
          + " ON CONFLICT (role_id, menu_id) DO NOTHING"
          + "</script>")
  int batchInsertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

  /**
   * 统计菜单的角色关联数量 问题495修复：用于删除前检查关联.
   *
   * @param menuId 菜单ID
   * @return 关联数量
   */
  @Select("SELECT COUNT(*) FROM sys_role_menu WHERE menu_id = #{menuId}")
  long countRoleMenus(@Param("menuId") Long menuId);
}
