package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.matter.entity.Deadline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 期限提醒 Mapper
 */
@Mapper
public interface DeadlineMapper extends BaseMapper<Deadline> {

    /**
     * 分页查询期限提醒
     */
    @Select("""
            <script>
            SELECT md.*, m.name as matter_name, m.matter_no
            FROM matter_deadline md
            LEFT JOIN matter m ON md.matter_id = m.id
            WHERE md.deleted = false
            <if test='query.matterId != null'>
                AND md.matter_id = #{query.matterId}
            </if>
            <if test='query.deadlineType != null and query.deadlineType != ""'>
                AND md.deadline_type = #{query.deadlineType}
            </if>
            <if test='query.status != null and query.status != ""'>
                AND md.status = #{query.status}
            </if>
            <if test='query.deadlineDateStart != null'>
                AND md.deadline_date &gt;= #{query.deadlineDateStart}
            </if>
            <if test='query.deadlineDateEnd != null'>
                AND md.deadline_date &lt;= #{query.deadlineDateEnd}
            </if>
            ORDER BY md.deadline_date ASC
            </script>
            """)
    IPage<Deadline> selectDeadlinePage(Page<Deadline> page, @Param("query") com.lawfirm.application.matter.dto.DeadlineQueryDTO query);

    /**
     * 查询需要提醒的期限（未发送提醒且即将到期）
     */
    @Select("""
            SELECT md.*, m.name as matter_name, m.lead_lawyer_id, u.real_name as lead_lawyer_name
            FROM matter_deadline md
            LEFT JOIN matter m ON md.matter_id = m.id
            LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id
            WHERE md.deleted = false
            AND md.status = 'ACTIVE'
            AND md.reminder_sent = false
            AND md.deadline_date BETWEEN CURRENT_DATE AND CURRENT_DATE + (md.reminder_days || ' days')::interval
            ORDER BY md.deadline_date ASC
            """)
    List<Deadline> selectNeedReminder();

    /**
     * 查询即将过期的期限（3天内）
     */
    @Select("""
            SELECT md.*, m.name as matter_name, m.lead_lawyer_id
            FROM matter_deadline md
            LEFT JOIN matter m ON md.matter_id = m.id
            WHERE md.deleted = false
            AND md.status = 'ACTIVE'
            AND md.deadline_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '3 days'
            ORDER BY md.deadline_date ASC
            """)
    List<Deadline> selectUpcomingDeadlines();

    /**
     * 根据项目ID查询期限列表
     */
    @Select("""
            SELECT md.*
            FROM matter_deadline md
            WHERE md.matter_id = #{matterId}
            AND md.deleted = false
            ORDER BY md.deadline_date ASC
            """)
    List<Deadline> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 查询用户即将到期的期限
     */
    @Select("""
            SELECT md.*, m.name as matter_name, m.matter_no
            FROM matter_deadline md
            LEFT JOIN matter m ON md.matter_id = m.id
            LEFT JOIN matter_participant mp ON m.id = mp.matter_id AND mp.deleted = false
            WHERE md.deleted = false
            AND md.status = 'ACTIVE'
            AND md.deadline_date >= CURRENT_DATE
            AND md.deadline_date <= CURRENT_DATE + (#{days} || ' days')::interval
            AND (m.lead_lawyer_id = #{userId} OR mp.user_id = #{userId})
            GROUP BY md.id, m.name, m.matter_no
            ORDER BY md.deadline_date ASC
            LIMIT #{limit}
            """)
    List<Deadline> selectMyUpcoming(@Param("userId") Long userId, @Param("days") Integer days, @Param("limit") Integer limit);
}

