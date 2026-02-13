package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.PurchaseReceive;
import com.lawfirm.infrastructure.persistence.mapper.PurchaseReceiveMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 采购入库仓储 */
@Repository
public class PurchaseReceiveRepository
    extends AbstractRepository<PurchaseReceiveMapper, PurchaseReceive> {

  /**
   * 根据采购申请ID查询入库记录
   *
   * @param requestId 采购申请ID
   * @return 入库记录列表
   */
  public List<PurchaseReceive> findByRequestId(final Long requestId) {
    return baseMapper.findByRequestId(requestId);
  }

  /**
   * 根据采购明细ID查询入库记录
   *
   * @param itemId 采购明细ID
   * @return 入库记录列表
   */
  public List<PurchaseReceive> findByItemId(final Long itemId) {
    return baseMapper.findByItemId(itemId);
  }

  /**
   * 统计采购明细的已入库数量
   *
   * @param itemId 采购明细ID
   * @return 已入库数量
   */
  public Integer sumQuantityByItemId(final Long itemId) {
    return baseMapper.sumQuantityByItemId(itemId);
  }
}
