package com.archivesystem.repository;

import com.archivesystem.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper.
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询.
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = false")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询.
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = false")
    User selectByEmail(@Param("email") String email);
}
