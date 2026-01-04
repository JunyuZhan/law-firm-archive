package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Fee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收费记录 Mapper
 */
@Mapper
public interface FeeMapper extends BaseMapper<Fee> {

    /**
     * 查询合同的所有收费记录
     */
    @Select("SELECT * FROM finance_fee WHERE contract_id = #{contractId} AND deleted = false ORDER BY planned_date")
    List<Fee> selectByContractId(@Param("contractId") Long contractId);

    /**
     * 查询案件的所有收费记录
     */
    @Select("SELECT * FROM finance_fee WHERE matter_id = #{matterId} AND deleted = false ORDER BY planned_date")
    List<Fee> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 统计合同已收金额
     */
    @Select("SELECT COALESCE(SUM(paid_amount), 0) FROM finance_fee WHERE contract_id = #{contractId} AND deleted = false")
    BigDecimal sumPaidAmountByContractId(@Param("contractId") Long contractId);

    /**
     * 查询所有待收款记录（用于智能匹配）
     * 状态为PENDING或PARTIAL的记录
     */
    @Select("SELECT * FROM finance_fee WHERE status IN ('PENDING', 'PARTIAL') AND deleted = false ORDER BY planned_date ASC NULLS LAST")
    List<Fee> selectPendingFees();
}

