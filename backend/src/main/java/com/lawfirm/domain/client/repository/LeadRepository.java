package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.Lead;
import com.lawfirm.infrastructure.persistence.mapper.LeadMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 案源线索 Repository
 */
@Repository
public class LeadRepository extends AbstractRepository<LeadMapper, Lead> {

    /**
     * 根据案源编号查询
     */
    public Optional<Lead> findByLeadNo(String leadNo) {
        Lead lead = baseMapper.selectByLeadNo(leadNo);
        return Optional.ofNullable(lead);
    }

    /**
     * 检查案源编号是否存在
     */
    public boolean existsByLeadNo(String leadNo) {
        return findByLeadNo(leadNo).isPresent();
    }
}

