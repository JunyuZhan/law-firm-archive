package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.document.entity.MatterDossierItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 项目卷宗目录 Mapper
 */
@Mapper
public interface MatterDossierItemMapper extends BaseMapper<MatterDossierItem> {

    /**
     * 根据项目ID查询目录项（按排序）
     */
    @Select("SELECT * FROM matter_dossier_item WHERE matter_id = #{matterId} AND deleted = false ORDER BY sort_order")
    List<MatterDossierItem> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 根据父ID查询子目录项
     */
    @Select("SELECT * FROM matter_dossier_item WHERE matter_id = #{matterId} AND parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
    List<MatterDossierItem> selectByParentId(@Param("matterId") Long matterId, @Param("parentId") Long parentId);

    /**
     * 更新目录项文件数量
     */
    @Update("UPDATE matter_dossier_item SET document_count = #{count}, updated_at = NOW() WHERE id = #{itemId}")
    int updateDocumentCount(@Param("itemId") Long itemId, @Param("count") Integer count);

    /**
     * 检查项目是否已初始化卷宗目录
     */
    @Select("SELECT COUNT(*) FROM matter_dossier_item WHERE matter_id = #{matterId} AND deleted = false")
    int countByMatterId(@Param("matterId") Long matterId);
}

