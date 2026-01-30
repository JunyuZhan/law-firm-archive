package com.lawfirm.domain.workbench.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.workbench.entity.Report;
import com.lawfirm.infrastructure.persistence.mapper.ReportMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 报表记录 Repository */
@Repository
public class ReportRepository extends AbstractRepository<ReportMapper, Report> {

  /**
   * 根据报表编号查询。
   *
   * @param reportNo 报表编号
   * @return 报表记录，如果不存在则返回空
   */
  public Optional<Report> findByReportNo(final String reportNo) {
    Report report = baseMapper.selectByReportNo(reportNo);
    return Optional.ofNullable(report);
  }
}
