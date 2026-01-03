package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ConflictCheck;
import com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 利益冲突检查仓储
 */
@Repository
public class ConflictCheckRepository extends AbstractRepository<ConflictCheckMapper, ConflictCheck> {

    /**
     * 根据检查编号查询
     */
    public Optional<ConflictCheck> findByCheckNo(String checkNo) {
        return Optional.ofNullable(baseMapper.selectByCheckNo(checkNo));
    }
}
