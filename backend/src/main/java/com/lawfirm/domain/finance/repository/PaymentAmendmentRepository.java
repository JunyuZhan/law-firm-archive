package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.PaymentAmendment;
import com.lawfirm.infrastructure.persistence.mapper.PaymentAmendmentMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 收款变更申请仓储
 *
 * <p>Requirements: 3.5
 */
@Repository
public class PaymentAmendmentRepository
    extends AbstractRepository<PaymentAmendmentMapper, PaymentAmendment> {

  /**
   * 查询收款记录的所有变更申请
   *
   * @param paymentId 收款记录ID
   * @return 变更申请列表
   */
  public List<PaymentAmendment> findByPaymentId(final Long paymentId) {
    return baseMapper.selectByPaymentId(paymentId);
  }

  /**
   * 查询待审批的变更申请
   *
   * @return 待审批的变更申请列表
   */
  public List<PaymentAmendment> findPendingAmendments() {
    return baseMapper.selectPendingAmendments();
  }

  /**
   * 查询用户提交的变更申请
   *
   * @param userId 用户ID
   * @return 变更申请列表
   */
  public List<PaymentAmendment> findByRequestedBy(final Long userId) {
    return baseMapper.selectByRequestedBy(userId);
  }
}
