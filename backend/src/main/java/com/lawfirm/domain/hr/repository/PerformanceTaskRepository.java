package com.lawfirm.domain.hr.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PerformanceTask;
import com.lawfirm.infrastructure.persistence.mapper.PerformanceTaskMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 考核任务仓储. */
@Repository
public class PerformanceTaskRepository
    extends AbstractRepository<PerformanceTaskMapper, PerformanceTask> {

  /**
   * 分页查询考核任务.
   *
   * @param page 分页参数
   * @param year 年份
   * @param periodType 周期类型
   * @param status 状态
   * @return 考核任务分页结果
   */
  public IPage<PerformanceTask> findPage(
      final Page<PerformanceTask> page,
      final Integer year,
      final String periodType,
      final String status) {
    return baseMapper.findPage(page, year, periodType, status);
  }

  /**
   * 查询进行中的考核任务.
   *
   * @return 考核任务列表
   */
  public List<PerformanceTask> findInProgress() {
    return baseMapper.findInProgress();
  }
}
