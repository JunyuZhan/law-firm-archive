package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PerformanceEvaluation;
import com.lawfirm.infrastructure.persistence.mapper.PerformanceEvaluationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 绩效评价仓储
 */
@Repository
public class PerformanceEvaluationRepository extends AbstractRepository<PerformanceEvaluationMapper, PerformanceEvaluation> {

    public List<PerformanceEvaluation> findByTaskAndEmployee(Long taskId, Long employeeId) {
        return baseMapper.findByTaskAndEmployee(taskId, employeeId);
    }

    public List<PerformanceEvaluation> findByTaskAndEvaluator(Long taskId, Long evaluatorId) {
        return baseMapper.findByTaskAndEvaluator(taskId, evaluatorId);
    }

    public PerformanceEvaluation findByTaskEmployeeAndType(Long taskId, Long employeeId, String evaluationType) {
        return baseMapper.findByTaskEmployeeAndType(taskId, employeeId, evaluationType);
    }

    public List<PerformanceEvaluation> findPendingByEvaluator(Long evaluatorId) {
        return baseMapper.findPendingByEvaluator(evaluatorId);
    }

    public List<Map<String, Object>> countByGrade(Long taskId) {
        return baseMapper.countByGrade(taskId);
    }
}
