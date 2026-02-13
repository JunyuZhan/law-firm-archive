package com.lawfirm.application.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.knowledge.command.CreateArticleCommand;
import com.lawfirm.application.knowledge.dto.KnowledgeArticleDTO;
import com.lawfirm.application.knowledge.dto.KnowledgeArticleQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.ArticleCollection;
import com.lawfirm.domain.knowledge.entity.KnowledgeArticle;
import com.lawfirm.domain.knowledge.repository.KnowledgeArticleRepository;
import com.lawfirm.infrastructure.persistence.mapper.ArticleCollectionMapper;
import com.lawfirm.infrastructure.persistence.mapper.KnowledgeArticleMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 经验文章应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeArticleAppService {

  /** 知识文章仓储 */
  private final KnowledgeArticleRepository articleRepository;

  /** 知识文章Mapper */
  private final KnowledgeArticleMapper articleMapper;

  /** 文章收藏Mapper */
  private final ArticleCollectionMapper articleCollectionMapper;

  /**
   * 分页查询文章
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<KnowledgeArticleDTO> listArticles(final KnowledgeArticleQueryDTO query) {
    IPage<KnowledgeArticle> page =
        articleMapper.selectArticlePage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getAuthorId(),
            query.getStatus(),
            query.getCategory(),
            query.getKeyword());

    List<KnowledgeArticleDTO> records =
        page.getRecords().stream().map(this::toArticleDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取文章详情
   *
   * @param id 文章ID
   * @return 文章DTO
   */
  public KnowledgeArticleDTO getArticleById(final Long id) {
    KnowledgeArticle article = articleRepository.getByIdOrThrow(id, "文章不存在");
    articleMapper.incrementViewCount(id);
    return toArticleDTO(article);
  }

  /**
   * 创建文章
   *
   * @param command 创建命令
   * @return 创建后的文章DTO
   * @throws BusinessException 如果标题或内容为空
   */
  @Transactional
  public KnowledgeArticleDTO createArticle(final CreateArticleCommand command) {
    if (!StringUtils.hasText(command.getTitle())) {
      throw new BusinessException("文章标题不能为空");
    }
    if (!StringUtils.hasText(command.getContent())) {
      throw new BusinessException("文章内容不能为空");
    }

    Long userId = SecurityUtils.getUserId();
    KnowledgeArticle article =
        KnowledgeArticle.builder()
            .title(command.getTitle())
            .category(command.getCategory())
            .content(command.getContent())
            .summary(command.getSummary())
            .authorId(userId)
            .status(KnowledgeArticle.STATUS_DRAFT)
            .tags(command.getTags())
            .viewCount(0)
            .likeCount(0)
            .commentCount(0)
            .build();

    articleRepository.save(article);
    log.info("文章创建成功: {}", article.getTitle());
    return toArticleDTO(article);
  }

  /**
   * 更新文章
   *
   * @param id 文章ID
   * @param command 更新命令
   * @return 更新后的文章DTO
   * @throws BusinessException 如果文章不存在或无权编辑
   */
  @Transactional
  public KnowledgeArticleDTO updateArticle(final Long id, final CreateArticleCommand command) {
    KnowledgeArticle article = articleRepository.getByIdOrThrow(id, "文章不存在");

    // 只有作者可以编辑
    Long userId = SecurityUtils.getUserId();
    if (!article.getAuthorId().equals(userId)) {
      throw new BusinessException("只能编辑自己的文章");
    }

    if (StringUtils.hasText(command.getTitle())) {
      article.setTitle(command.getTitle());
    }
    if (command.getCategory() != null) {
      article.setCategory(command.getCategory());
    }
    if (StringUtils.hasText(command.getContent())) {
      article.setContent(command.getContent());
    }
    if (command.getSummary() != null) {
      article.setSummary(command.getSummary());
    }
    if (command.getTags() != null) {
      article.setTags(command.getTags());
    }

    articleRepository.updateById(article);
    log.info("文章更新成功: {}", article.getTitle());
    return toArticleDTO(article);
  }

  /**
   * 删除文章
   *
   * @param id 文章ID
   * @throws BusinessException 如果文章不存在或无权删除
   */
  @Transactional
  public void deleteArticle(final Long id) {
    KnowledgeArticle article = articleRepository.getByIdOrThrow(id, "文章不存在");

    // 只有作者可以删除
    Long userId = SecurityUtils.getUserId();
    if (!article.getAuthorId().equals(userId)) {
      throw new BusinessException("只能删除自己的文章");
    }

    articleMapper.deleteById(id);
    log.info("文章删除成功: {}", article.getTitle());
  }

  /**
   * 发布文章
   *
   * @param id 文章ID
   * @return 发布后的文章DTO
   * @throws BusinessException 如果文章不存在、无权发布或已发布
   */
  @Transactional
  public KnowledgeArticleDTO publishArticle(final Long id) {
    KnowledgeArticle article = articleRepository.getByIdOrThrow(id, "文章不存在");

    Long userId = SecurityUtils.getUserId();
    if (!article.getAuthorId().equals(userId)) {
      throw new BusinessException("只能发布自己的文章");
    }

    if (KnowledgeArticle.STATUS_PUBLISHED.equals(article.getStatus())) {
      throw new BusinessException("文章已发布");
    }

    article.setStatus(KnowledgeArticle.STATUS_PUBLISHED);
    article.setPublishedAt(LocalDateTime.now());
    articleRepository.updateById(article);
    log.info("文章发布成功: {}", article.getTitle());
    return toArticleDTO(article);
  }

  /**
   * 归档文章
   *
   * @param id 文章ID
   */
  @Transactional
  public void archiveArticle(final Long id) {
    KnowledgeArticle article = articleRepository.getByIdOrThrow(id, "文章不存在");
    article.setStatus(KnowledgeArticle.STATUS_ARCHIVED);
    articleRepository.updateById(article);
    log.info("文章归档成功: {}", article.getTitle());
  }

  /**
   * 点赞文章
   *
   * @param id 文章ID
   */
  @Transactional
  public void likeArticle(final Long id) {
    articleRepository.getByIdOrThrow(id, "文章不存在");
    articleMapper.incrementLikeCount(id);
    log.info("文章点赞成功: id={}", id);
  }

  /**
   * 获取我的文章
   *
   * @return 我的文章列表
   */
  public List<KnowledgeArticleDTO> getMyArticles() {
    Long userId = SecurityUtils.getUserId();
    KnowledgeArticleQueryDTO query = new KnowledgeArticleQueryDTO();
    query.setAuthorId(userId);
    query.setPageNum(1);
    query.setPageSize(100);
    return listArticles(query).getRecords();
  }

  /**
   * 收藏文章（M10-023）
   *
   * @param articleId 文章ID
   * @throws BusinessException 如果文章不存在或已收藏
   */
  @Transactional
  public void collectArticle(final Long articleId) {
    articleRepository.getByIdOrThrow(articleId, "文章不存在");
    Long userId = SecurityUtils.getUserId();

    int count = articleCollectionMapper.countByUserAndArticle(userId, articleId);
    if (count > 0) {
      throw new BusinessException("已收藏该文章");
    }

    ArticleCollection collection =
        ArticleCollection.builder().userId(userId).articleId(articleId).build();
    articleCollectionMapper.insert(collection);
    log.info("文章收藏成功: userId={}, articleId={}", userId, articleId);
  }

  /**
   * 取消收藏文章（M10-023）
   *
   * @param articleId 文章ID
   */
  @Transactional
  public void uncollectArticle(final Long articleId) {
    Long userId = SecurityUtils.getUserId();
    articleCollectionMapper.delete(
        new LambdaQueryWrapper<ArticleCollection>()
            .eq(ArticleCollection::getUserId, userId)
            .eq(ArticleCollection::getArticleId, articleId));
    log.info("文章取消收藏: userId={}, articleId={}", userId, articleId);
  }

  /**
   * 获取我的收藏文章（M10-023）
   *
   * @return 收藏的文章列表
   */
  public List<KnowledgeArticleDTO> getMyCollectedArticles() {
    Long userId = SecurityUtils.getUserId();
    List<Long> articleIds = articleCollectionMapper.selectArticleIdsByUserId(userId);
    if (articleIds.isEmpty()) {
      return Collections.emptyList();
    }

    return articleIds.stream()
        .map(
            id -> {
              KnowledgeArticle article = articleRepository.getById(id);
              return article != null ? toArticleDTO(article) : null;
            })
        .filter(dto -> dto != null)
        .collect(Collectors.toList());
  }

  /**
   * 获取状态名称.
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case KnowledgeArticle.STATUS_DRAFT -> "草稿";
      case KnowledgeArticle.STATUS_PUBLISHED -> "已发布";
      case KnowledgeArticle.STATUS_ARCHIVED -> "已归档";
      default -> status;
    };
  }

  /**
   * 转换为DTO.
   *
   * @param article 文章实体
   * @return 文章DTO
   */
  private KnowledgeArticleDTO toArticleDTO(final KnowledgeArticle article) {
    KnowledgeArticleDTO dto = new KnowledgeArticleDTO();
    dto.setId(article.getId());
    dto.setTitle(article.getTitle());
    dto.setCategory(article.getCategory());
    dto.setContent(article.getContent());
    dto.setSummary(article.getSummary());
    dto.setAuthorId(article.getAuthorId());
    dto.setStatus(article.getStatus());
    dto.setStatusName(getStatusName(article.getStatus()));
    dto.setTags(article.getTags());
    dto.setViewCount(article.getViewCount());
    dto.setLikeCount(article.getLikeCount());
    dto.setCommentCount(article.getCommentCount());
    dto.setPublishedAt(article.getPublishedAt());
    dto.setCreatedAt(article.getCreatedAt());
    return dto;
  }
}
