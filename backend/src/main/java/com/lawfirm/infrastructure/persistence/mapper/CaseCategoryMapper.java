package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.CaseCategory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 案例分类Mapper */
@Mapper
public interface CaseCategoryMapper extends BaseMapper<CaseCategory> {

  /**
   * 查询所有分类.
   *
   * @return 案例分类列表
   */
  @Select("SELECT * FROM case_category WHERE deleted = false ORDER BY sort_order, id")
  List<CaseCategory> selectAllCategories();

  /**
   * 查询子分类.
   *
   * @param parentId 父分类ID
   * @return 子分类列表
   */
  @Select(
      "SELECT * FROM case_category WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
  List<CaseCategory> selectByParentId(@Param("parentId") Long parentId);
}
