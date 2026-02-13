package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.FinanceContractAmendment;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractAmendmentMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 财务合同变更记录仓储 */
@Repository
public class FinanceContractAmendmentRepository
    extends AbstractRepository<FinanceContractAmendmentMapper, FinanceContractAmendment> {

  /**
   * 根据合同ID查询变更记录
   *
   * @param contractId 合同ID
   * @return 变更记录列表
   */
  public List<FinanceContractAmendment> findByContractId(final Long contractId) {
    return baseMapper.selectByContractId(contractId);
  }

  /**
   * 查询待处理的变更记录
   *
   * @return 待处理的变更记录列表
   */
  public List<FinanceContractAmendment> findPendingAmendments() {
    return baseMapper.selectPendingAmendments();
  }

  /**
   * 统计合同的待处理变更数量
   *
   * @param contractId 合同ID
   * @return 待处理变更数量
   */
  public int countPendingByContractId(final Long contractId) {
    return baseMapper.countPendingByContractId(contractId);
  }
}
