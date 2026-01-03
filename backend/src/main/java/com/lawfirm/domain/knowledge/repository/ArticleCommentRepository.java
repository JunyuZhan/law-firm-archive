package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.ArticleComment;
import com.lawfirm.infrastructure.persistence.mapper.ArticleCommentMapper;
import org.springframework.stereotype.Repository;

/**
 * 文章评论仓储（M10-022）
 */
@Repository
public class ArticleCommentRepository extends AbstractRepository<ArticleCommentMapper, ArticleComment> {
}

