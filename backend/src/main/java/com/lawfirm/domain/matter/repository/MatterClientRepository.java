package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.MatterClient;
import com.lawfirm.infrastructure.persistence.mapper.MatterClientMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 项目-客户关联仓储。
 *
 * <p>提供项目-客户关联数据的持久化操作。
 */
@Repository
public class MatterClientRepository extends AbstractRepository<MatterClientMapper, MatterClient> {

  /**
   * 查询项目的所有客户关联。
   *
   * @param matterId 案件ID
   * @return 客户关联列表
   */
  public List<MatterClient> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 查询项目的主要客户。
   *
   * @param matterId 案件ID
   * @return 主要客户
   */
  public Optional<MatterClient> findPrimaryClient(final Long matterId) {
    MatterClient primary = baseMapper.selectPrimaryClient(matterId);
    return Optional.ofNullable(primary);
  }

  /**
   * 检查客户是否已关联到项目。
   *
   * @param matterId 案件ID
   * @param clientId 客户ID
   * @return 是否已关联
   */
  public boolean existsByMatterIdAndClientId(final Long matterId, final Long clientId) {
    int count = baseMapper.countByMatterIdAndClientId(matterId, clientId);
    return count > 0;
  }

  /**
   * 删除项目的所有客户关联。
   *
   * @param matterId 案件ID
   */
  public void deleteByMatterId(final Long matterId) {
    baseMapper.deleteByMatterId(matterId);
  }
}
