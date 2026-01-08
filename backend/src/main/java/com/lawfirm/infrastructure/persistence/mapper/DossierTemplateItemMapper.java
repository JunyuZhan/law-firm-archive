package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.document.entity.DossierTemplateItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 卷宗目录项 Mapper
 */
@Mapper
public interface DossierTemplateItemMapper extends BaseMapper<DossierTemplateItem> {

    /**
     * 根据模板ID查询目录项（按排序）
     */
    @Select("SELECT * FROM dossier_template_item WHERE template_id = #{templateId} ORDER BY sort_order")
    List<DossierTemplateItem> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据父ID查询子目录项
     */
    @Select("SELECT * FROM dossier_template_item WHERE template_id = #{templateId} AND parent_id = #{parentId} ORDER BY sort_order")
    List<DossierTemplateItem> selectByParentId(@Param("templateId") Long templateId, @Param("parentId") Long parentId);
}

