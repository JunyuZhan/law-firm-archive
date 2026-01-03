package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.Training;
import com.lawfirm.infrastructure.persistence.mapper.TrainingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 培训计划仓储
 */
@Repository
public class TrainingRepository extends AbstractRepository<TrainingMapper, Training> {

    /**
     * 查询可报名的培训
     */
    public List<Training> findAvailableTrainings() {
        return baseMapper.selectAvailableTrainings();
    }

    /**
     * 增加报名人数
     */
    public void incrementParticipants(Long id) {
        baseMapper.incrementParticipants(id);
    }

    /**
     * 减少报名人数
     */
    public void decrementParticipants(Long id) {
        baseMapper.decrementParticipants(id);
    }
}
