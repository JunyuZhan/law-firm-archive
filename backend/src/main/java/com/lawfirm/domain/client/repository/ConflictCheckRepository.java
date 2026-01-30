package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ConflictCheck;
import com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 利益冲突检查仓储。 */
@Repository
public class ConflictCheckRepository
    extends AbstractRepository<ConflictCheckMapper, ConflictCheck> {

  /**
   * 根据检查编号查询。
   *
   * @param checkNo 检查编号
   * @return 利益冲突检查
   */
  public Optional<ConflictCheck> findByCheckNo(final String checkNo) {
    return Optional.ofNullable(baseMapper.selectByCheckNo(checkNo));
  }
}
