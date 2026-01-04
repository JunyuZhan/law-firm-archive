package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.PrepaymentUsage;
import com.lawfirm.infrastructure.persistence.mapper.PrepaymentUsageMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 预收款核销记录仓储
 */
@Repository
public class PrepaymentUsageRepository extends AbstractRepository<PrepaymentUsageMapper, PrepaymentUsage> {

    /**
     * 查询预收款的核销记录
     */
    public List<PrepaymentUsage> findByPrepaymentId(Long prepaymentId) {
        return baseMapper.selectByPrepaymentId(prepaymentId);
    }

    /**
     * 查询收费记录的核销来源
     */
    public List<PrepaymentUsage> findByFeeId(Long feeId) {
        return baseMapper.selectByFeeId(feeId);
    }
}
