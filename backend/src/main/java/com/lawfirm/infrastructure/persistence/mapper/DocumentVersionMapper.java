package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.document.entity.DocumentVersion;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 文档版本Mapper */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {

  /**
   * 查询文档的所有版本.
   *
   * @param documentId 文档ID
   * @return 文档版本列表
   */
  @Select("SELECT * FROM doc_version WHERE document_id = #{documentId} ORDER BY version DESC")
  List<DocumentVersion> selectByDocumentId(@Param("documentId") Long documentId);

  /**
   * 查询文档的最新版本号.
   *
   * @param documentId 文档ID
   * @return 最新版本号
   */
  @Select("SELECT COALESCE(MAX(version), 0) FROM doc_version WHERE document_id = #{documentId}")
  Integer selectMaxVersion(@Param("documentId") Long documentId);

  /**
   * 根据文档ID和版本号查询版本记录.
   *
   * @param documentId 文档ID
   * @param version 版本号
   * @return 文档版本记录，不存在则返回null
   */
  @Select("SELECT * FROM doc_version WHERE document_id = #{documentId} AND version = #{version}")
  DocumentVersion selectByDocumentIdAndVersion(
      @Param("documentId") Long documentId, @Param("version") Integer version);
}
