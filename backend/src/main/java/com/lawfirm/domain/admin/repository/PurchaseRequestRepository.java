package com.lawfirm.domain.admin.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.PurchaseRequest;
import com.lawfirm.infrastructure.persistence.mapper.PurchaseRequestMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** 采购申请仓储 */
@Repository
public class PurchaseRequestRepository
    extends AbstractRepository<PurchaseRequestMapper, PurchaseRequest> {

  /**
   * 分页查询采购申请
   *
   * @param page 分页对象
   * @param keyword 关键词
   * @param purchaseType 采购类型
   * @param status 状态
   * @param applicantId 申请人ID
   * @param departmentId 部门ID
   * @return 采购申请分页结果
   */
  public IPage<PurchaseRequest> findPage(
      final Page<PurchaseRequest> page,
      final String keyword,
      final String purchaseType,
      final String status,
      final Long applicantId,
      final Long departmentId) {
    return baseMapper.findPage(page, keyword, purchaseType, status, applicantId, departmentId);
  }

  /**
   * 查询待审批的采购申请
   *
   * @return 待审批的采购申请列表
   */
  public List<PurchaseRequest> findPendingApproval() {
    return baseMapper.findPendingApproval();
  }

  /**
   * 按状态统计采购申请数量
   *
   * @return 各状态采购申请数量统计列表
   */
  public List<Map<String, Object>> countByStatus() {
    return baseMapper.countByStatus();
  }

  /**
   * 按类型统计采购金额
   *
   * @return 各类型采购金额统计列表
   */
  public List<Map<String, Object>> sumAmountByType() {
    return baseMapper.sumAmountByType();
  }

  /**
   * 统计供应商的采购记录数
   *
   * @param supplierId 供应商ID
   * @return 采购记录数
   */
  public long countBySupplierId(final Long supplierId) {
    return baseMapper.countBySupplierId(supplierId);
  }
}
