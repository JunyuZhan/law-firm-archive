package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PerformanceScore;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 绩效评分明细Mapper */
@Mapper
public interface PerformanceScoreMapper extends BaseMapper<PerformanceScore> {

  /**
   * 根据评估ID查询绩效评分明细.
   *
   * @param evaluationId 评估ID
   * @return 绩效评分明细列表
   */
  @Select(
      "SELECT * FROM hr_performance_score WHERE deleted = false AND evaluation_id = #{evaluationId}")
  List<PerformanceScore> findByEvaluationId(@Param("evaluationId") Long evaluationId);

  /**
   * 根据评估ID删除绩效评分明细.
   *
   * @param evaluationId 评估ID
   */
  @Select("DELETE FROM hr_performance_score WHERE evaluation_id = #{evaluationId}")
  void deleteByEvaluationId(@Param("evaluationId") Long evaluationId);
}
