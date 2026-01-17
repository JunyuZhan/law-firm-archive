package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.matter.entity.MatterStateCompensation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 国家赔偿案件业务信息 Mapper
 */
@Mapper
public interface MatterStateCompensationMapper extends BaseMapper<MatterStateCompensation> {

    /**
     * 根据案件ID查询国家赔偿信息
     */
    @Select("""
        SELECT * FROM matter_state_compensation
        WHERE matter_id = #{matterId} AND deleted = false
        LIMIT 1
        """)
    MatterStateCompensation selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 检查案件是否已存在国家赔偿信息
     */
    @Select("""
        SELECT COUNT(*) FROM matter_state_compensation
        WHERE matter_id = #{matterId} AND deleted = false
        """)
    int countByMatterId(@Param("matterId") Long matterId);
}
