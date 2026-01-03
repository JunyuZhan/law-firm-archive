package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PerformanceEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 绩效评价Mapper
 */
@Mapper
public interface PerformanceEvaluationMapper extends BaseMapper<PerformanceEvaluation> {

    @Select("SELECT * FROM hr_performance_evaluation WHERE deleted = false AND task_id = #{taskId} AND employee_id = #{employeeId}")
    List<PerformanceEvaluation> findByTaskAndEmployee(@Param("taskId") Long taskId, @Param("employeeId") Long employeeId);

    @Select("SELECT * FROM hr_performance_evaluation WHERE deleted = false AND task_id = #{taskId} AND evaluator_id = #{evaluatorId}")
    List<PerformanceEvaluation> findByTaskAndEvaluator(@Param("taskId") Long taskId, @Param("evaluatorId") Long evaluatorId);

    @Select("SELECT * FROM hr_performance_evaluation WHERE deleted = false AND task_id = #{taskId} " +
            "AND employee_id = #{employeeId} AND evaluation_type = #{evaluationType}")
    PerformanceEvaluation findByTaskEmployeeAndType(@Param("taskId") Long taskId, 
                                                     @Param("employeeId") Long employeeId,
                                                     @Param("evaluationType") String evaluationType);

    @Select("SELECT * FROM hr_performance_evaluation WHERE deleted = false AND evaluator_id = #{evaluatorId} AND status = 'PENDING'")
    List<PerformanceEvaluation> findPendingByEvaluator(@Param("evaluatorId") Long evaluatorId);

    @Select("SELECT grade, COUNT(*) as count FROM hr_performance_evaluation WHERE deleted = false " +
            "AND task_id = #{taskId} AND evaluation_type = 'SUPERVISOR' GROUP BY grade")
    List<Map<String, Object>> countByGrade(@Param("taskId") Long taskId);
}
