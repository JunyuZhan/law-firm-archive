package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = false")
    User selectByUsername(@Param("username") String username);

    /**
     * 分页查询用户
     */
    @Select("""
        <script>
        SELECT u.*, d.name as department_name FROM sys_user u
        LEFT JOIN sys_department d ON u.department_id = d.id AND d.deleted = false
        WHERE u.deleted = false
        <if test="username != null and username != ''">
            AND u.username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="realName != null and realName != ''">
            AND u.real_name LIKE CONCAT('%', #{realName}, '%')
        </if>
        <if test="phone != null and phone != ''">
            AND u.phone LIKE CONCAT('%', #{phone}, '%')
        </if>
        <if test="departmentId != null">
            AND u.department_id = #{departmentId}
        </if>
        <if test="status != null and status != ''">
            AND u.status = #{status}
        </if>
        <if test="compensationType != null and compensationType != ''">
            AND u.compensation_type = #{compensationType}
        </if>
        ORDER BY u.id DESC
        </script>
        """)
    IPage<User> selectUserPage(Page<User> page,
                               @Param("username") String username,
                               @Param("realName") String realName,
                               @Param("phone") String phone,
                               @Param("departmentId") Long departmentId,
                               @Param("status") String status,
                               @Param("compensationType") String compensationType);

    /**
     * 统计部门下的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE department_id = #{departmentId} AND deleted = false")
    int countByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 查询用户的角色编码列表
     */
    @Select("""
        SELECT r.role_code FROM sys_role r
        INNER JOIN sys_user_role ur ON r.id = ur.role_id
        WHERE ur.user_id = #{userId} AND r.deleted = false
        """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的角色ID列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的权限编码列表
     */
    @Select("""
        SELECT DISTINCT m.permission FROM sys_menu m
        INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
        INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
        WHERE ur.user_id = #{userId} AND m.permission IS NOT NULL AND m.deleted = false
        """)
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 删除用户角色关联
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteUserRoles(@Param("userId") Long userId);

    /**
     * 批量插入用户角色关联
     */
    @Insert("""
        <script>
        INSERT INTO sys_user_role (user_id, role_id) VALUES
        <foreach collection="roleIds" item="roleId" separator=",">
            (#{userId}, #{roleId})
        </foreach>
        </script>
        """)
    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 根据角色编码查询用户ID列表
     */
    @Select("""
        SELECT DISTINCT u.id FROM sys_user u
        INNER JOIN sys_user_role ur ON u.id = ur.user_id
        INNER JOIN sys_role r ON ur.role_id = r.id
        WHERE r.role_code = #{roleCode} AND u.deleted = false AND r.deleted = false
        LIMIT 1
        """)
    List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 查询行政人员（角色为ADMIN的用户）
     */
    @Select("""
        SELECT DISTINCT u.* FROM sys_user u
        INNER JOIN sys_user_role ur ON u.id = ur.user_id
        INNER JOIN sys_role r ON ur.role_id = r.id
        WHERE r.role_code = 'ADMIN' AND u.deleted = false AND r.deleted = false AND u.status = 'ACTIVE'
        """)
    List<User> selectAdminUsers();

    /**
     * 获取用户最高数据范围权限
     * 优先级: ALL > DEPT_AND_CHILD > DEPT > SELF
     */
    @Select("""
        SELECT r.data_scope FROM sys_role r
        INNER JOIN sys_user_role ur ON r.id = ur.role_id
        WHERE ur.user_id = #{userId} AND r.deleted = false
        ORDER BY 
            CASE r.data_scope 
                WHEN 'ALL' THEN 1 
                WHEN 'DEPT_AND_CHILD' THEN 2 
                WHEN 'DEPT' THEN 3 
                ELSE 4 
            END
        LIMIT 1
        """)
    String selectHighestDataScopeByUserId(@Param("userId") Long userId);

    /**
     * 批量删除用户角色关联
     * 问题452修复：支持批量操作
     */
    @Delete("""
        <script>
        DELETE FROM sys_user_role WHERE user_id IN
        <foreach collection="userIds" item="userId" open="(" separator="," close=")">
            #{userId}
        </foreach>
        </script>
        """)
    void batchDeleteUserRoles(@Param("userIds") List<Long> userIds);
}
