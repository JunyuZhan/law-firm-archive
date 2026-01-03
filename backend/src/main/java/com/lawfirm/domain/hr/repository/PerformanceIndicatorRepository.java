package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PerformanceIndicator;
import com.lawfirm.infrastructure.persistence.mapper.PerformanceIndicatorMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 考核指标仓储
 */
@Repository
public class PerformanceIndicatorRepository extends AbstractRepository<PerformanceIndicatorMapper, PerformanceIndicator> {

    public List<PerformanceIndicator> findAllActive() {
        return baseMapper.findAllActive();
    }

    public List<PerformanceIndicator> findByRole(String role) {
        return baseMapper.findByRole(role);
    }

    public List<PerformanceIndicator> findByCategory(String category) {
        return baseMapper.findByCategory(category);
    }
}
