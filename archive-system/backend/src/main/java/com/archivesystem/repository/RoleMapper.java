package com.archivesystem.repository;

import com.archivesystem.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper接口.
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色编码查询.
     */
    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode} AND deleted = false")
    Role selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 根据用户ID查询角色列表.
     */
    @Select("SELECT r.* FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId} AND r.deleted = false")
    List<Role> selectByUserId(@Param("userId") Long userId);
}
