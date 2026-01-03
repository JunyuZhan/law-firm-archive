package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 案件仓储
 */
@Repository
public class MatterRepository extends AbstractRepository<MatterMapper, Matter> {

    /**
     * 根据案件编号查询
     */
    public Optional<Matter> findByMatterNo(String matterNo) {
        return Optional.ofNullable(baseMapper.selectByMatterNo(matterNo));
    }

    /**
     * 检查案件编号是否存在
     */
    public boolean existsByMatterNo(String matterNo) {
        return findByMatterNo(matterNo).isPresent();
    }
}
