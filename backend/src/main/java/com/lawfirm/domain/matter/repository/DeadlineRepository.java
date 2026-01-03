package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Deadline;
import com.lawfirm.infrastructure.persistence.mapper.DeadlineMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 期限提醒 Repository
 */
@Repository
public class DeadlineRepository extends AbstractRepository<DeadlineMapper, Deadline> {

    /**
     * 查询需要提醒的期限
     */
    public List<Deadline> findNeedReminder() {
        return baseMapper.selectNeedReminder();
    }

    /**
     * 查询即将过期的期限
     */
    public List<Deadline> findUpcomingDeadlines() {
        return baseMapper.selectUpcomingDeadlines();
    }

    /**
     * 根据项目ID查询期限列表
     */
    public List<Deadline> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }
}

