package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.Contract;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.type.Alias;
import org.springframework.stereotype.Repository;

/** 劳动合同 Repository. */
@Alias("HrContractRepository")
@Repository("hrContractRepository")
public class ContractRepository extends AbstractRepository<ContractMapper, Contract> {

  /**
   * 根据合同编号查询.
   *
   * @param contractNo 合同编号
   * @return 劳动合同
   */
  public Optional<Contract> findByContractNo(final String contractNo) {
    Contract contract = lambdaQuery().eq(Contract::getContractNo, contractNo).one();
    return Optional.ofNullable(contract);
  }

  /**
   * 根据员工ID查询生效中的合同.
   *
   * @param employeeId 员工ID
   * @return 劳动合同
   */
  public Optional<Contract> findActiveContractByEmployeeId(final Long employeeId) {
    Contract contract = baseMapper.selectActiveContractByEmployeeId(employeeId);
    return Optional.ofNullable(contract);
  }

  /**
   * 根据员工ID查询所有合同.
   *
   * @param employeeId 员工ID
   * @return 劳动合同列表
   */
  public List<Contract> findByEmployeeId(final Long employeeId) {
    return baseMapper.selectByEmployeeId(employeeId);
  }
}
