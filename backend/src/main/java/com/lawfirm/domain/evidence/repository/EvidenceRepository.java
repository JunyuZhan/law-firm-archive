package com.lawfirm.domain.evidence.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 证据仓储
 */
@Repository
public class EvidenceRepository extends AbstractRepository<EvidenceMapper, Evidence> {

    /**
     * 按案件查询证据
     */
    public List<Evidence> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }

    /**
     * 获取案件下的证据分组
     */
    public List<String> findGroupsByMatterId(Long matterId) {
        return baseMapper.selectGroupsByMatterId(matterId);
    }

    /**
     * 获取分组内最大排序号
     */
    public Integer getMaxSortOrder(Long matterId, String groupName) {
        return baseMapper.selectMaxSortOrder(matterId, groupName);
    }
}
