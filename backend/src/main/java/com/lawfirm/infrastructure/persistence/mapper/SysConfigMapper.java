package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.SysConfig;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 系统配置Mapper */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

  /**
   * 查询所有配置.
   *
   * @return 所有配置列表
   */
  @Select("SELECT * FROM sys_config WHERE deleted = false ORDER BY id")
  List<SysConfig> selectAllConfigs();

  /**
   * 根据键查询.
   *
   * @param key 配置键
   * @return 系统配置
   */
  @Select("SELECT * FROM sys_config WHERE config_key = #{key} AND deleted = false")
  SysConfig selectByKey(@Param("key") String key);

  /**
   * 更新配置值.
   *
   * @param key 配置键
   * @param value 配置值
   * @return 更新数量
   */
  @Update(
      "UPDATE sys_config SET config_value = #{value}, updated_at = NOW() WHERE config_key = #{key}")
  int updateValueByKey(@Param("key") String key, @Param("value") String value);

  /**
   * 根据键直接查询配置值.
   *
   * @param key 配置键
   * @return 配置值
   */
  @Select("SELECT config_value FROM sys_config WHERE config_key = #{key} AND deleted = false")
  String selectValueByKey(@Param("key") String key);
}
