package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Prepayment;
import com.lawfirm.infrastructure.persistence.mapper.PrepaymentMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 预收款仓储 */
@Repository
public class PrepaymentRepository extends AbstractRepository<PrepaymentMapper, Prepayment> {

  /**
   * 查询客户的有效预收款
   *
   * @param clientId 客户ID
   * @return 预收款列表
   */
  public List<Prepayment> findActiveByClientId(final Long clientId) {
    return baseMapper.selectActiveByClientId(clientId);
  }

  /**
   * 查询项目关联的预收款
   *
   * @param matterId 项目ID
   * @return 预收款列表
   */
  public List<Prepayment> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 查询合同关联的预收款
   *
   * @param contractId 合同ID
   * @return 预收款列表
   */
  public List<Prepayment> findByContractId(final Long contractId) {
    return baseMapper.selectByContractId(contractId);
  }
}
