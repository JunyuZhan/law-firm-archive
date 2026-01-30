package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.DevelopmentMilestone;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 发展规划里程碑 Mapper */
@Mapper
public interface DevelopmentMilestoneMapper extends BaseMapper<DevelopmentMilestone> {

  /**
   * 查询规划的所有里程碑.
   *
   * @param planId 规划ID
   * @return 里程碑列表
   */
  @Select(
      "SELECT * FROM hr_development_milestone WHERE plan_id = #{planId} ORDER BY sort_order ASC")
  List<DevelopmentMilestone> selectByPlanId(@Param("planId") Long planId);

  /**
   * 统计已完成里程碑数量.
   *
   * @param planId 规划ID
   * @return 已完成数量
   */
  @Select(
      "SELECT COUNT(*) FROM hr_development_milestone WHERE plan_id = #{planId} AND status = 'COMPLETED'")
  int countCompleted(@Param("planId") Long planId);

  /**
   * 统计总里程碑数量.
   *
   * @param planId 规划ID
   * @return 总数量
   */
  @Select("SELECT COUNT(*) FROM hr_development_milestone WHERE plan_id = #{planId}")
  int countTotal(@Param("planId") Long planId);

  /**
   * 删除规划的所有里程碑.
   *
   * @param planId 规划ID
   * @return 删除行数
   */
  @Update("DELETE FROM hr_development_milestone WHERE plan_id = #{planId}")
  int deleteByPlanId(@Param("planId") Long planId);
}
