package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.QualityIssue;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 问题整改Mapper（M10-032） */
@Mapper
public interface QualityIssueMapper extends BaseMapper<QualityIssue> {

  /**
   * 根据问题编号查询.
   *
   * @param issueNo 问题编号
   * @return 问题整改记录
   */
  @Select("SELECT * FROM quality_issue WHERE issue_no = #{issueNo} AND deleted = false")
  QualityIssue selectByIssueNo(@Param("issueNo") String issueNo);

  /**
   * 查询项目的所有问题.
   *
   * @param matterId 项目ID
   * @return 问题整改记录列表
   */
  @Select(
      "SELECT * FROM quality_issue WHERE matter_id = #{matterId} AND deleted = false ORDER BY created_at DESC")
  List<QualityIssue> selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 查询待整改的问题。
   *
   * @return 待整改的问题列表
   */
  @Select(
      "SELECT * FROM quality_issue WHERE status IN ('OPEN', 'IN_PROGRESS') "
          + "AND deleted = false ORDER BY priority DESC, due_date ASC")
  List<QualityIssue> selectPendingIssues();
}
