package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PerformanceIndicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 考核指标Mapper
 */
@Mapper
public interface PerformanceIndicatorMapper extends BaseMapper<PerformanceIndicator> {

    @Select("SELECT * FROM hr_performance_indicator WHERE deleted = false AND status = 'ACTIVE' ORDER BY sort_order ASC")
    List<PerformanceIndicator> findAllActive();

    @Select("SELECT * FROM hr_performance_indicator WHERE deleted = false AND status = 'ACTIVE' " +
            "AND (applicable_role = 'ALL' OR applicable_role = #{role}) ORDER BY sort_order ASC")
    List<PerformanceIndicator> findByRole(@Param("role") String role);

    @Select("SELECT * FROM hr_performance_indicator WHERE deleted = false AND category = #{category} ORDER BY sort_order ASC")
    List<PerformanceIndicator> findByCategory(@Param("category") String category);
}
