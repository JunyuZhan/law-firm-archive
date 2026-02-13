package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.evidence.entity.EvidenceList;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 证据清单Mapper */
@Mapper
public interface EvidenceListMapper extends BaseMapper<EvidenceList> {

  /**
   * 分页查询证据清单列表.
   *
   * @param page 分页参数
   * @param matterId 案件ID
   * @param listType 清单类型
   * @return 证据清单分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM evidence_list WHERE deleted = false "
          + "<if test='matterId != null'> AND matter_id = #{matterId} </if>"
          + "<if test='listType != null'> AND list_type = #{listType} </if>"
          + "ORDER BY created_at DESC"
          + "</script>")
  IPage<EvidenceList> selectListPage(
      Page<EvidenceList> page,
      @Param("matterId") Long matterId,
      @Param("listType") String listType);

  /**
   * 根据案件ID查询证据清单列表.
   *
   * @param matterId 案件ID
   * @return 证据清单列表
   */
  @Select(
      "SELECT * FROM evidence_list WHERE matter_id = #{matterId} AND deleted = false ORDER BY created_at DESC")
  List<EvidenceList> selectByMatterId(@Param("matterId") Long matterId);
}
