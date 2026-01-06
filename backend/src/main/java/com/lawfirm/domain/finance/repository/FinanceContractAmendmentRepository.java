package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.FinanceContractAmendment;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractAmendmentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 财务合同变更记录仓储
 */
@Repository
public class FinanceContractAmendmentRepository extends AbstractRepository<FinanceContractAmendmentMapper, FinanceContractAmendment> {

    /**
     * 根据合同ID查询变更记录
     */
    public List<FinanceContractAmendment> findByContractId(Long contractId) {
        return baseMapper.selectByContractId(contractId);
    }

    /**
     * 查询待处理的变更记录
     */
    public List<FinanceContractAmendment> findPendingAmendments() {
        return baseMapper.selectPendingAmendments();
    }

    /**
     * 统计合同的待处理变更数量
     */
    public int countPendingByContractId(Long contractId) {
        return baseMapper.countPendingByContractId(contractId);
    }
}
