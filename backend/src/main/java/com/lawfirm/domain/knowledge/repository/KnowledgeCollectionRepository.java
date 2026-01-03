package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.KnowledgeCollection;
import com.lawfirm.infrastructure.persistence.mapper.KnowledgeCollectionMapper;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeCollectionRepository extends AbstractRepository<KnowledgeCollectionMapper, KnowledgeCollection> {
}
