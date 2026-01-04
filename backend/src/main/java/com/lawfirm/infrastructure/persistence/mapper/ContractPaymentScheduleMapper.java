package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.ContractPaymentSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同付款计划 Mapper
 */
@Mapper
public interface ContractPaymentScheduleMapper extends BaseMapper<ContractPaymentSchedule> {

    /**
     * 查询合同的所有付款计划
     */
    @Select("SELECT * FROM contract_payment_schedule WHERE contract_id = #{contractId} AND deleted = false ORDER BY planned_date")
    List<ContractPaymentSchedule> selectByContractId(@Param("contractId") Long contractId);

    /**
     * 统计合同付款计划总金额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM contract_payment_schedule WHERE contract_id = #{contractId} AND deleted = false AND status != 'CANCELLED'")
    BigDecimal sumAmountByContractId(@Param("contractId") Long contractId);

    /**
     * 统计合同已收金额（状态为PAID的付款计划）
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM contract_payment_schedule WHERE contract_id = #{contractId} AND deleted = false AND status = 'PAID'")
    BigDecimal sumPaidAmountByContractId(@Param("contractId") Long contractId);

    /**
     * 软删除合同的所有付款计划
     */
    @Update("UPDATE contract_payment_schedule SET deleted = true WHERE contract_id = #{contractId}")
    void deleteByContractId(@Param("contractId") Long contractId);
}
