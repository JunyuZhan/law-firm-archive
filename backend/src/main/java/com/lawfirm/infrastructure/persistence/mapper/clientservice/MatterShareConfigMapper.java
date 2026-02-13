package com.lawfirm.infrastructure.persistence.mapper.clientservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.clientservice.entity.MatterShareConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 项目共享配置 Mapper */
@Mapper
public interface MatterShareConfigMapper extends BaseMapper<MatterShareConfig> {

  /**
   * 根据项目ID查询共享配置.
   *
   * @param matterId 项目ID
   * @return 项目共享配置
   */
  @Select(
      "SELECT * FROM openapi_matter_share_config WHERE matter_id = #{matterId} AND deleted = false")
  MatterShareConfig selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 更新启用状态.
   *
   * @param matterId 项目ID
   * @param enabled 是否启用
   * @param updatedBy 更新人ID
   * @return 更新数量
   */
  @Update(
      "UPDATE openapi_matter_share_config SET enabled = #{enabled}, "
          + "updated_at = CURRENT_TIMESTAMP, updated_by = #{updatedBy} "
          + "WHERE matter_id = #{matterId}")
  int updateEnabled(
      @Param("matterId") Long matterId,
      @Param("enabled") Boolean enabled,
      @Param("updatedBy") Long updatedBy);
}
