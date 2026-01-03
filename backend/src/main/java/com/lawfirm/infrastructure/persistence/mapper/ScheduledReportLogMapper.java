package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.workbench.entity.ScheduledReportLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 定时报表执行记录 Mapper
 */
@Mapper
public interface ScheduledReportLogMapper extends BaseMapper<ScheduledReportLog> {

    /**
     * 分页查询执行记录
     */
    @Select("""
        <script>
        SELECT * FROM workbench_scheduled_report_log
        WHERE 1=1
        <if test="taskId != null">
            AND task_id = #{taskId}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        ORDER BY execute_time DESC
        </script>
        """)
    IPage<ScheduledReportLog> selectLogPage(Page<ScheduledReportLog> page,
                                             @Param("taskId") Long taskId,
                                             @Param("status") String status);

    /**
     * 查询最近一次执行记录
     */
    @Select("SELECT * FROM workbench_scheduled_report_log WHERE task_id = #{taskId} ORDER BY execute_time DESC LIMIT 1")
    ScheduledReportLog selectLatestByTaskId(@Param("taskId") Long taskId);
}
