package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PerformanceScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 绩效评分明细Mapper
 */
@Mapper
public interface PerformanceScoreMapper extends BaseMapper<PerformanceScore> {

    @Select("SELECT * FROM hr_performance_score WHERE deleted = false AND evaluation_id = #{evaluationId}")
    List<PerformanceScore> findByEvaluationId(@Param("evaluationId") Long evaluationId);

    @Select("DELETE FROM hr_performance_score WHERE evaluation_id = #{evaluationId}")
    void deleteByEvaluationId(@Param("evaluationId") Long evaluationId);
}
