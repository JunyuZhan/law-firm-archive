package com.archivesystem.repository;

import com.archivesystem.entity.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案分类Mapper接口.
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 根据父ID查询子分类.
     */
    @Select("SELECT * FROM arc_category WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order, id")
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询顶级分类.
     */
    @Select("SELECT * FROM arc_category WHERE parent_id IS NULL AND deleted = false ORDER BY sort_order, id")
    List<Category> selectRootCategories();

    /**
     * 根据分类号查询.
     */
    @Select("SELECT * FROM arc_category WHERE category_code = #{categoryCode} AND deleted = false")
    Category selectByCategoryCode(@Param("categoryCode") String categoryCode);
}
