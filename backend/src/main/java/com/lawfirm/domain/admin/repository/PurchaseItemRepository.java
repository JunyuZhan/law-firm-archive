package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.PurchaseItem;
import com.lawfirm.infrastructure.persistence.mapper.PurchaseItemMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 采购明细仓储 */
@Repository
public class PurchaseItemRepository extends AbstractRepository<PurchaseItemMapper, PurchaseItem> {

  /**
   * 根据采购申请ID查询明细。
   *
   * @param requestId 采购申请ID
   * @return 采购明细列表
   */
  public List<PurchaseItem> findByRequestId(final Long requestId) {
    return baseMapper.findByRequestId(requestId);
  }

  /**
   * 删除采购申请的所有明细。
   *
   * @param requestId 采购申请ID
   */
  public void deleteByRequestId(final Long requestId) {
    baseMapper.deleteByRequestId(requestId);
  }
}
