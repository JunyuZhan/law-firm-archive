package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.Regularization;
import com.lawfirm.infrastructure.persistence.mapper.RegularizationMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 转正申请 Repository. */
@Repository
public class RegularizationRepository
    extends AbstractRepository<RegularizationMapper, Regularization> {

  /**
   * 根据申请编号查询.
   *
   * @param applicationNo 申请编号
   * @return 转正申请
   */
  public Optional<Regularization> findByApplicationNo(final String applicationNo) {
    Regularization regularization =
        lambdaQuery().eq(Regularization::getApplicationNo, applicationNo).one();
    return Optional.ofNullable(regularization);
  }

  /**
   * 根据员工ID查询转正申请.
   *
   * @param employeeId 员工ID
   * @return 转正申请列表
   */
  public List<Regularization> findByEmployeeId(final Long employeeId) {
    return baseMapper.selectByEmployeeId(employeeId);
  }
}
