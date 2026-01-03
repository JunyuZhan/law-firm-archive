package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.PurchaseItem;
import com.lawfirm.infrastructure.persistence.mapper.PurchaseItemMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 采购明细仓储
 */
@Repository
public class PurchaseItemRepository extends AbstractRepository<PurchaseItemMapper, PurchaseItem> {

    public List<PurchaseItem> findByRequestId(Long requestId) {
        return baseMapper.findByRequestId(requestId);
    }

    public void deleteByRequestId(Long requestId) {
        baseMapper.deleteByRequestId(requestId);
    }
}
