package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.RoleChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色变更历史Mapper
 */
@Mapper
public interface RoleChangeLogMapper extends BaseMapper<RoleChangeLog> {

    /**
     * 根据用户ID查询角色变更历史
     */
    @Select("SELECT * FROM sys_role_change_log WHERE user_id = #{userId} ORDER BY changed_at DESC")
    List<RoleChangeLog> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据变更类型查询角色变更历史
     */
    @Select("SELECT * FROM sys_role_change_log WHERE change_type = #{changeType} ORDER BY changed_at DESC")
    List<RoleChangeLog> selectByChangeType(@Param("changeType") String changeType);
}

