package com.lawfirm.domain.workbench.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.workbench.entity.ScheduledReportLog;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportLogMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 定时报表执行记录 Repository */
@Repository
public class ScheduledReportLogRepository
    extends ServiceImpl<ScheduledReportLogMapper, ScheduledReportLog> {

  /**
   * 查询最近一次执行记录。
   *
   * @param taskId 任务ID
   * @return 最近一次执行记录，如果不存在则返回空
   */
  public Optional<ScheduledReportLog> findLatestByTaskId(final Long taskId) {
    return Optional.ofNullable(baseMapper.selectLatestByTaskId(taskId));
  }
}
