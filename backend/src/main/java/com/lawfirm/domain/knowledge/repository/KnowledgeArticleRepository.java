package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.KnowledgeArticle;
import com.lawfirm.infrastructure.persistence.mapper.KnowledgeArticleMapper;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeArticleRepository extends AbstractRepository<KnowledgeArticleMapper, KnowledgeArticle> {
}
