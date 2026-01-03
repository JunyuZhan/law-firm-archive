package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.infrastructure.persistence.mapper.MatterParticipantMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 案件参与人 Repository
 */
@Repository
public class MatterParticipantRepository extends AbstractRepository<MatterParticipantMapper, MatterParticipant> {

    /**
     * 查询案件的所有参与人
     */
    public List<MatterParticipant> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }

    /**
     * 查询案件的主办律师
     */
    public Optional<MatterParticipant> findLeadLawyer(Long matterId) {
        MatterParticipant lead = baseMapper.selectLeadLawyer(matterId);
        return Optional.ofNullable(lead);
    }

    /**
     * 检查用户是否已在案件团队中
     */
    public boolean existsByMatterIdAndUserId(Long matterId, Long userId) {
        int count = baseMapper.countByMatterIdAndUserId(matterId, userId);
        return count > 0;
    }

    /**
     * 删除案件的所有参与人
     */
    public void deleteByMatterId(Long matterId) {
        baseMapper.deleteByMatterId(matterId);
    }
}

