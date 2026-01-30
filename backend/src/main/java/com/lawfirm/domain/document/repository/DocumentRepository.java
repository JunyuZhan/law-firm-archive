package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 文档仓储。
 *
 * <p>提供文档数据的持久化操作。
 */
@Repository
public class DocumentRepository extends AbstractRepository<DocumentMapper, Document> {

  /**
   * 查询文档所有版本。
   *
   * @param docId 文档ID
   * @return 文档版本列表
   */
  public List<Document> findAllVersions(final Long docId) {
    return baseMapper.selectAllVersions(docId);
  }

  /**
   * 根据卷宗目录项ID查询文档。
   *
   * @param dossierItemId 卷宗目录项ID
   * @return 文档列表
   */
  public List<Document> findByDossierItemId(final Long dossierItemId) {
    return baseMapper.selectByDossierItemId(dossierItemId);
  }
}
