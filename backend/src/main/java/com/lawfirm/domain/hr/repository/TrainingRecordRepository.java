package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.TrainingRecord;
import com.lawfirm.infrastructure.persistence.mapper.TrainingRecordMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 培训记录仓储. */
@Repository
public class TrainingRecordRepository
    extends AbstractRepository<TrainingRecordMapper, TrainingRecord> {

  /**
   * 查询员工的培训记录.
   *
   * @param employeeId 员工ID
   * @return 培训记录列表
   */
  public List<TrainingRecord> findByEmployeeId(final Long employeeId) {
    return baseMapper.selectByEmployeeId(employeeId);
  }

  /**
   * 查询培训的参与记录.
   *
   * @param trainingId 培训ID
   * @return 培训记录列表
   */
  public List<TrainingRecord> findByTrainingId(final Long trainingId) {
    return baseMapper.selectByTrainingId(trainingId);
  }

  /**
   * 检查员工是否已报名.
   *
   * @param trainingId 培训ID
   * @param employeeId 员工ID
   * @return 是否已报名
   */
  public boolean hasEnrolled(final Long trainingId, final Long employeeId) {
    return baseMapper.countByTrainingAndEmployee(trainingId, employeeId) > 0;
  }

  /**
   * 统计员工获得的总学分.
   *
   * @param employeeId 员工ID
   * @return 总学分
   */
  public int sumCreditsByEmployeeId(final Long employeeId) {
    return baseMapper.sumCreditsByEmployeeId(employeeId);
  }

  /**
   * 统计培训的完成人数.
   *
   * @param trainingId 培训ID
   * @return 完成人数
   */
  public int countCompletedByTrainingId(final Long trainingId) {
    return baseMapper.countCompletedByTrainingId(trainingId);
  }

  /**
   * 根据培训ID和员工ID查找记录.
   *
   * @param trainingId 培训ID
   * @param employeeId 员工ID
   * @return 培训记录
   */
  public TrainingRecord findByTrainingIdAndEmployeeId(
      final Long trainingId, final Long employeeId) {
    return baseMapper.selectByTrainingIdAndEmployeeId(trainingId, employeeId);
  }
}
