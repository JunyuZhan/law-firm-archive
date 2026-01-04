package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Prepayment;
import com.lawfirm.infrastructure.persistence.mapper.PrepaymentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 预收款仓储
 */
@Repository
public class PrepaymentRepository extends AbstractRepository<PrepaymentMapper, Prepayment> {

    /**
     * 查询客户的有效预收款
     */
    public List<Prepayment> findActiveByClientId(Long clientId) {
        return baseMapper.selectActiveByClientId(clientId);
    }

    /**
     * 查询项目关联的预收款
     */
    public List<Prepayment> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }

    /**
     * 查询合同关联的预收款
     */
    public List<Prepayment> findByContractId(Long contractId) {
        return baseMapper.selectByContractId(contractId);
    }
}
