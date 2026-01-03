package com.lawfirm.domain.workbench.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.workbench.entity.Report;
import com.lawfirm.infrastructure.persistence.mapper.ReportMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 报表记录 Repository
 */
@Repository
public class ReportRepository extends AbstractRepository<ReportMapper, Report> {

    /**
     * 根据报表编号查询
     */
    public Optional<Report> findByReportNo(String reportNo) {
        Report report = baseMapper.selectByReportNo(reportNo);
        return Optional.ofNullable(report);
    }
}

