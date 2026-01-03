package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Timesheet;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工时记录仓储
 */
@Repository
public class TimesheetRepository extends AbstractRepository<TimesheetMapper, Timesheet> {

    /**
     * 按用户和日期范围查询
     */
    public List<Timesheet> findByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectByUserAndDateRange(userId, startDate, endDate);
    }

    /**
     * 统计用户某月总工时
     */
    public BigDecimal sumHoursByUserAndMonth(Long userId, int year, int month) {
        return baseMapper.sumHoursByUserAndMonth(userId, year, month);
    }

    /**
     * 统计案件总工时
     */
    public BigDecimal sumHoursByMatter(Long matterId) {
        return baseMapper.sumHoursByMatter(matterId);
    }

    /**
     * 查询待审批工时
     */
    public List<Timesheet> findPendingApproval() {
        return baseMapper.selectPendingApproval();
    }
}
