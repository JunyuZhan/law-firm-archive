package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.document.entity.DocumentTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 文档模板Mapper
 */
@Mapper
public interface DocumentTemplateMapper extends BaseMapper<DocumentTemplate> {

    /**
     * 分页查询模板
     */
    @Select("<script>" +
            "SELECT * FROM doc_template WHERE deleted = false " +
            "<if test='name != null and name != \"\"'> AND name LIKE CONCAT('%', #{name}, '%') </if>" +
            "<if test='categoryId != null'> AND category_id = #{categoryId} </if>" +
            "<if test='templateType != null and templateType != \"\"'> AND template_type = #{templateType} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "ORDER BY use_count DESC, created_at DESC" +
            "</script>")
    IPage<DocumentTemplate> selectTemplatePage(Page<DocumentTemplate> page,
                                               @Param("name") String name,
                                               @Param("categoryId") Long categoryId,
                                               @Param("templateType") String templateType,
                                               @Param("status") String status);

    /**
     * 增加使用次数
     */
    @Update("UPDATE doc_template SET use_count = use_count + 1 WHERE id = #{id}")
    int incrementUseCount(@Param("id") Long id);
}
