package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.MatterStateCompensation;
import com.lawfirm.infrastructure.persistence.mapper.MatterStateCompensationMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 国家赔偿案件业务信息仓储。
 *
 * <p>提供国家赔偿案件业务信息的持久化操作。
 */
@Repository
public class MatterStateCompensationRepository
    extends AbstractRepository<MatterStateCompensationMapper, MatterStateCompensation> {

  /**
   * 根据案件ID查询国家赔偿信息。
   *
   * @param matterId 案件ID
   * @return 国家赔偿信息
   */
  public MatterStateCompensation findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 根据案件ID查询国家赔偿信息（返回Optional）。
   *
   * @param matterId 案件ID
   * @return 国家赔偿信息
   */
  public Optional<MatterStateCompensation> findOptionalByMatterId(final Long matterId) {
    return Optional.ofNullable(findByMatterId(matterId));
  }

  /**
   * 检查案件是否已存在国家赔偿信息。
   *
   * @param matterId 案件ID
   * @return 是否存在
   */
  public boolean existsByMatterId(final Long matterId) {
    int count = baseMapper.countByMatterId(matterId);
    return count > 0;
  }

  /**
   * 根据案件ID删除国家赔偿信息（软删除）。
   *
   * @param matterId 案件ID
   * @return 是否删除成功
   */
  public boolean deleteByMatterId(final Long matterId) {
    return lambdaUpdate().eq(MatterStateCompensation::getMatterId, matterId).remove();
  }
}
