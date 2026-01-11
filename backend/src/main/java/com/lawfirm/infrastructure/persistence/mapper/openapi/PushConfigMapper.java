package com.lawfirm.infrastructure.persistence.mapper.openapi;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.openapi.entity.PushConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 推送配置 Mapper
 */
@Mapper
public interface PushConfigMapper extends BaseMapper<PushConfig> {

    /**
     * 根据项目ID查询配置
     */
    @Select("SELECT * FROM openapi_push_config WHERE matter_id = #{matterId} AND deleted = false")
    PushConfig selectByMatterId(@Param("matterId") Long matterId);
}

