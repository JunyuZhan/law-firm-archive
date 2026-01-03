package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.CareerLevel;
import com.lawfirm.infrastructure.persistence.mapper.CareerLevelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 职级通道 Repository
 */
@Repository
public class CareerLevelRepository extends AbstractRepository<CareerLevelMapper, CareerLevel> {

    /**
     * 按类别查询职级列表
     */
    public List<CareerLevel> findByCategory(String category) {
        return baseMapper.selectByCategory(category);
    }

    /**
     * 查询下一级职级
     */
    public Optional<CareerLevel> findNextLevel(String category, Integer currentOrder) {
        return Optional.ofNullable(baseMapper.selectNextLevel(category, currentOrder));
    }

    /**
     * 根据编码查询
     */
    public Optional<CareerLevel> findByLevelCode(String levelCode) {
        return Optional.ofNullable(baseMapper.selectByLevelCode(levelCode));
    }
}
