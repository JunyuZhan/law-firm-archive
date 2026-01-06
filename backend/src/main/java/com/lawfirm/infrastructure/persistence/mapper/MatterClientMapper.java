package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.matter.entity.MatterClient;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 项目-客户关联 Mapper
 */
@Mapper
public interface MatterClientMapper extends BaseMapper<MatterClient> {

    /**
     * 查询项目的所有客户关联
     */
    @Select("""
        SELECT mc.*, c.name as client_name, c.client_type
        FROM matter_client mc
        INNER JOIN crm_client c ON mc.client_id = c.id
        WHERE mc.matter_id = #{matterId} AND mc.deleted = false
        ORDER BY mc.is_primary DESC, mc.id
        """)
    List<MatterClient> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 查询项目的主要客户
     */
    @Select("""
        SELECT mc.* FROM matter_client mc
        WHERE mc.matter_id = #{matterId} AND mc.is_primary = true AND mc.deleted = false
        LIMIT 1
        """)
    MatterClient selectPrimaryClient(@Param("matterId") Long matterId);

    /**
     * 删除项目的所有客户关联（软删除）
     */
    @Update("UPDATE matter_client SET deleted = true WHERE matter_id = #{matterId}")
    void deleteByMatterId(@Param("matterId") Long matterId);

    /**
     * 检查客户是否已关联到项目
     */
    @Select("""
        SELECT COUNT(*) FROM matter_client
        WHERE matter_id = #{matterId} AND client_id = #{clientId} AND deleted = false
        """)
    int countByMatterIdAndClientId(@Param("matterId") Long matterId, @Param("clientId") Long clientId);
}

