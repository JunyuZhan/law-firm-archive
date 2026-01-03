package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.infrastructure.persistence.mapper.CommissionMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 提成记录 Repository
 */
@Repository
public class CommissionRepository extends AbstractRepository<CommissionMapper, Commission> {

    /**
     * 根据收款ID查询提成记录
     */
    public List<Commission> findByPaymentId(Long paymentId) {
        return baseMapper.selectByPaymentId(paymentId);
    }

    /**
     * 根据案件ID查询提成记录
     */
    public List<Commission> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }

    /**
     * 根据用户ID查询提成记录
     */
    public List<Commission> findByUserId(Long userId, int offset, int limit) {
        return baseMapper.selectByUserId(userId, offset, limit);
    }

    /**
     * 统计用户提成总额
     */
    public BigDecimal sumCommissionByUserId(Long userId) {
        BigDecimal sum = baseMapper.sumCommissionByUserId(userId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * 删除收款相关的提成记录（软删除）
     */
    public void deleteByPaymentId(Long paymentId) {
        List<Commission> commissions = findByPaymentId(paymentId);
        for (Commission commission : commissions) {
            softDelete(commission.getId());
        }
    }

    /**
     * 统计总提成金额
     */
    public BigDecimal sumTotalCommission(String startDate, String endDate) {
        BigDecimal sum = baseMapper.sumTotalCommission(startDate, endDate);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * 按状态统计提成金额
     */
    public BigDecimal sumCommissionByStatus(String status, String startDate, String endDate) {
        BigDecimal sum = baseMapper.sumCommissionByStatus(status, startDate, endDate);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * 统计提成记录数
     */
    public Long countCommissions(String startDate, String endDate) {
        Long count = baseMapper.countCommissions(startDate, endDate);
        return count != null ? count : 0L;
    }

    /**
     * 按用户汇总提成
     */
    public List<java.util.Map<String, Object>> sumCommissionByUser(String startDate, String endDate) {
        return baseMapper.sumCommissionByUser(startDate, endDate);
    }

    /**
     * 查询提成报表数据
     */
    public List<java.util.Map<String, Object>> queryCommissionReportData(String startDate, String endDate, Long userId) {
        return baseMapper.queryCommissionReportData(startDate, endDate, userId);
    }
}

