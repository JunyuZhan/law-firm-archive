package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.LeadFollowUp;
import com.lawfirm.infrastructure.persistence.mapper.LeadFollowUpMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 案源跟进记录 Repository。 */
@Repository
public class LeadFollowUpRepository extends AbstractRepository<LeadFollowUpMapper, LeadFollowUp> {

  /**
   * 查询案源的所有跟进记录。
   *
   * @param leadId 案源ID
   * @return 跟进记录列表
   */
  public List<LeadFollowUp> findByLeadId(final Long leadId) {
    return baseMapper.selectByLeadId(leadId);
  }
}
