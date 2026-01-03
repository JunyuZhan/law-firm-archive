package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 系统配置Mapper
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 查询所有配置
     */
    @Select("SELECT * FROM sys_config WHERE deleted = false ORDER BY id")
    List<SysConfig> selectAllConfigs();

    /**
     * 根据键查询
     */
    @Select("SELECT * FROM sys_config WHERE config_key = #{key} AND deleted = false")
    SysConfig selectByKey(@Param("key") String key);

    /**
     * 更新配置值
     */
    @Update("UPDATE sys_config SET config_value = #{value}, updated_at = NOW() WHERE config_key = #{key}")
    int updateValueByKey(@Param("key") String key, @Param("value") String value);
}
