package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.document.entity.DocumentCategory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 文档分类Mapper */
@Mapper
public interface DocumentCategoryMapper extends BaseMapper<DocumentCategory> {

  /**
   * 查询子分类.
   *
   * @param parentId 父分类ID
   * @return 子分类列表
   */
  @Select(
      "SELECT * FROM doc_category WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
  List<DocumentCategory> selectByParentId(Long parentId);

  /**
   * 查询所有分类（树形结构用）.
   *
   * @return 所有文档分类列表
   */
  @Select("SELECT * FROM doc_category WHERE deleted = false ORDER BY parent_id, sort_order")
  List<DocumentCategory> selectAllCategories();
}
