package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.infrastructure.persistence.mapper.PaymentMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收款记录仓储
 */
@Repository
public class PaymentRepository extends AbstractRepository<PaymentMapper, Payment> {

    /**
     * 查询收费的所有收款记录
     */
    public List<Payment> findByFeeId(Long feeId) {
        return baseMapper.selectByFeeId(feeId);
    }

    /**
     * 统计收费已确认金额
     */
    public BigDecimal sumConfirmedAmountByFeeId(Long feeId) {
        return baseMapper.sumConfirmedAmountByFeeId(feeId);
    }

    /**
     * 根据合同ID和状态查询收款记录
     */
    public List<Payment> findByContractIdAndStatus(Long contractId, String status) {
        return baseMapper.selectByContractIdAndStatus(contractId, status);
    }

    /**
     * 根据合同ID查询所有收款记录
     */
    public List<Payment> findByContractId(Long contractId) {
        return baseMapper.selectByContractId(contractId);
    }
}

