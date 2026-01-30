package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.workbench.entity.ScheduledReport;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 定时报表任务 Mapper */
@Mapper
public interface ScheduledReportMapper extends BaseMapper<ScheduledReport> {

  /**
   * 分页查询定时报表任务.
   *
   * @param page 分页对象
   * @param keyword 关键词
   * @param status 状态
   * @param createdBy 创建人ID
   * @return 定时报表任务分页结果
   */
  @Select(
      """
        <script>
        SELECT sr.*, rt.template_name
        FROM workbench_scheduled_report sr
        LEFT JOIN workbench_report_template rt ON sr.template_id = rt.id
        WHERE sr.deleted = false
        <if test="keyword != null and keyword != ''">
            AND (sr.task_name LIKE CONCAT('%', #{keyword}, '%') OR sr.description LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null and status != ''">
            AND sr.status = #{status}
        </if>
        <if test="createdBy != null">
            AND sr.created_by = #{createdBy}
        </if>
        ORDER BY sr.created_at DESC
        </script>
        """)
  IPage<ScheduledReport> selectScheduledReportPage(
      Page<ScheduledReport> page,
      @Param("keyword") String keyword,
      @Param("status") String status,
      @Param("createdBy") Long createdBy);

  /**
   * 根据任务编号查询.
   *
   * @param taskNo 任务编号
   * @return 定时报表任务
   */
  @Select(
      "SELECT * FROM workbench_scheduled_report WHERE task_no = #{taskNo} AND deleted = false LIMIT 1")
  ScheduledReport selectByTaskNo(@Param("taskNo") String taskNo);

  /**
   * 查询待执行的任务.
   *
   * @param now 当前时间
   * @return 待执行任务列表
   */
  @Select(
      """
        SELECT * FROM workbench_scheduled_report
        WHERE deleted = false
        AND status = 'ACTIVE'
        AND next_execute_time <= #{now}
        ORDER BY next_execute_time ASC
        """)
  List<ScheduledReport> selectPendingTasks(@Param("now") LocalDateTime now);

  /**
   * 更新执行统计.
   *
   * @param id 任务ID
   * @param lastExecuteTime 最后执行时间
   * @param lastExecuteStatus 最后执行状态
   * @param nextExecuteTime 下次执行时间
   * @param successIncrement 成功次数增量
   * @param failIncrement 失败次数增量
   * @return 更新数量
   */
  @Update(
      """
        UPDATE workbench_scheduled_report
        SET last_execute_time = #{lastExecuteTime},
            last_execute_status = #{lastExecuteStatus},
            next_execute_time = #{nextExecuteTime},
            total_execute_count = total_execute_count + 1,
            success_count = success_count + #{successIncrement},
            fail_count = fail_count + #{failIncrement},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
  int updateExecuteStats(
      @Param("id") Long id,
      @Param("lastExecuteTime") LocalDateTime lastExecuteTime,
      @Param("lastExecuteStatus") String lastExecuteStatus,
      @Param("nextExecuteTime") LocalDateTime nextExecuteTime,
      @Param("successIncrement") int successIncrement,
      @Param("failIncrement") int failIncrement);
}
