package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.system.entity.DataHandoverDetail;
import com.lawfirm.infrastructure.persistence.mapper.DataHandoverDetailMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 数据交接明细仓储。
 *
 * <p>提供数据交接明细的持久化操作。
 */
@Repository
public class DataHandoverDetailRepository
    extends ServiceImpl<DataHandoverDetailMapper, DataHandoverDetail> {

  /**
   * 根据交接单ID查询明细。
   *
   * @param handoverId 交接单ID
   * @return 交接明细列表
   */
  public List<DataHandoverDetail> findByHandoverId(final Long handoverId) {
    return baseMapper.selectByHandoverId(handoverId);
  }

  /**
   * 根据交接单ID和数据类型查询明细。
   *
   * @param handoverId 交接单ID
   * @param dataType 数据类型
   * @return 交接明细列表
   */
  public List<DataHandoverDetail> findByHandoverIdAndType(
      final Long handoverId, final String dataType) {
    return baseMapper.selectByHandoverIdAndType(handoverId, dataType);
  }

  /**
   * 统计交接单明细数量。
   *
   * @param handoverId 交接单ID
   * @return 明细数量
   */
  public int countByHandoverId(final Long handoverId) {
    return baseMapper.countByHandoverId(handoverId);
  }

  /**
   * 统计交接单指定状态的明细数量。
   *
   * @param handoverId 交接单ID
   * @param status 状态
   * @return 明细数量
   */
  public int countByHandoverIdAndStatus(final Long handoverId, final String status) {
    return baseMapper.countByHandoverIdAndStatus(handoverId, status);
  }

  /**
   * 批量更新状态。
   *
   * @param handoverId 交接单ID
   * @param status 状态
   * @return 更新数量
   */
  public int updateStatusByHandoverId(final Long handoverId, final String status) {
    return baseMapper.updateStatusByHandoverId(handoverId, status);
  }
}
