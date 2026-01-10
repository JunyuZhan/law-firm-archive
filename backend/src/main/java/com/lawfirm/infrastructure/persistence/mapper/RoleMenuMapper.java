package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.RoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色菜单关联 Mapper
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

    /**
     * 根据角色ID删除关联
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据菜单ID删除关联
     */
    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int deleteByMenuId(@Param("menuId") Long menuId);

    /**
     * 根据角色ID查询菜单ID列表
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID列表查询菜单ID列表
     */
    @Select("<script>" +
            "SELECT DISTINCT menu_id FROM sys_role_menu WHERE role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            "</script>")
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 批量插入角色菜单关联
     * 问题453修复：支持批量操作
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_menu (role_id, menu_id) VALUES " +
            "<foreach collection='roleMenus' item='rm' separator=','>" +
            "(#{rm.roleId}, #{rm.menuId})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("roleMenus") List<RoleMenu> roleMenus);

    /**
     * 根据角色ID和菜单ID列表删除关联
     * 问题455修复：支持差异更新
     */
    @Delete("<script>" +
            "DELETE FROM sys_role_menu WHERE role_id = #{roleId} AND menu_id IN " +
            "<foreach collection='menuIds' item='menuId' open='(' separator=',' close=')'>" +
            "#{menuId}" +
            "</foreach>" +
            "</script>")
    int deleteByRoleIdAndMenuIds(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
