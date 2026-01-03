package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.DevelopmentMilestone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 发展规划里程碑 Mapper
 */
@Mapper
public interface DevelopmentMilestoneMapper extends BaseMapper<DevelopmentMilestone> {

    /**
     * 查询规划的所有里程碑
     */
    @Select("SELECT * FROM hr_development_milestone WHERE plan_id = #{planId} ORDER BY sort_order ASC")
    List<DevelopmentMilestone> selectByPlanId(@Param("planId") Long planId);

    /**
     * 统计已完成里程碑数量
     */
    @Select("SELECT COUNT(*) FROM hr_development_milestone WHERE plan_id = #{planId} AND status = 'COMPLETED'")
    int countCompleted(@Param("planId") Long planId);

    /**
     * 统计总里程碑数量
     */
    @Select("SELECT COUNT(*) FROM hr_development_milestone WHERE plan_id = #{planId}")
    int countTotal(@Param("planId") Long planId);

    /**
     * 删除规划的所有里程碑
     */
    @Update("DELETE FROM hr_development_milestone WHERE plan_id = #{planId}")
    int deleteByPlanId(@Param("planId") Long planId);
}
