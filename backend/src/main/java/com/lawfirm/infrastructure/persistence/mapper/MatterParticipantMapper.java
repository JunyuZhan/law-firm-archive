package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 案件参与人 Mapper
 */
@Mapper
public interface MatterParticipantMapper extends BaseMapper<MatterParticipant> {

    /**
     * 查询案件的所有参与人
     */
    @Select("""
        SELECT mp.*, u.real_name as user_name, u.position, u.compensation_type
        FROM matter_participant mp
        INNER JOIN sys_user u ON mp.user_id = u.id
        WHERE mp.matter_id = #{matterId} AND mp.deleted = false
        ORDER BY mp.role, mp.id
        """)
    List<MatterParticipant> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 查询案件的主办律师
     */
    @Select("""
        SELECT mp.* FROM matter_participant mp
        WHERE mp.matter_id = #{matterId} AND mp.role = 'LEAD' AND mp.status = 'ACTIVE' AND mp.deleted = false
        """)
    MatterParticipant selectLeadLawyer(@Param("matterId") Long matterId);

    /**
     * 删除案件的所有参与人
     */
    @Delete("DELETE FROM matter_participant WHERE matter_id = #{matterId}")
    void deleteByMatterId(@Param("matterId") Long matterId);

    /**
     * 检查用户是否已在案件团队中
     */
    @Select("""
        SELECT COUNT(*) FROM matter_participant
        WHERE matter_id = #{matterId} AND user_id = #{userId} AND status = 'ACTIVE' AND deleted = false
        """)
    int countByMatterIdAndUserId(@Param("matterId") Long matterId, @Param("userId") Long userId);
}

