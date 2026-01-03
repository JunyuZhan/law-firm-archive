package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.PurchaseReceive;
import com.lawfirm.infrastructure.persistence.mapper.PurchaseReceiveMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 采购入库仓储
 */
@Repository
public class PurchaseReceiveRepository extends AbstractRepository<PurchaseReceiveMapper, PurchaseReceive> {

    public List<PurchaseReceive> findByRequestId(Long requestId) {
        return baseMapper.findByRequestId(requestId);
    }

    public List<PurchaseReceive> findByItemId(Long itemId) {
        return baseMapper.findByItemId(itemId);
    }

    public Integer sumQuantityByItemId(Long itemId) {
        return baseMapper.sumQuantityByItemId(itemId);
    }
}
