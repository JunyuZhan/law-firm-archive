package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.DevelopmentPlan;
import com.lawfirm.infrastructure.persistence.mapper.DevelopmentPlanMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 个人发展规划 Repository
 */
@Repository
public class DevelopmentPlanRepository extends AbstractRepository<DevelopmentPlanMapper, DevelopmentPlan> {

    /**
     * 根据规划编号查询
     */
    public Optional<DevelopmentPlan> findByPlanNo(String planNo) {
        return Optional.ofNullable(baseMapper.selectByPlanNo(planNo));
    }

    /**
     * 查询员工当年规划
     */
    public Optional<DevelopmentPlan> findByEmployeeAndYear(Long employeeId, Integer planYear) {
        return Optional.ofNullable(baseMapper.selectByEmployeeAndYear(employeeId, planYear));
    }
}
