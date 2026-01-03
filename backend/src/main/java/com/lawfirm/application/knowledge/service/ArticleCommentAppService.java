package com.lawfirm.application.knowledge.service;

import com.lawfirm.application.knowledge.command.CreateArticleCommentCommand;
import com.lawfirm.application.knowledge.dto.ArticleCommentDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.ArticleComment;
import com.lawfirm.domain.knowledge.entity.KnowledgeArticle;
import com.lawfirm.domain.knowledge.repository.ArticleCommentRepository;
import com.lawfirm.domain.knowledge.repository.KnowledgeArticleRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ArticleCommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章评论应用服务（M10-022）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCommentAppService {

    private final ArticleCommentRepository commentRepository;
    private final ArticleCommentMapper commentMapper;
    private final KnowledgeArticleRepository articleRepository;
    private final UserRepository userRepository;

    /**
     * 创建评论
     */
    @Transactional
    public ArticleCommentDTO createComment(CreateArticleCommentCommand command) {
        KnowledgeArticle article = articleRepository.getByIdOrThrow(command.getArticleId(), "文章不存在");
        Long userId = SecurityUtils.getUserId();

        // 如果是回复，检查父评论是否存在
        if (command.getParentId() != null) {
            ArticleComment parent = commentRepository.getById(command.getParentId());
            if (parent == null || !parent.getArticleId().equals(command.getArticleId())) {
                throw new BusinessException("父评论不存在或不属于该文章");
            }
        }

        ArticleComment comment = ArticleComment.builder()
                .articleId(command.getArticleId())
                .userId(userId)
                .parentId(command.getParentId())
                .content(command.getContent())
                .likeCount(0)
                .build();
        commentRepository.save(comment);

        // 增加文章评论数
        commentMapper.incrementCommentCount(command.getArticleId());

        log.info("创建文章评论: articleId={}, userId={}", command.getArticleId(), userId);
        return toDTO(comment);
    }

    /**
     * 获取文章的所有评论
     */
    public List<ArticleCommentDTO> getArticleComments(Long articleId) {
        List<ArticleComment> comments = commentMapper.selectByArticleId(articleId);
        return comments.stream()
                .filter(c -> c.getParentId() == null) // 只返回顶级评论
                .map(comment -> {
                    ArticleCommentDTO dto = toDTO(comment);
                    // 获取回复
                    List<ArticleComment> replies = commentMapper.selectByParentId(comment.getId());
                    dto.setReplies(replies.stream()
                            .map(this::toDTO)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 删除评论
     */
    @Transactional
    public void deleteComment(Long id) {
        ArticleComment comment = commentRepository.getByIdOrThrow(id, "评论不存在");
        Long userId = SecurityUtils.getUserId();

        // 只有评论作者可以删除
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评论");
        }

        commentRepository.removeById(id);
        // 减少文章评论数
        commentMapper.decrementCommentCount(comment.getArticleId());
        log.info("删除文章评论: id={}", id);
    }

    /**
     * 点赞评论
     */
    @Transactional
    public void likeComment(Long id) {
        commentRepository.getByIdOrThrow(id, "评论不存在");
        commentMapper.incrementLikeCount(id);
        log.info("点赞评论: id={}", id);
    }

    private ArticleCommentDTO toDTO(ArticleComment comment) {
        ArticleCommentDTO dto = new ArticleCommentDTO();
        dto.setId(comment.getId());
        dto.setArticleId(comment.getArticleId());
        dto.setUserId(comment.getUserId());
        dto.setParentId(comment.getParentId());
        dto.setContent(comment.getContent());
        dto.setLikeCount(comment.getLikeCount());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        // 获取用户信息
        User user = userRepository.getById(comment.getUserId());
        if (user != null) {
            dto.setUserName(user.getRealName());
        }

        return dto;
    }
}

