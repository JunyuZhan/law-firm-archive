package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.PrepaymentUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预收款核销记录 Mapper
 */
@Mapper
public interface PrepaymentUsageMapper extends BaseMapper<PrepaymentUsage> {

    /**
     * 查询预收款的核销记录
     */
    @Select("SELECT * FROM finance_prepayment_usage WHERE prepayment_id = #{prepaymentId} AND deleted = false ORDER BY usage_time DESC")
    List<PrepaymentUsage> selectByPrepaymentId(@Param("prepaymentId") Long prepaymentId);

    /**
     * 查询收费记录的核销来源
     */
    @Select("SELECT * FROM finance_prepayment_usage WHERE fee_id = #{feeId} AND deleted = false ORDER BY usage_time DESC")
    List<PrepaymentUsage> selectByFeeId(@Param("feeId") Long feeId);

    /**
     * 统计预收款已核销金额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM finance_prepayment_usage WHERE prepayment_id = #{prepaymentId} AND deleted = false")
    BigDecimal sumUsedAmountByPrepaymentId(@Param("prepaymentId") Long prepaymentId);
}
