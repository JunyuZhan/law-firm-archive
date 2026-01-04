package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.ContractPaymentSchedule;
import com.lawfirm.infrastructure.persistence.mapper.ContractPaymentScheduleMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同付款计划仓储
 */
@Repository
public class ContractPaymentScheduleRepository extends AbstractRepository<ContractPaymentScheduleMapper, ContractPaymentSchedule> {

    /**
     * 查询合同的所有付款计划
     */
    public List<ContractPaymentSchedule> findByContractId(Long contractId) {
        return baseMapper.selectByContractId(contractId);
    }

    /**
     * 统计合同付款计划总金额
     */
    public BigDecimal sumAmountByContractId(Long contractId) {
        return baseMapper.sumAmountByContractId(contractId);
    }

    /**
     * 统计合同已收金额（状态为PAID的付款计划）
     */
    public BigDecimal sumPaidAmountByContractId(Long contractId) {
        return baseMapper.sumPaidAmountByContractId(contractId);
    }

    /**
     * 删除合同的所有付款计划
     */
    public void deleteByContractId(Long contractId) {
        baseMapper.deleteByContractId(contractId);
    }
}
