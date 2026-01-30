package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.infrastructure.persistence.mapper.CommissionMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 提成记录 Repository */
@Repository
public class CommissionRepository extends AbstractRepository<CommissionMapper, Commission> {

  /**
   * 根据收款ID查询提成记录
   *
   * @param paymentId 收款ID
   * @return 提成记录列表
   */
  public List<Commission> findByPaymentId(final Long paymentId) {
    return baseMapper.selectByPaymentId(paymentId);
  }

  /**
   * 根据案件ID查询提成记录
   *
   * @param matterId 案件ID
   * @return 提成记录列表
   */
  public List<Commission> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 根据用户ID查询提成记录
   *
   * @param userId 用户ID
   * @param offset 偏移量
   * @param limit 限制数量
   * @return 提成记录列表
   */
  public List<Commission> findByUserId(final Long userId, final int offset, final int limit) {
    return baseMapper.selectByUserId(userId, offset, limit);
  }

  /**
   * 根据用户ID查询所有提成记录（不分页）
   *
   * @param userId 用户ID
   * @return 提成记录列表
   */
  public List<Commission> findByUserId(final Long userId) {
    return baseMapper.selectAllByUserId(userId);
  }

  /**
   * 根据用户ID和状态查询提成记录
   *
   * @param userId 用户ID
   * @param status 状态
   * @return 提成记录列表
   */
  public List<Commission> findByUserIdAndStatus(final Long userId, final String status) {
    return baseMapper.selectByUserIdAndStatus(userId, status);
  }

  /**
   * 统计用户提成总额
   *
   * @param userId 用户ID
   * @return 提成总额
   */
  public BigDecimal sumCommissionByUserId(final Long userId) {
    BigDecimal sum = baseMapper.sumCommissionByUserId(userId);
    return sum != null ? sum : BigDecimal.ZERO;
  }

  /**
   * 删除收款相关的提成记录（软删除）
   *
   * @param paymentId 收款ID
   */
  public void deleteByPaymentId(final Long paymentId) {
    List<Commission> commissions = findByPaymentId(paymentId);
    for (Commission commission : commissions) {
      softDelete(commission.getId());
    }
  }

  /**
   * 统计总提成金额
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 总提成金额
   */
  public BigDecimal sumTotalCommission(final String startDate, final String endDate) {
    BigDecimal sum = baseMapper.sumTotalCommission(startDate, endDate);
    return sum != null ? sum : BigDecimal.ZERO;
  }

  /**
   * 按状态统计提成金额
   *
   * @param status 状态
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 提成金额
   */
  public BigDecimal sumCommissionByStatus(
      final String status, final String startDate, final String endDate) {
    BigDecimal sum = baseMapper.sumCommissionByStatus(status, startDate, endDate);
    return sum != null ? sum : BigDecimal.ZERO;
  }

  /**
   * 统计提成记录数
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 记录数
   */
  public Long countCommissions(final String startDate, final String endDate) {
    Long count = baseMapper.countCommissions(startDate, endDate);
    return count != null ? count : 0L;
  }

  /**
   * 按用户汇总提成
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 按用户汇总的提成列表
   */
  public List<java.util.Map<String, Object>> sumCommissionByUser(
      final String startDate, final String endDate) {
    return baseMapper.sumCommissionByUser(startDate, endDate);
  }

  /**
   * 查询提成报表数据
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param userId 用户ID
   * @return 提成报表数据列表
   */
  public List<java.util.Map<String, Object>> queryCommissionReportData(
      final String startDate, final String endDate, final Long userId) {
    return baseMapper.queryCommissionReportData(startDate, endDate, userId);
  }

  /**
   * ✅ 修复问题553: 批量查询多个用户的提成总额（避免N+1查询）
   *
   * @param userIds 用户ID列表
   * @return Map<用户ID, 提成总额>
   */
  public java.util.Map<Long, BigDecimal> sumCommissionByUserIds(final List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return java.util.Collections.emptyMap();
    }

    List<java.util.Map<String, Object>> results = baseMapper.sumCommissionGroupByUserId(userIds);

    java.util.Map<Long, BigDecimal> commissionMap = new java.util.HashMap<>();
    for (java.util.Map<String, Object> item : results) {
      if (item == null) {
        continue;
      }
      Object userIdObj = item.get("user_id");
      Object commissionObj = item.get("total_commission");
      if (userIdObj != null && commissionObj != null) {
        Long userId = ((Number) userIdObj).longValue();
        BigDecimal commission =
            commissionObj instanceof BigDecimal
                ? (BigDecimal) commissionObj
                : new BigDecimal(commissionObj.toString());
        commissionMap.put(userId, commission);
      }
    }
    return commissionMap;
  }
}
