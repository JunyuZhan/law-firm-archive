package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.infrastructure.persistence.mapper.FeeMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收费记录仓储
 */
@Repository
public class FeeRepository extends AbstractRepository<FeeMapper, Fee> {

    /**
     * 查询合同的所有收费记录
     */
    public List<Fee> findByContractId(Long contractId) {
        return baseMapper.selectByContractId(contractId);
    }

    /**
     * 查询案件的所有收费记录
     */
    public List<Fee> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }

    /**
     * 统计合同已收金额
     */
    public BigDecimal sumPaidAmountByContractId(Long contractId) {
        return baseMapper.sumPaidAmountByContractId(contractId);
    }
}

