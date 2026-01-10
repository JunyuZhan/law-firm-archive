package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.TrainingRecord;
import com.lawfirm.infrastructure.persistence.mapper.TrainingRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 培训记录仓储
 */
@Repository
public class TrainingRecordRepository extends AbstractRepository<TrainingRecordMapper, TrainingRecord> {

    /**
     * 查询员工的培训记录
     */
    public List<TrainingRecord> findByEmployeeId(Long employeeId) {
        return baseMapper.selectByEmployeeId(employeeId);
    }

    /**
     * 查询培训的参与记录
     */
    public List<TrainingRecord> findByTrainingId(Long trainingId) {
        return baseMapper.selectByTrainingId(trainingId);
    }

    /**
     * 检查员工是否已报名
     */
    public boolean hasEnrolled(Long trainingId, Long employeeId) {
        return baseMapper.countByTrainingAndEmployee(trainingId, employeeId) > 0;
    }

    /**
     * 统计员工获得的总学分
     */
    public int sumCreditsByEmployeeId(Long employeeId) {
        return baseMapper.sumCreditsByEmployeeId(employeeId);
    }

    /**
     * 统计培训的完成人数
     */
    public int countCompletedByTrainingId(Long trainingId) {
        return baseMapper.countCompletedByTrainingId(trainingId);
    }

    /**
     * 根据培训ID和员工ID查找记录
     */
    public TrainingRecord findByTrainingIdAndEmployeeId(Long trainingId, Long employeeId) {
        return baseMapper.selectByTrainingIdAndEmployeeId(trainingId, employeeId);
    }
}
