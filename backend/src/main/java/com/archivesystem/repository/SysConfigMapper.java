package com.archivesystem.repository;

import com.archivesystem.entity.SysConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统配置Mapper接口
 * @author junyuzhan
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置键查询
     */
    @Select("SELECT * FROM sys_config WHERE config_key = #{configKey}")
    SysConfig selectByKey(@Param("configKey") String configKey);

    /**
     * 根据配置分组查询
     */
    @Select("SELECT * FROM sys_config WHERE config_group = #{configGroup} ORDER BY sort_order")
    List<SysConfig> selectByGroup(@Param("configGroup") String configGroup);

    /**
     * 查询所有配置（按分组和排序）
     */
    @Select("SELECT * FROM sys_config ORDER BY config_group, sort_order")
    List<SysConfig> selectAllOrdered();

    /**
     * 查询以指定前缀开头的配置
     */
    @Select("SELECT * FROM sys_config WHERE config_key LIKE #{prefix} || '%' ORDER BY sort_order")
    List<SysConfig> selectByKeyPrefix(@Param("prefix") String prefix);
}
