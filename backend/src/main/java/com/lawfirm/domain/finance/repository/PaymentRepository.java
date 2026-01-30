package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.infrastructure.persistence.mapper.PaymentMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 收款记录仓储 */
@Repository
public class PaymentRepository extends AbstractRepository<PaymentMapper, Payment> {

  /**
   * 查询收费的所有收款记录
   *
   * @param feeId 收费ID
   * @return 收款记录列表
   */
  public List<Payment> findByFeeId(final Long feeId) {
    return baseMapper.selectByFeeId(feeId);
  }

  /**
   * 统计收费已确认金额
   *
   * @param feeId 收费ID
   * @return 已确认金额
   */
  public BigDecimal sumConfirmedAmountByFeeId(final Long feeId) {
    return baseMapper.sumConfirmedAmountByFeeId(feeId);
  }

  /**
   * 根据合同ID和状态查询收款记录
   *
   * @param contractId 合同ID
   * @param status 状态
   * @return 收款记录列表
   */
  public List<Payment> findByContractIdAndStatus(final Long contractId, final String status) {
    return baseMapper.selectByContractIdAndStatus(contractId, status);
  }

  /**
   * 根据合同ID查询所有收款记录
   *
   * @param contractId 合同ID
   * @return 收款记录列表
   */
  public List<Payment> findByContractId(final Long contractId) {
    return baseMapper.selectByContractId(contractId);
  }
}
