package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PerformanceScore;
import com.lawfirm.infrastructure.persistence.mapper.PerformanceScoreMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 绩效评分明细仓储
 */
@Repository
public class PerformanceScoreRepository extends AbstractRepository<PerformanceScoreMapper, PerformanceScore> {

    public List<PerformanceScore> findByEvaluationId(Long evaluationId) {
        return baseMapper.findByEvaluationId(evaluationId);
    }

    public void deleteByEvaluationId(Long evaluationId) {
        baseMapper.deleteByEvaluationId(evaluationId);
    }
}
