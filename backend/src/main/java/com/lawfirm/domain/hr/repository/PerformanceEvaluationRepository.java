package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PerformanceEvaluation;
import com.lawfirm.infrastructure.persistence.mapper.PerformanceEvaluationMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** 绩效评价仓储. */
@Repository
public class PerformanceEvaluationRepository
    extends AbstractRepository<PerformanceEvaluationMapper, PerformanceEvaluation> {

  /**
   * 根据任务和员工查询绩效评价.
   *
   * @param taskId 任务ID
   * @param employeeId 员工ID
   * @return 绩效评价列表
   */
  public List<PerformanceEvaluation> findByTaskAndEmployee(
      final Long taskId, final Long employeeId) {
    return baseMapper.findByTaskAndEmployee(taskId, employeeId);
  }

  /**
   * 根据任务和评价人查询绩效评价.
   *
   * @param taskId 任务ID
   * @param evaluatorId 评价人ID
   * @return 绩效评价列表
   */
  public List<PerformanceEvaluation> findByTaskAndEvaluator(
      final Long taskId, final Long evaluatorId) {
    return baseMapper.findByTaskAndEvaluator(taskId, evaluatorId);
  }

  /**
   * 根据任务、员工和类型查询绩效评价.
   *
   * @param taskId 任务ID
   * @param employeeId 员工ID
   * @param evaluationType 评价类型
   * @return 绩效评价
   */
  public PerformanceEvaluation findByTaskEmployeeAndType(
      final Long taskId, final Long employeeId, final String evaluationType) {
    return baseMapper.findByTaskEmployeeAndType(taskId, employeeId, evaluationType);
  }

  /**
   * 查询评价人待评价的列表.
   *
   * @param evaluatorId 评价人ID
   * @return 待评价的绩效评价列表
   */
  public List<PerformanceEvaluation> findPendingByEvaluator(final Long evaluatorId) {
    return baseMapper.findPendingByEvaluator(evaluatorId);
  }

  /**
   * 按等级统计评价数量.
   *
   * @param taskId 任务ID
   * @return 等级统计结果
   */
  public List<Map<String, Object>> countByGrade(final Long taskId) {
    return baseMapper.countByGrade(taskId);
  }
}
