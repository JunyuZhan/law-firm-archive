package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.document.entity.DocumentCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档分类Mapper
 */
@Mapper
public interface DocumentCategoryMapper extends BaseMapper<DocumentCategory> {

    /**
     * 查询子分类
     */
    @Select("SELECT * FROM doc_category WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
    List<DocumentCategory> selectByParentId(Long parentId);

    /**
     * 查询所有分类（树形结构用）
     */
    @Select("SELECT * FROM doc_category WHERE deleted = false ORDER BY parent_id, sort_order")
    List<DocumentCategory> selectAllCategories();
}
