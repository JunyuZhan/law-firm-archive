package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.document.entity.MatterDossierItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 项目卷宗目录 Mapper */
@Mapper
public interface MatterDossierItemMapper extends BaseMapper<MatterDossierItem> {

  /**
   * 根据项目ID查询目录项（按排序）.
   *
   * @param matterId 项目ID
   * @return 目录项列表
   */
  @Select(
      "SELECT * FROM matter_dossier_item WHERE matter_id = #{matterId} AND deleted = false ORDER BY sort_order")
  List<MatterDossierItem> selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 根据父ID查询子目录项.
   *
   * @param matterId 项目ID
   * @param parentId 父目录ID
   * @return 子目录项列表
   */
  @Select(
      "SELECT * FROM matter_dossier_item WHERE matter_id = #{matterId} "
          + "AND parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
  List<MatterDossierItem> selectByParentId(
      @Param("matterId") Long matterId, @Param("parentId") Long parentId);

  /**
   * 更新目录项文件数量.
   *
   * @param itemId 目录项ID
   * @param count 文件数量
   * @return 更新数量
   */
  @Update(
      "UPDATE matter_dossier_item SET document_count = #{count}, updated_at = NOW() WHERE id = #{itemId}")
  int updateDocumentCount(@Param("itemId") Long itemId, @Param("count") Integer count);

  /**
   * 检查项目是否已初始化卷宗目录.
   *
   * @param matterId 项目ID
   * @return 目录项数量
   */
  @Select(
      "SELECT COUNT(*) FROM matter_dossier_item WHERE matter_id = #{matterId} AND deleted = false")
  int countByMatterId(@Param("matterId") Long matterId);
}
