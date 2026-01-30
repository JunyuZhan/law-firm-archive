package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.evidence.entity.Evidence;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 证据Mapper */
@Mapper
public interface EvidenceMapper extends BaseMapper<Evidence> {

  /**
   * 分页查询证据.
   *
   * @param page 分页对象
   * @param matterId 案件ID
   * @param name 证据名称
   * @param evidenceType 证据类型
   * @param groupName 分组名称
   * @param crossExamStatus 质证状态
   * @param matterIds 案件ID列表
   * @return 证据分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM evidence WHERE deleted = false "
          + "<if test='matterId != null'> AND matter_id = #{matterId} </if>"
          + "<if test='name != null and name != \"\"'> AND name LIKE CONCAT('%', #{name}, '%') </if>"
          + "<if test='evidenceType != null and evidenceType != \"\"'> AND evidence_type = #{evidenceType} </if>"
          + "<if test='groupName != null and groupName != \"\"'> AND group_name = #{groupName} </if>"
          + "<if test='crossExamStatus != null and crossExamStatus != \"\"'> "
          + "AND cross_exam_status = #{crossExamStatus} </if>"
          + "<if test='matterIds != null and matterIds.size() > 0'> AND matter_id IN "
          + "<foreach collection='matterIds' item='id' open='(' separator=',' close=')'>"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "ORDER BY group_name, sort_order, created_at"
          + "</script>")
  IPage<Evidence> selectEvidencePage(
      Page<Evidence> page,
      @Param("matterId") Long matterId,
      @Param("name") String name,
      @Param("evidenceType") String evidenceType,
      @Param("groupName") String groupName,
      @Param("crossExamStatus") String crossExamStatus,
      @Param("matterIds") java.util.List<Long> matterIds);

  /**
   * 按案件查询证据列表.
   *
   * @param matterId 案件ID
   * @return 证据列表
   */
  @Select(
      "SELECT * FROM evidence WHERE matter_id = #{matterId} AND deleted = false ORDER BY group_name, sort_order")
  List<Evidence> selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 获取案件下的证据分组.
   *
   * @param matterId 案件ID
   * @return 分组名称列表
   */
  @Select(
      "SELECT DISTINCT group_name FROM evidence WHERE matter_id = #{matterId} "
          + "AND deleted = false AND group_name IS NOT NULL ORDER BY group_name")
  List<String> selectGroupsByMatterId(@Param("matterId") Long matterId);

  /**
   * 获取分组内最大排序号.
   *
   * @param matterId 案件ID
   * @param groupName 分组名称
   * @return 最大排序号
   */
  @Select(
      "SELECT COALESCE(MAX(sort_order), 0) FROM evidence "
          + "WHERE matter_id = #{matterId} AND group_name = #{groupName} "
          + "AND deleted = false")
  Integer selectMaxSortOrder(
      @Param("matterId") Long matterId, @Param("groupName") String groupName);
}
