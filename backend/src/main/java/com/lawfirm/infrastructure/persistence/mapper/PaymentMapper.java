package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Payment;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 收款记录 Mapper */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

  /**
   * 查询收费的所有收款记录.
   *
   * @param feeId 收费ID
   * @return 收款记录列表
   */
  @Select(
      "SELECT * FROM finance_payment WHERE fee_id = #{feeId} AND deleted = false ORDER BY payment_date DESC")
  List<Payment> selectByFeeId(@Param("feeId") Long feeId);

  /**
   * 统计收费已收金额.
   *
   * @param feeId 收费ID
   * @return 已收金额
   */
  @Select(
      "SELECT COALESCE(SUM(amount), 0) FROM finance_payment "
          + "WHERE fee_id = #{feeId} AND status = 'CONFIRMED' AND deleted = false")
  BigDecimal sumConfirmedAmountByFeeId(@Param("feeId") Long feeId);

  /**
   * 根据合同ID和状态查询收款记录.
   *
   * @param contractId 合同ID
   * @param status 状态
   * @return 收款记录列表
   */
  @Select(
      "SELECT * FROM finance_payment WHERE contract_id = #{contractId} "
          + "AND status = #{status} AND deleted = false ORDER BY payment_date DESC")
  List<Payment> selectByContractIdAndStatus(
      @Param("contractId") Long contractId, @Param("status") String status);

  /**
   * 根据合同ID查询所有收款记录.
   *
   * @param contractId 合同ID
   * @return 收款记录列表
   */
  @Select(
      "SELECT * FROM finance_payment WHERE contract_id = #{contractId} AND deleted = false ORDER BY payment_date DESC")
  List<Payment> selectByContractId(@Param("contractId") Long contractId);

  /**
   * 统计项目已收款金额（通过合同关联）.
   *
   * @param matterId 项目ID
   * @return 已收款金额
   */
  @Select(
      "SELECT COALESCE(SUM(p.amount), 0) FROM finance_payment p "
          + "INNER JOIN finance_contract c ON p.contract_id = c.id "
          + "WHERE c.matter_id = #{matterId} AND p.status = 'CONFIRMED' AND p.deleted = false")
  BigDecimal sumReceivedByMatterId(@Param("matterId") Long matterId);
}
