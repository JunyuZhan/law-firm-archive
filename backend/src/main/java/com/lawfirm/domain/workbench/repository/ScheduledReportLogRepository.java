package com.lawfirm.domain.workbench.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.workbench.entity.ScheduledReportLog;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportLogMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 定时报表执行记录 Repository
 */
@Repository
public class ScheduledReportLogRepository extends ServiceImpl<ScheduledReportLogMapper, ScheduledReportLog> {

    /**
     * 查询最近一次执行记录
     */
    public Optional<ScheduledReportLog> findLatestByTaskId(Long taskId) {
        return Optional.ofNullable(baseMapper.selectLatestByTaskId(taskId));
    }
}
