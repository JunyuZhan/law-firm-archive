package com.lawfirm.domain.hr.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PerformanceTask;
import com.lawfirm.infrastructure.persistence.mapper.PerformanceTaskMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 考核任务仓储
 */
@Repository
public class PerformanceTaskRepository extends AbstractRepository<PerformanceTaskMapper, PerformanceTask> {

    public IPage<PerformanceTask> findPage(Page<PerformanceTask> page, Integer year, String periodType, String status) {
        return baseMapper.findPage(page, year, periodType, status);
    }

    public List<PerformanceTask> findInProgress() {
        return baseMapper.findInProgress();
    }
}
