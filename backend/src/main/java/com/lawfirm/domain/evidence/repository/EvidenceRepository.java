package com.lawfirm.domain.evidence.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 证据仓储 */
@Repository
public class EvidenceRepository extends AbstractRepository<EvidenceMapper, Evidence> {

  /**
   * 按案件查询证据
   *
   * @param matterId 案件ID
   * @return 证据列表
   */
  public List<Evidence> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 获取案件下的证据分组
   *
   * @param matterId 案件ID
   * @return 分组名称列表
   */
  public List<String> findGroupsByMatterId(final Long matterId) {
    return baseMapper.selectGroupsByMatterId(matterId);
  }

  /**
   * 获取分组内最大排序号
   *
   * @param matterId 案件ID
   * @param groupName 分组名称
   * @return 最大排序号
   */
  public Integer getMaxSortOrder(final Long matterId, final String groupName) {
    return baseMapper.selectMaxSortOrder(matterId, groupName);
  }
}
