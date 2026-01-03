package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.LeadFollowUp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 案源跟进记录 Mapper
 */
@Mapper
public interface LeadFollowUpMapper extends BaseMapper<LeadFollowUp> {

    /**
     * 查询案源的所有跟进记录
     */
    @Select("""
        SELECT f.*, u.real_name as follow_user_name
        FROM crm_lead_follow_up f
        LEFT JOIN sys_user u ON f.follow_user_id = u.id
        WHERE f.lead_id = #{leadId}
        ORDER BY f.created_at DESC
        """)
    List<LeadFollowUp> selectByLeadId(@Param("leadId") Long leadId);
}

