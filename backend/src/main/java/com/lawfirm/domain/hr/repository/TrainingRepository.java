package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.Training;
import com.lawfirm.infrastructure.persistence.mapper.TrainingMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 培训计划仓储. */
@Repository
public class TrainingRepository extends AbstractRepository<TrainingMapper, Training> {

  /**
   * 查询可报名的培训.
   *
   * @return 可报名的培训列表
   */
  public List<Training> findAvailableTrainings() {
    return baseMapper.selectAvailableTrainings();
  }

  /**
   * 增加报名人数.
   *
   * @param id 培训ID
   */
  public void incrementParticipants(final Long id) {
    baseMapper.incrementParticipants(id);
  }

  /**
   * 减少报名人数.
   *
   * @param id 培训ID
   */
  public void decrementParticipants(final Long id) {
    baseMapper.decrementParticipants(id);
  }
}
