package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.ArticleCollection;
import com.lawfirm.infrastructure.persistence.mapper.ArticleCollectionMapper;
import org.springframework.stereotype.Repository;

/**
 * 文章收藏仓储（M10-023）
 */
@Repository
public class ArticleCollectionRepository extends AbstractRepository<ArticleCollectionMapper, ArticleCollection> {
}

