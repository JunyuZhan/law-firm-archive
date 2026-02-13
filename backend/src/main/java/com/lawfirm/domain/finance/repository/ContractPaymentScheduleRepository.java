package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.ContractPaymentSchedule;
import com.lawfirm.infrastructure.persistence.mapper.ContractPaymentScheduleMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 合同付款计划仓储 */
@Repository
public class ContractPaymentScheduleRepository
    extends AbstractRepository<ContractPaymentScheduleMapper, ContractPaymentSchedule> {

  /**
   * 查询合同的所有付款计划
   *
   * @param contractId 合同ID
   * @return 付款计划列表
   */
  public List<ContractPaymentSchedule> findByContractId(final Long contractId) {
    return baseMapper.selectByContractId(contractId);
  }

  /**
   * 统计合同付款计划总金额
   *
   * @param contractId 合同ID
   * @return 总金额
   */
  public BigDecimal sumAmountByContractId(final Long contractId) {
    return baseMapper.sumAmountByContractId(contractId);
  }

  /**
   * 统计合同已收金额（状态为PAID的付款计划）
   *
   * @param contractId 合同ID
   * @return 已收金额
   */
  public BigDecimal sumPaidAmountByContractId(final Long contractId) {
    return baseMapper.sumPaidAmountByContractId(contractId);
  }

  /**
   * 删除合同的所有付款计划
   *
   * @param contractId 合同ID
   */
  public void deleteByContractId(final Long contractId) {
    baseMapper.deleteByContractId(contractId);
  }
}
