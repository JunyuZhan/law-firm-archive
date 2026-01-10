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
     * 根据用户ID查询所有提成记录（不分页）
     */
    public List<Commission> findByUserId(Long userId) {
        return baseMapper.selectAllByUserId(userId);
    }

    /**
     * 根据用户ID和状态查询提成记录
     */
    public List<Commission> findByUserIdAndStatus(Long userId, String status) {
        return baseMapper.selectByUserIdAndStatus(userId, status);
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

    /**
     * ✅ 修复问题553: 批量查询多个用户的提成总额（避免N+1查询）
     * @param userIds 用户ID列表
     * @return Map<用户ID, 提成总额>
     */
    public java.util.Map<Long, BigDecimal> sumCommissionByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        
        List<java.util.Map<String, Object>> results = baseMapper.sumCommissionGroupByUserId(userIds);
        
        java.util.Map<Long, BigDecimal> commissionMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> item : results) {
            if (item == null) continue;
            Object userIdObj = item.get("user_id");
            Object commissionObj = item.get("total_commission");
            if (userIdObj != null && commissionObj != null) {
                Long userId = ((Number) userIdObj).longValue();
                BigDecimal commission = commissionObj instanceof BigDecimal ? 
                        (BigDecimal) commissionObj : new BigDecimal(commissionObj.toString());
                commissionMap.put(userId, commission);
            }
        }
        return commissionMap;
    }
}

