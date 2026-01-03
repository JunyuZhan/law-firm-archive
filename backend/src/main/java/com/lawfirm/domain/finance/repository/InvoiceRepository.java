package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Invoice;
import com.lawfirm.infrastructure.persistence.mapper.InvoiceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 发票仓储
 */
@Repository
public class InvoiceRepository extends AbstractRepository<InvoiceMapper, Invoice> {

    /**
     * 查询收费的所有发票
     */
    public List<Invoice> findByFeeId(Long feeId) {
        return baseMapper.selectByFeeId(feeId);
    }

    /**
     * 查询合同的所有发票
     */
    public List<Invoice> findByContractId(Long contractId) {
        return baseMapper.selectByContractId(contractId);
    }
}

