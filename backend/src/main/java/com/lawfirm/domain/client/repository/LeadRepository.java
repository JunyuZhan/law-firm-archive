package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.Lead;
import com.lawfirm.infrastructure.persistence.mapper.LeadMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 案源线索 Repository。 */
@Repository
public class LeadRepository extends AbstractRepository<LeadMapper, Lead> {

  /**
   * 根据案源编号查询。
   *
   * @param leadNo 案源编号
   * @return 案源线索
   */
  public Optional<Lead> findByLeadNo(final String leadNo) {
    Lead lead = baseMapper.selectByLeadNo(leadNo);
    return Optional.ofNullable(lead);
  }

  /**
   * 检查案源编号是否存在。
   *
   * @param leadNo 案源编号
   * @return 是否存在
   */
  public boolean existsByLeadNo(final String leadNo) {
    return findByLeadNo(leadNo).isPresent();
  }
}
