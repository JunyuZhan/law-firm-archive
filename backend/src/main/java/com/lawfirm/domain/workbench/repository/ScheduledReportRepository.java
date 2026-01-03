package com.lawfirm.domain.workbench.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.workbench.entity.ScheduledReport;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 定时报表任务 Repository
 */
@Repository
public class ScheduledReportRepository extends AbstractRepository<ScheduledReportMapper, ScheduledReport> {

    /**
     * 根据任务编号查询
     */
    public Optional<ScheduledReport> findByTaskNo(String taskNo) {
        return Optional.ofNullable(baseMapper.selectByTaskNo(taskNo));
    }

    /**
     * 查询待执行的任务
     */
    public List<ScheduledReport> findPendingTasks(LocalDateTime now) {
        return baseMapper.selectPendingTasks(now);
    }

    /**
     * 更新执行统计
     */
    public void updateExecuteStats(Long id, LocalDateTime lastExecuteTime, String lastExecuteStatus,
                                   LocalDateTime nextExecuteTime, boolean success) {
        baseMapper.updateExecuteStats(id, lastExecuteTime, lastExecuteStatus, nextExecuteTime,
                success ? 1 : 0, success ? 0 : 1);
    }
}
