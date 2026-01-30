package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.PrepaymentUsage;
import com.lawfirm.infrastructure.persistence.mapper.PrepaymentUsageMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 预收款核销记录仓储 */
@Repository
public class PrepaymentUsageRepository
    extends AbstractRepository<PrepaymentUsageMapper, PrepaymentUsage> {

  /**
   * 查询预收款的核销记录。
   *
   * @param prepaymentId 预收款ID
   * @return 核销记录列表
   */
  public List<PrepaymentUsage> findByPrepaymentId(final Long prepaymentId) {
    return baseMapper.selectByPrepaymentId(prepaymentId);
  }

  /**
   * 查询收费记录的核销来源。
   *
   * @param feeId 收费记录ID
   * @return 核销记录列表
   */
  public List<PrepaymentUsage> findByFeeId(final Long feeId) {
    return baseMapper.selectByFeeId(feeId);
  }
}
