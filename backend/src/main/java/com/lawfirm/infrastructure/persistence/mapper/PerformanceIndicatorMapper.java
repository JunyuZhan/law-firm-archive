package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PerformanceIndicator;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 考核指标Mapper */
@Mapper
public interface PerformanceIndicatorMapper extends BaseMapper<PerformanceIndicator> {

  /**
   * 查询所有激活的考核指标.
   *
   * @return 激活的考核指标列表
   */
  @Select(
      "SELECT * FROM hr_performance_indicator WHERE deleted = false AND status = 'ACTIVE' ORDER BY sort_order ASC")
  List<PerformanceIndicator> findAllActive();

  /**
   * 根据角色查询考核指标.
   *
   * @param role 角色
   * @return 考核指标列表
   */
  @Select(
      "SELECT * FROM hr_performance_indicator WHERE deleted = false AND status = 'ACTIVE' "
          + "AND (applicable_role = 'ALL' OR applicable_role = #{role}) ORDER BY sort_order ASC")
  List<PerformanceIndicator> findByRole(@Param("role") String role);

  /**
   * 根据分类查询考核指标.
   *
   * @param category 分类
   * @return 考核指标列表
   */
  @Select(
      "SELECT * FROM hr_performance_indicator WHERE deleted = false AND category = #{category} ORDER BY sort_order ASC")
  List<PerformanceIndicator> findByCategory(@Param("category") String category);
}
