package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.DevelopmentPlan;
import com.lawfirm.infrastructure.persistence.mapper.DevelopmentPlanMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 个人发展规划 Repository. */
@Repository
public class DevelopmentPlanRepository
    extends AbstractRepository<DevelopmentPlanMapper, DevelopmentPlan> {

  /**
   * 根据规划编号查询.
   *
   * @param planNo 规划编号
   * @return 个人发展规划
   */
  public Optional<DevelopmentPlan> findByPlanNo(final String planNo) {
    return Optional.ofNullable(baseMapper.selectByPlanNo(planNo));
  }

  /**
   * 查询员工当年规划.
   *
   * @param employeeId 员工ID
   * @param planYear 规划年份
   * @return 个人发展规划
   */
  public Optional<DevelopmentPlan> findByEmployeeAndYear(
      final Long employeeId, final Integer planYear) {
    return Optional.ofNullable(baseMapper.selectByEmployeeAndYear(employeeId, planYear));
  }
}
