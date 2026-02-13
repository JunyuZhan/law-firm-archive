package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.TrainingRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 培训记录 Mapper */
@Mapper
public interface TrainingRecordMapper extends BaseMapper<TrainingRecord> {

  /**
   * 查询员工的培训记录.
   *
   * @param employeeId 员工ID
   * @return 培训记录列表
   */
  @Select(
      "SELECT * FROM hr_training_record WHERE employee_id = #{employeeId} "
          + "AND deleted = false ORDER BY enroll_time DESC")
  List<TrainingRecord> selectByEmployeeId(@Param("employeeId") Long employeeId);

  /**
   * 查询培训的参与记录.
   *
   * @param trainingId 培训ID
   * @return 培训记录列表
   */
  @Select(
      "SELECT * FROM hr_training_record WHERE training_id = #{trainingId} AND deleted = false ORDER BY enroll_time")
  List<TrainingRecord> selectByTrainingId(@Param("trainingId") Long trainingId);

  /**
   * 检查员工是否已报名.
   *
   * @param trainingId 培训ID
   * @param employeeId 员工ID
   * @return 报名数量
   */
  @Select(
      "SELECT COUNT(*) FROM hr_training_record "
          + "WHERE training_id = #{trainingId} AND employee_id = #{employeeId} "
          + "AND status != 'CANCELLED' AND deleted = false")
  int countByTrainingAndEmployee(
      @Param("trainingId") Long trainingId, @Param("employeeId") Long employeeId);

  /**
   * 统计员工获得的总学分.
   *
   * @param employeeId 员工ID
   * @return 总学分
   */
  @Select(
      "SELECT COALESCE(SUM(earned_credits), 0) FROM hr_training_record "
          + "WHERE employee_id = #{employeeId} AND passed = true "
          + "AND deleted = false")
  int sumCreditsByEmployeeId(@Param("employeeId") Long employeeId);

  /**
   * 统计培训的完成人数.
   *
   * @param trainingId 培训ID
   * @return 完成人数
   */
  @Select(
      "SELECT COUNT(*) FROM hr_training_record WHERE training_id = #{trainingId} "
          + "AND status = 'COMPLETED' AND certificate_url IS NOT NULL "
          + "AND deleted = false")
  int countCompletedByTrainingId(@Param("trainingId") Long trainingId);

  /**
   * 根据培训ID和员工ID查找记录.
   *
   * @param trainingId 培训ID
   * @param employeeId 员工ID
   * @return 培训记录
   */
  @Select(
      "SELECT * FROM hr_training_record WHERE training_id = #{trainingId} "
          + "AND employee_id = #{employeeId} AND deleted = false LIMIT 1")
  TrainingRecord selectByTrainingIdAndEmployeeId(
      @Param("trainingId") Long trainingId, @Param("employeeId") Long employeeId);
}
