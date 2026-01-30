package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PerformanceEvaluation;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 绩效评价Mapper */
@Mapper
public interface PerformanceEvaluationMapper extends BaseMapper<PerformanceEvaluation> {

  /**
   * 根据任务和员工查询绩效评价.
   *
   * @param taskId 任务ID
   * @param employeeId 员工ID
   * @return 绩效评价列表
   */
  @Select(
      "SELECT * FROM hr_performance_evaluation WHERE deleted = false "
          + "AND task_id = #{taskId} AND employee_id = #{employeeId}")
  List<PerformanceEvaluation> findByTaskAndEmployee(
      @Param("taskId") Long taskId, @Param("employeeId") Long employeeId);

  /**
   * 根据任务和评价人查询绩效评价.
   *
   * @param taskId 任务ID
   * @param evaluatorId 评价人ID
   * @return 绩效评价列表
   */
  @Select(
      "SELECT * FROM hr_performance_evaluation WHERE deleted = false "
          + "AND task_id = #{taskId} AND evaluator_id = #{evaluatorId}")
  List<PerformanceEvaluation> findByTaskAndEvaluator(
      @Param("taskId") Long taskId, @Param("evaluatorId") Long evaluatorId);

  /**
   * 根据任务、员工和评价类型查询绩效评价.
   *
   * @param taskId 任务ID
   * @param employeeId 员工ID
   * @param evaluationType 评价类型
   * @return 绩效评价
   */
  @Select(
      "SELECT * FROM hr_performance_evaluation WHERE deleted = false AND task_id = #{taskId} "
          + "AND employee_id = #{employeeId} AND evaluation_type = #{evaluationType}")
  PerformanceEvaluation findByTaskEmployeeAndType(
      @Param("taskId") Long taskId,
      @Param("employeeId") Long employeeId,
      @Param("evaluationType") String evaluationType);

  /**
   * 查询评价人待评价的绩效评价.
   *
   * @param evaluatorId 评价人ID
   * @return 待评价的绩效评价列表
   */
  @Select(
      "SELECT * FROM hr_performance_evaluation WHERE deleted = false "
          + "AND evaluator_id = #{evaluatorId} AND status = 'PENDING'")
  List<PerformanceEvaluation> findPendingByEvaluator(@Param("evaluatorId") Long evaluatorId);

  /**
   * 按等级统计绩效评价.
   *
   * @param taskId 任务ID
   * @return 统计结果
   */
  @Select(
      "SELECT grade, COUNT(*) as count FROM hr_performance_evaluation WHERE deleted = false "
          + "AND task_id = #{taskId} AND evaluation_type = 'SUPERVISOR' GROUP BY grade")
  List<Map<String, Object>> countByGrade(@Param("taskId") Long taskId);
}
