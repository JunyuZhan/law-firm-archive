package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.infrastructure.persistence.mapper.FeeMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 收费记录仓储 */
@Repository
public class FeeRepository extends AbstractRepository<FeeMapper, Fee> {

  /**
   * 查询合同的所有收费记录
   *
   * @param contractId 合同ID
   * @return 收费记录列表
   */
  public List<Fee> findByContractId(final Long contractId) {
    return baseMapper.selectByContractId(contractId);
  }

  /**
   * 查询案件的所有收费记录
   *
   * @param matterId 案件ID
   * @return 收费记录列表
   */
  public List<Fee> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 统计合同已收金额
   *
   * @param contractId 合同ID
   * @return 已收金额
   */
  public BigDecimal sumPaidAmountByContractId(final Long contractId) {
    return baseMapper.sumPaidAmountByContractId(contractId);
  }

  /**
   * 查询合同最近一笔收款
   *
   * @param contractId 合同ID
   * @return 最近一笔收费记录
   */
  public Fee findLastByContractId(final Long contractId) {
    return baseMapper.selectLastByContractId(contractId);
  }
}
