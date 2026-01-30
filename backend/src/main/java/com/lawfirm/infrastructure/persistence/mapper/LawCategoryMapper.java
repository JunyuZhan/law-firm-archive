package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.LawCategory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 法规分类Mapper */
@Mapper
public interface LawCategoryMapper extends BaseMapper<LawCategory> {

  /**
   * 查询所有分类（树形）.
   *
   * @return 所有法规分类列表
   */
  @Select("SELECT * FROM law_category WHERE deleted = false ORDER BY sort_order, id")
  List<LawCategory> selectAllCategories();

  /**
   * 查询子分类.
   *
   * @param parentId 父分类ID
   * @return 子分类列表
   */
  @Select(
      "SELECT * FROM law_category WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
  List<LawCategory> selectByParentId(@Param("parentId") Long parentId);
}
