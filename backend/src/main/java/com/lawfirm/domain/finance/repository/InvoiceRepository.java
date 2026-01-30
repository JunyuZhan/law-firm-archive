package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Invoice;
import com.lawfirm.infrastructure.persistence.mapper.InvoiceMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 发票仓储 */
@Repository
public class InvoiceRepository extends AbstractRepository<InvoiceMapper, Invoice> {

  /**
   * 查询收费的所有发票。
   *
   * @param feeId 收费记录ID
   * @return 发票列表
   */
  public List<Invoice> findByFeeId(final Long feeId) {
    return baseMapper.selectByFeeId(feeId);
  }

  /**
   * 查询合同的所有发票。
   *
   * @param contractId 合同ID
   * @return 发票列表
   */
  public List<Invoice> findByContractId(final Long contractId) {
    return baseMapper.selectByContractId(contractId);
  }
}
