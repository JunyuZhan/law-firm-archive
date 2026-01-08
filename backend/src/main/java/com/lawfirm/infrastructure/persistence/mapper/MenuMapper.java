package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.Menu;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 菜单Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    /**
     * 查询所有菜单
     */
    @Select("SELECT * FROM sys_menu WHERE deleted = false ORDER BY sort_order, id")
    List<Menu> selectAllMenus();

    /**
     * 查询用户菜单
     */
    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.deleted = false AND m.status = 'ENABLED' " +
            "ORDER BY m.sort_order, m.id")
    List<Menu> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询角色菜单
     */
    @Select("SELECT m.* FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.deleted = false " +
            "ORDER BY m.sort_order, m.id")
    List<Menu> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询子菜单
     */
    @Select("SELECT * FROM sys_menu WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
    List<Menu> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 删除角色菜单关联
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(@Param("roleId") Long roleId);

    /**
     * 插入角色菜单关联（如果已存在则忽略）
     */
    @Insert("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId}) " +
            "ON CONFLICT (role_id, menu_id) DO NOTHING")
    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
