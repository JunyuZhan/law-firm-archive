package com.lawfirm.domain.hr.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.hr.entity.DevelopmentMilestone;
import com.lawfirm.infrastructure.persistence.mapper.DevelopmentMilestoneMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 发展规划里程碑 Repository. */
@Repository
public class DevelopmentMilestoneRepository
    extends ServiceImpl<DevelopmentMilestoneMapper, DevelopmentMilestone> {

  /**
   * 查询规划的所有里程碑.
   *
   * @param planId 规划ID
   * @return 里程碑列表
   */
  public List<DevelopmentMilestone> findByPlanId(final Long planId) {
    return baseMapper.selectByPlanId(planId);
  }

  /**
   * 统计已完成里程碑数量.
   *
   * @param planId 规划ID
   * @return 已完成数量
   */
  public int countCompleted(final Long planId) {
    return baseMapper.countCompleted(planId);
  }

  /**
   * 统计总里程碑数量.
   *
   * @param planId 规划ID
   * @return 总数量
   */
  public int countTotal(final Long planId) {
    return baseMapper.countTotal(planId);
  }

  /**
   * 删除规划的所有里程碑.
   *
   * @param planId 规划ID
   */
  public void deleteByPlanId(final Long planId) {
    baseMapper.deleteByPlanId(planId);
  }
}
