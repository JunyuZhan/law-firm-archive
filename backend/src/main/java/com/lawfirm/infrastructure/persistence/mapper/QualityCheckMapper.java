package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.QualityCheck;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 质量检查Mapper（M10-031） */
@Mapper
public interface QualityCheckMapper extends BaseMapper<QualityCheck> {

  /**
   * 根据检查编号查询.
   *
   * @param checkNo 检查编号
   * @return 质量检查
   */
  @Select("SELECT * FROM quality_check WHERE check_no = #{checkNo} AND deleted = false")
  QualityCheck selectByCheckNo(@Param("checkNo") String checkNo);

  /**
   * 查询项目的所有检查.
   *
   * @param matterId 项目ID
   * @return 质量检查列表
   */
  @Select(
      "SELECT * FROM quality_check WHERE matter_id = #{matterId} AND deleted = false ORDER BY check_date DESC")
  List<QualityCheck> selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 查询进行中的检查。
   *
   * @return 进行中的检查列表
   */
  @Select(
      "SELECT * FROM quality_check WHERE status = 'IN_PROGRESS' AND deleted = false ORDER BY check_date DESC")
  List<QualityCheck> selectInProgress();
}
