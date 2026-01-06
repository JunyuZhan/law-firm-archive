package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.PaymentAmendment;
import com.lawfirm.infrastructure.persistence.mapper.PaymentAmendmentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 收款变更申请仓储
 * 
 * Requirements: 3.5
 */
@Repository
public class PaymentAmendmentRepository extends AbstractRepository<PaymentAmendmentMapper, PaymentAmendment> {

    /**
     * 查询收款记录的所有变更申请
     */
    public List<PaymentAmendment> findByPaymentId(Long paymentId) {
        return baseMapper.selectByPaymentId(paymentId);
    }

    /**
     * 查询待审批的变更申请
     */
    public List<PaymentAmendment> findPendingAmendments() {
        return baseMapper.selectPendingAmendments();
    }

    /**
     * 查询用户提交的变更申请
     */
    public List<PaymentAmendment> findByRequestedBy(Long userId) {
        return baseMapper.selectByRequestedBy(userId);
    }
}
