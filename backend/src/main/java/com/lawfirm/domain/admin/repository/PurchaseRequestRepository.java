package com.lawfirm.domain.admin.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.PurchaseRequest;
import com.lawfirm.infrastructure.persistence.mapper.PurchaseRequestMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 采购申请仓储
 */
@Repository
public class PurchaseRequestRepository extends AbstractRepository<PurchaseRequestMapper, PurchaseRequest> {

    public IPage<PurchaseRequest> findPage(Page<PurchaseRequest> page, String keyword, String purchaseType,
                                           String status, Long applicantId, Long departmentId) {
        return baseMapper.findPage(page, keyword, purchaseType, status, applicantId, departmentId);
    }

    public List<PurchaseRequest> findPendingApproval() {
        return baseMapper.findPendingApproval();
    }

    public List<Map<String, Object>> countByStatus() {
        return baseMapper.countByStatus();
    }

    public List<Map<String, Object>> sumAmountByType() {
        return baseMapper.sumAmountByType();
    }

    /**
     * 统计供应商的采购记录数
     */
    public long countBySupplierId(Long supplierId) {
        return baseMapper.countBySupplierId(supplierId);
    }
}
