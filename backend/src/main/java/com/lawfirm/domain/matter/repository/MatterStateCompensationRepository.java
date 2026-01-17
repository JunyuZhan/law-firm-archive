package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.MatterStateCompensation;
import com.lawfirm.infrastructure.persistence.mapper.MatterStateCompensationMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 国家赔偿案件业务信息 Repository
 */
@Repository
public class MatterStateCompensationRepository extends AbstractRepository<MatterStateCompensationMapper, MatterStateCompensation> {

    /**
     * 根据案件ID查询国家赔偿信息
     */
    public MatterStateCompensation findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }

    /**
     * 根据案件ID查询国家赔偿信息（返回Optional）
     */
    public Optional<MatterStateCompensation> findOptionalByMatterId(Long matterId) {
        return Optional.ofNullable(findByMatterId(matterId));
    }

    /**
     * 检查案件是否已存在国家赔偿信息
     */
    public boolean existsByMatterId(Long matterId) {
        int count = baseMapper.countByMatterId(matterId);
        return count > 0;
    }

    /**
     * 根据案件ID删除国家赔偿信息（软删除）
     */
    public boolean deleteByMatterId(Long matterId) {
        return lambdaUpdate()
                .eq(MatterStateCompensation::getMatterId, matterId)
                .remove();
    }
}
