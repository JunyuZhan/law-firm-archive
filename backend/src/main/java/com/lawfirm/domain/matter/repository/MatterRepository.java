package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 案件仓储。
 *
 * <p>提供案件数据的持久化操作。
 */
@Repository
public class MatterRepository extends AbstractRepository<MatterMapper, Matter> {

  /**
   * 根据案件编号查询。
   *
   * @param matterNo 案件编号
   * @return 案件信息
   */
  public Optional<Matter> findByMatterNo(final String matterNo) {
    return Optional.ofNullable(baseMapper.selectByMatterNo(matterNo));
  }

  /**
   * 检查案件编号是否存在。
   *
   * @param matterNo 案件编号
   * @return 是否存在
   */
  public boolean existsByMatterNo(final String matterNo) {
    return findByMatterNo(matterNo).isPresent();
  }
}
