package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收款记录 Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 查询收费的所有收款记录
     */
    @Select("SELECT * FROM finance_payment WHERE fee_id = #{feeId} AND deleted = false ORDER BY payment_date DESC")
    List<Payment> selectByFeeId(@Param("feeId") Long feeId);

    /**
     * 统计收费已收金额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM finance_payment WHERE fee_id = #{feeId} AND status = 'CONFIRMED' AND deleted = false")
    BigDecimal sumConfirmedAmountByFeeId(@Param("feeId") Long feeId);

    /**
     * 根据合同ID和状态查询收款记录
     */
    @Select("SELECT * FROM finance_payment WHERE contract_id = #{contractId} AND status = #{status} AND deleted = false ORDER BY payment_date DESC")
    List<Payment> selectByContractIdAndStatus(@Param("contractId") Long contractId, @Param("status") String status);

    /**
     * 根据合同ID查询所有收款记录
     */
    @Select("SELECT * FROM finance_payment WHERE contract_id = #{contractId} AND deleted = false ORDER BY payment_date DESC")
    List<Payment> selectByContractId(@Param("contractId") Long contractId);
}

