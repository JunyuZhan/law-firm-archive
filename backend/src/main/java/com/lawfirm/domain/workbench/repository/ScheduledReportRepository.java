package com.lawfirm.domain.workbench.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.workbench.entity.ScheduledReport;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 定时报表任务 Repository */
@Repository
public class ScheduledReportRepository
    extends AbstractRepository<ScheduledReportMapper, ScheduledReport> {

  /**
   * 根据任务编号查询
   *
   * @param taskNo 任务编号
   * @return 定时报表任务Optional
   */
  public Optional<ScheduledReport> findByTaskNo(final String taskNo) {
    return Optional.ofNullable(baseMapper.selectByTaskNo(taskNo));
  }

  /**
   * 查询待执行的任务
   *
   * @param now 当前时间
   * @return 待执行任务列表
   */
  public List<ScheduledReport> findPendingTasks(final LocalDateTime now) {
    return baseMapper.selectPendingTasks(now);
  }

  /**
   * 更新执行统计
   *
   * @param id 任务ID
   * @param lastExecuteTime 最后执行时间
   * @param lastExecuteStatus 最后执行状态
   * @param nextExecuteTime 下次执行时间
   * @param success 是否成功
   */
  public void updateExecuteStats(
      final Long id,
      final LocalDateTime lastExecuteTime,
      final String lastExecuteStatus,
      final LocalDateTime nextExecuteTime,
      final boolean success) {
    baseMapper.updateExecuteStats(
        id, lastExecuteTime, lastExecuteStatus, nextExecuteTime, success ? 1 : 0, success ? 0 : 1);
  }
}
