package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.document.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文档Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /**
     * 分页查询文档
     */
    @Select("<script>" +
            "SELECT * FROM doc_document WHERE deleted = false " +
            "<if test='title != null and title != \"\"'> AND title LIKE CONCAT('%', #{title}, '%') </if>" +
            "<if test='categoryId != null'> AND category_id = #{categoryId} </if>" +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='securityLevel != null and securityLevel != \"\"'> AND security_level = #{securityLevel} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='fileType != null and fileType != \"\"'> AND file_type = #{fileType} </if>" +
            "AND is_latest = true " +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<Document> selectDocumentPage(Page<Document> page,
                                       @Param("title") String title,
                                       @Param("categoryId") Long categoryId,
                                       @Param("matterId") Long matterId,
                                       @Param("securityLevel") String securityLevel,
                                       @Param("status") String status,
                                       @Param("fileType") String fileType);

    /**
     * 查询文档所有版本
     */
    @Select("SELECT * FROM doc_document WHERE (id = #{docId} OR parent_doc_id = #{docId}) AND deleted = false ORDER BY version DESC")
    java.util.List<Document> selectAllVersions(@Param("docId") Long docId);
}
