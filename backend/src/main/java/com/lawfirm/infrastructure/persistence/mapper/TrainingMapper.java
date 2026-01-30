package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.Training;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 培训计划 Mapper */
@Mapper
public interface TrainingMapper extends BaseMapper<Training> {

  /**
   * 查询可报名的培训.
   *
   * @return 可报名的培训列表
   */
  @Select(
      "SELECT * FROM hr_training WHERE status = 'PUBLISHED' "
          + "AND enroll_deadline >= CURRENT_DATE AND deleted = false "
          + "ORDER BY start_time")
  List<Training> selectAvailableTrainings();

  /**
   * 增加报名人数.
   *
   * @param id 培训ID
   * @return 更新数量
   */
  @Update("UPDATE hr_training SET current_participants = current_participants + 1 WHERE id = #{id}")
  int incrementParticipants(@Param("id") Long id);

  /**
   * 减少报名人数.
   *
   * @param id 培训ID
   * @return 更新数量
   */
  @Update(
      "UPDATE hr_training SET current_participants = current_participants - 1 "
          + "WHERE id = #{id} AND current_participants > 0")
  int decrementParticipants(@Param("id") Long id);
}
