package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.LeadFollowUp;
import com.lawfirm.infrastructure.persistence.mapper.LeadFollowUpMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案源跟进记录 Repository
 */
@Repository
public class LeadFollowUpRepository extends AbstractRepository<LeadFollowUpMapper, LeadFollowUp> {

    /**
     * 查询案源的所有跟进记录
     */
    public List<LeadFollowUp> findByLeadId(Long leadId) {
        return baseMapper.selectByLeadId(leadId);
    }
}

