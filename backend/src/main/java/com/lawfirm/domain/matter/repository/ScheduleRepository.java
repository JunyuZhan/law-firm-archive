package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Schedule;
import com.lawfirm.infrastructure.persistence.mapper.ScheduleMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 日程仓储
 */
@Repository
public class ScheduleRepository extends AbstractRepository<ScheduleMapper, Schedule> {

    /**
     * 查询用户某天的日程
     */
    public List<Schedule> findByUserAndDate(Long userId, LocalDate date) {
        return baseMapper.selectByUserAndDate(userId, date);
    }

    /**
     * 查询需要提醒的日程
     */
    public List<Schedule> findNeedReminder() {
        return baseMapper.selectNeedReminder();
    }
}
