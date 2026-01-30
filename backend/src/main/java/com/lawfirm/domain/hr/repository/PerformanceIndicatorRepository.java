package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PerformanceIndicator;
import com.lawfirm.infrastructure.persistence.mapper.PerformanceIndicatorMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 考核指标仓储. */
@Repository
public class PerformanceIndicatorRepository
    extends AbstractRepository<PerformanceIndicatorMapper, PerformanceIndicator> {

  /**
   * 查询所有启用的考核指标.
   *
   * @return 考核指标列表
   */
  public List<PerformanceIndicator> findAllActive() {
    return baseMapper.findAllActive();
  }

  /**
   * 根据角色查询考核指标.
   *
   * @param role 角色
   * @return 考核指标列表
   */
  public List<PerformanceIndicator> findByRole(final String role) {
    return baseMapper.findByRole(role);
  }

  /**
   * 根据分类查询考核指标.
   *
   * @param category 分类
   * @return 考核指标列表
   */
  public List<PerformanceIndicator> findByCategory(final String category) {
    return baseMapper.findByCategory(category);
  }
}
