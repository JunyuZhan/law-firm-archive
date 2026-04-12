package com.archivesystem.service.impl;

import com.archivesystem.entity.RetentionPeriod;
import com.archivesystem.repository.RetentionPeriodMapper;
import com.archivesystem.service.RetentionPeriodService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 保管期限服务实现.
 * @author junyuzhan
 */
@Service
@RequiredArgsConstructor
public class RetentionPeriodServiceImpl implements RetentionPeriodService {

    private final RetentionPeriodMapper retentionPeriodMapper;

    @Override
    public List<RetentionPeriod> listAll() {
        LambdaQueryWrapper<RetentionPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RetentionPeriod::getSortOrder)
                .orderByAsc(RetentionPeriod::getId);
        return retentionPeriodMapper.selectList(wrapper);
    }
}
