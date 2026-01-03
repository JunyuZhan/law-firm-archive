package com.lawfirm.domain.hr.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.hr.entity.DevelopmentMilestone;
import com.lawfirm.infrastructure.persistence.mapper.DevelopmentMilestoneMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 发展规划里程碑 Repository
 */
@Repository
public class DevelopmentMilestoneRepository extends ServiceImpl<DevelopmentMilestoneMapper, DevelopmentMilestone> {

    /**
     * 查询规划的所有里程碑
     */
    public List<DevelopmentMilestone> findByPlanId(Long planId) {
        return baseMapper.selectByPlanId(planId);
    }

    /**
     * 统计已完成里程碑数量
     */
    public int countCompleted(Long planId) {
        return baseMapper.countCompleted(planId);
    }

    /**
     * 统计总里程碑数量
     */
    public int countTotal(Long planId) {
        return baseMapper.countTotal(planId);
    }

    /**
     * 删除规划的所有里程碑
     */
    public void deleteByPlanId(Long planId) {
        baseMapper.deleteByPlanId(planId);
    }
}
