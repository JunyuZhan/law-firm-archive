package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.Resignation;
import com.lawfirm.infrastructure.persistence.mapper.ResignationMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 离职申请 Repository. */
@Repository
public class ResignationRepository extends AbstractRepository<ResignationMapper, Resignation> {

  /**
   * 根据申请编号查询.
   *
   * @param applicationNo 申请编号
   * @return 离职申请
   */
  public Optional<Resignation> findByApplicationNo(final String applicationNo) {
    Resignation resignation = lambdaQuery().eq(Resignation::getApplicationNo, applicationNo).one();
    return Optional.ofNullable(resignation);
  }

  /**
   * 根据员工ID查询离职申请.
   *
   * @param employeeId 员工ID
   * @return 离职申请列表
   */
  public List<Resignation> findByEmployeeId(final Long employeeId) {
    return baseMapper.selectByEmployeeId(employeeId);
  }
}
