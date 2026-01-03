package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.document.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档版本Mapper
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {

    /**
     * 查询文档的所有版本
     */
    @Select("SELECT * FROM doc_version WHERE document_id = #{documentId} ORDER BY version DESC")
    List<DocumentVersion> selectByDocumentId(@Param("documentId") Long documentId);

    /**
     * 查询文档的最新版本号
     */
    @Select("SELECT COALESCE(MAX(version), 0) FROM doc_version WHERE document_id = #{documentId}")
    Integer selectMaxVersion(@Param("documentId") Long documentId);
}
