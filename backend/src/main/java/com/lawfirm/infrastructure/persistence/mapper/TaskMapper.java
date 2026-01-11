package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.matter.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 任务Mapper
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 分页查询任务
     */
    @Select("<script>" +
            "SELECT * FROM task WHERE deleted = false " +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='assigneeId != null'> AND assignee_id = #{assigneeId} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='priority != null and priority != \"\"'> AND priority = #{priority} </if>" +
            "<if test='title != null and title != \"\"'> AND title LIKE '%' || #{title} || '%' </if>" +
            "ORDER BY " +
            "CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END, " +
            "due_date ASC NULLS LAST, created_at DESC" +
            "</script>")
    IPage<Task> selectTaskPage(Page<Task> page,
                               @Param("matterId") Long matterId,
                               @Param("assigneeId") Long assigneeId,
                               @Param("status") String status,
                               @Param("priority") String priority,
                               @Param("title") String title);

    /**
     * 查询我的待办任务
     */
    @Select("SELECT * FROM task WHERE assignee_id = #{userId} AND status IN ('TODO', 'IN_PROGRESS') AND deleted = false ORDER BY due_date ASC NULLS LAST")
    List<Task> selectMyTodoTasks(@Param("userId") Long userId);

    /**
     * 查询即将到期的任务
     */
    @Select("SELECT * FROM task WHERE due_date BETWEEN #{today} AND #{deadline} AND status IN ('TODO', 'IN_PROGRESS') AND deleted = false ORDER BY due_date")
    List<Task> selectUpcomingTasks(@Param("today") LocalDate today, @Param("deadline") LocalDate deadline);

    /**
     * 查询逾期任务
     */
    @Select("SELECT * FROM task WHERE due_date < #{today} AND status IN ('TODO', 'IN_PROGRESS') AND deleted = false ORDER BY due_date")
    List<Task> selectOverdueTasks(@Param("today") LocalDate today);

    /**
     * 统计案件任务数
     */
    @Select("SELECT COUNT(*) FROM task WHERE matter_id = #{matterId} AND deleted = false")
    int countByMatter(@Param("matterId") Long matterId);

    /**
     * 统计案件已完成任务数
     */
    @Select("SELECT COUNT(*) FROM task WHERE matter_id = #{matterId} AND status = 'COMPLETED' AND deleted = false")
    int countCompletedByMatter(@Param("matterId") Long matterId);

    /**
     * 统计用户待办任务数
     */
    @Select("SELECT COUNT(*) FROM task WHERE assignee_id = #{userId} AND status IN ('TODO', 'IN_PROGRESS') AND deleted = false")
    int countPendingByAssigneeId(@Param("userId") Long userId);

    /**
     * 统计用户逾期任务数
     */
    @Select("SELECT COUNT(*) FROM task WHERE assignee_id = #{userId} AND status IN ('TODO', 'IN_PROGRESS') AND due_date < CURRENT_DATE AND deleted = false")
    int countOverdueByAssigneeId(@Param("userId") Long userId);

    /**
     * 查询用户待办任务（限制数量）
     */
    @Select("SELECT * FROM task WHERE assignee_id = #{userId} AND status IN ('TODO', 'IN_PROGRESS') AND deleted = false ORDER BY due_date ASC NULLS LAST LIMIT #{limit}")
    List<Task> selectPendingByAssigneeId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询需要发送自定义提醒的任务
     * 条件：reminder_date在指定时间范围内，且未发送过提醒，任务未完成
     */
    @Select("SELECT * FROM task WHERE reminder_date BETWEEN #{startTime} AND #{endTime} " +
            "AND (reminder_sent IS NULL OR reminder_sent = false) " +
            "AND status IN ('TODO', 'IN_PROGRESS') AND deleted = false")
    List<Task> selectTasksNeedReminder(@Param("startTime") java.time.LocalDateTime startTime, 
                                        @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 根据项目ID查询任务列表
     */
    @Select("SELECT * FROM task WHERE matter_id = #{matterId} AND deleted = false ORDER BY due_date ASC NULLS LAST")
    List<Task> selectByMatterId(@Param("matterId") Long matterId);
}
