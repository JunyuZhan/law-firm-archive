package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.MatterClient;
import com.lawfirm.infrastructure.persistence.mapper.MatterClientMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目-客户关联 Repository
 */
@Repository
public class MatterClientRepository extends AbstractRepository<MatterClientMapper, MatterClient> {

    /**
     * 查询项目的所有客户关联
     */
    public List<MatterClient> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }

    /**
     * 查询项目的主要客户
     */
    public Optional<MatterClient> findPrimaryClient(Long matterId) {
        MatterClient primary = baseMapper.selectPrimaryClient(matterId);
        return Optional.ofNullable(primary);
    }

    /**
     * 检查客户是否已关联到项目
     */
    public boolean existsByMatterIdAndClientId(Long matterId, Long clientId) {
        int count = baseMapper.countByMatterIdAndClientId(matterId, clientId);
        return count > 0;
    }

    /**
     * 删除项目的所有客户关联
     */
    public void deleteByMatterId(Long matterId) {
        baseMapper.deleteByMatterId(matterId);
    }
}

