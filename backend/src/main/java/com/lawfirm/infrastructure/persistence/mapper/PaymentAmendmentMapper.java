package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.PaymentAmendment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收款变更申请 Mapper
 * 
 * Requirements: 3.5
 */
@Mapper
public interface PaymentAmendmentMapper extends BaseMapper<PaymentAmendment> {

    /**
     * 查询收款记录的所有变更申请
     */
    @Select("SELECT * FROM fin_payment_amendment WHERE payment_id = #{paymentId} AND deleted = false ORDER BY requested_at DESC")
    List<PaymentAmendment> selectByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * 查询待审批的变更申请
     */
    @Select("SELECT * FROM fin_payment_amendment WHERE status = 'PENDING' AND deleted = false ORDER BY requested_at ASC")
    List<PaymentAmendment> selectPendingAmendments();

    /**
     * 查询用户提交的变更申请
     */
    @Select("SELECT * FROM fin_payment_amendment WHERE requested_by = #{userId} AND deleted = false ORDER BY requested_at DESC")
    List<PaymentAmendment> selectByRequestedBy(@Param("userId") Long userId);
}
