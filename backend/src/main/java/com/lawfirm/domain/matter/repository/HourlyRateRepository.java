package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.HourlyRate;
import com.lawfirm.infrastructure.persistence.mapper.HourlyRateMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 小时费率仓储
 */
@Repository
public class HourlyRateRepository extends AbstractRepository<HourlyRateMapper, HourlyRate> {

    /**
     * 获取用户当前有效费率
     */
    public HourlyRate findCurrentRate(Long userId, LocalDate date) {
        return baseMapper.selectCurrentRate(userId, date);
    }

    /**
     * 获取用户最新费率
     */
    public HourlyRate findLatestRate(Long userId) {
        return baseMapper.selectLatestRate(userId);
    }
}
