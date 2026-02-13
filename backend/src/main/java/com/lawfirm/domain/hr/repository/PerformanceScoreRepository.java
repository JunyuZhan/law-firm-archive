package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PerformanceScore;
import com.lawfirm.infrastructure.persistence.mapper.PerformanceScoreMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 绩效评分明细仓储. */
@Repository
public class PerformanceScoreRepository
    extends AbstractRepository<PerformanceScoreMapper, PerformanceScore> {

  /**
   * 根据评价ID查询评分明细.
   *
   * @param evaluationId 评价ID
   * @return 评分明细列表
   */
  public List<PerformanceScore> findByEvaluationId(final Long evaluationId) {
    return baseMapper.findByEvaluationId(evaluationId);
  }

  /**
   * 根据评价ID删除评分明细.
   *
   * @param evaluationId 评价ID
   */
  public void deleteByEvaluationId(final Long evaluationId) {
    baseMapper.deleteByEvaluationId(evaluationId);
  }
}
