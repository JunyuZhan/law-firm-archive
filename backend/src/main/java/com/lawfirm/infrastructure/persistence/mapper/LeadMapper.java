package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.Lead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 案源线索 Mapper
 */
@Mapper
public interface LeadMapper extends BaseMapper<Lead> {

    /**
     * 分页查询案源列表
     */
    @Select("""
        <script>
        SELECT l.*, u1.real_name as originator_name, u2.real_name as responsible_user_name
        FROM crm_lead l
        LEFT JOIN sys_user u1 ON l.originator_id = u1.id
        LEFT JOIN sys_user u2 ON l.responsible_user_id = u2.id
        WHERE l.deleted = false
        <if test="leadName != null and leadName != ''">
            AND l.lead_name LIKE CONCAT('%', #{leadName}, '%')
        </if>
        <if test="status != null and status != ''">
            AND l.status = #{status}
        </if>
        <if test="originatorId != null">
            AND l.originator_id = #{originatorId}
        </if>
        <if test="responsibleUserId != null">
            AND l.responsible_user_id = #{responsibleUserId}
        </if>
        <if test="sourceChannel != null and sourceChannel != ''">
            AND l.source_channel = #{sourceChannel}
        </if>
        ORDER BY l.created_at DESC
        </script>
        """)
    List<Lead> selectLeadPage(
            @Param("leadName") String leadName,
            @Param("status") String status,
            @Param("originatorId") Long originatorId,
            @Param("responsibleUserId") Long responsibleUserId,
            @Param("sourceChannel") String sourceChannel
    );

    /**
     * 根据案源编号查询
     */
    @Select("SELECT * FROM crm_lead WHERE lead_no = #{leadNo} AND deleted = false")
    Lead selectByLeadNo(@Param("leadNo") String leadNo);

    /**
     * 统计总案源数（M2-033）
     */
    @Select("SELECT COUNT(*) FROM crm_lead WHERE deleted = false")
    Long countTotalLeads();

    /**
     * 统计已转化案源数（M2-034）
     */
    @Select("SELECT COUNT(*) FROM crm_lead WHERE status = 'CONVERTED' AND deleted = false")
    Long countConvertedLeads();

    /**
     * 按来源渠道统计（M2-033）
     */
    @Select("SELECT " +
            "source_channel, " +
            "COUNT(*) as lead_count, " +
            "COUNT(CASE WHEN status = 'CONVERTED' THEN 1 END) as converted_count, " +
            "CASE " +
            "  WHEN COUNT(*) > 0 THEN (COUNT(CASE WHEN status = 'CONVERTED' THEN 1 END)::DECIMAL / COUNT(*) * 100) " +
            "  ELSE 0 " +
            "END as conversion_rate " +
            "FROM crm_lead " +
            "WHERE deleted = false " +
            "GROUP BY source_channel " +
            "ORDER BY lead_count DESC")
    List<Map<String, Object>> countBySourceChannel();

    /**
     * 按状态统计（M2-033）
     */
    @Select("SELECT " +
            "status, " +
            "COUNT(*) as lead_count " +
            "FROM crm_lead " +
            "WHERE deleted = false " +
            "GROUP BY status " +
            "ORDER BY lead_count DESC")
    List<Map<String, Object>> countByStatus();

    /**
     * 按案源人统计（M2-033）
     */
    @Select("SELECT " +
            "l.originator_id, " +
            "u.real_name as originator_name, " +
            "COUNT(*) as lead_count, " +
            "COUNT(CASE WHEN l.status = 'CONVERTED' THEN 1 END) as converted_count, " +
            "CASE " +
            "  WHEN COUNT(*) > 0 THEN (COUNT(CASE WHEN l.status = 'CONVERTED' THEN 1 END)::DECIMAL / COUNT(*) * 100) " +
            "  ELSE 0 " +
            "END as conversion_rate " +
            "FROM crm_lead l " +
            "LEFT JOIN sys_user u ON l.originator_id = u.id " +
            "WHERE l.deleted = false AND l.originator_id IS NOT NULL " +
            "GROUP BY l.originator_id, u.real_name " +
            "ORDER BY lead_count DESC")
    List<Map<String, Object>> countByOriginator();

    /**
     * 转化率分析（按时间统计）（M2-034）
     */
    @Select("SELECT " +
            "TO_CHAR(DATE_TRUNC('month', converted_at), 'YYYY-MM') as period, " +
            "COUNT(*) as converted_count " +
            "FROM crm_lead " +
            "WHERE status = 'CONVERTED' AND deleted = false " +
            "AND converted_at >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '11 months') " +
            "GROUP BY DATE_TRUNC('month', converted_at) " +
            "ORDER BY period")
    List<Map<String, Object>> countConversionTrend();

    /**
     * 转化率分析（按来源渠道和状态）（M2-034）
     */
    @Select("SELECT " +
            "source_channel, " +
            "COUNT(*) as total_count, " +
            "COUNT(CASE WHEN status = 'CONVERTED' THEN 1 END) as converted_count, " +
            "COUNT(CASE WHEN status = 'ABANDONED' THEN 1 END) as abandoned_count, " +
            "COUNT(CASE WHEN status IN ('PENDING', 'FOLLOWING') THEN 1 END) as active_count, " +
            "CASE " +
            "  WHEN COUNT(*) > 0 THEN (COUNT(CASE WHEN status = 'CONVERTED' THEN 1 END)::DECIMAL / COUNT(*) * 100) " +
            "  ELSE 0 " +
            "END as conversion_rate " +
            "FROM crm_lead " +
            "WHERE deleted = false " +
            "GROUP BY source_channel " +
            "ORDER BY conversion_rate DESC")
    List<Map<String, Object>> analyzeConversionRate();
}

