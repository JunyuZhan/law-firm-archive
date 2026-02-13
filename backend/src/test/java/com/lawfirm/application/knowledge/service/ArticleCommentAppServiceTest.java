package com.lawfirm.application.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** ArticleCommentAppService 单元测试 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleCommentAppService 文章评论服务测试")
class ArticleCommentAppServiceTest {

  private static final Long TEST_ARTICLE_ID = 100L;
  private static final Long TEST_COMMENT_ID = 200L;
  private static final Long TEST_USER_ID = 1L;
  private static final Long OTHER_USER_ID = 999L;
  private static final Long PARENT_COMMENT_ID = 300L;

  @Mock private ArticleCommentRepository commentRepository;

  @Mock private ArticleCommentMapper commentMapper;

  @Mock private KnowledgeArticleRepository articleRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private ArticleCommentAppService commentAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("创建评论测试")
  class CreateCommentTests {

    @Test
    @DisplayName("应该成功创建顶级评论")
    void createComment_shouldSuccess_whenTopLevel() {
      // Given
      KnowledgeArticle article =
          KnowledgeArticle.builder().id(TEST_ARTICLE_ID).title("测试文章").build();

      CreateArticleCommentCommand command = new CreateArticleCommentCommand();
      command.setArticleId(TEST_ARTICLE_ID);
      command.setContent("这是一条评论");

      when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
      when(commentRepository.save(any(ArticleComment.class)))
          .thenAnswer(
              invocation -> {
                ArticleComment comment = invocation.getArgument(0);
                comment.setId(TEST_COMMENT_ID);
                return true;
              });
      doNothing().when(commentMapper).incrementCommentCount(TEST_ARTICLE_ID);

      User user = User.builder().id(TEST_USER_ID).realName("测试用户").build();
      when(userRepository.getById(TEST_USER_ID)).thenReturn(user);

      // When
      ArticleCommentDTO result = commentAppService.createComment(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEqualTo("这是一条评论");
      assertThat(result.getArticleId()).isEqualTo(TEST_ARTICLE_ID);
      assertThat(result.getParentId()).isNull();
      verify(commentRepository).save(any(ArticleComment.class));
      verify(commentMapper).incrementCommentCount(TEST_ARTICLE_ID);
    }

    @Test
    @DisplayName("应该成功创建回复评论")
    void createComment_shouldSuccess_whenReply() {
      // Given
      KnowledgeArticle article =
          KnowledgeArticle.builder().id(TEST_ARTICLE_ID).title("测试文章").build();

      ArticleComment parentComment =
          ArticleComment.builder().id(PARENT_COMMENT_ID).articleId(TEST_ARTICLE_ID).build();

      CreateArticleCommentCommand command = new CreateArticleCommentCommand();
      command.setArticleId(TEST_ARTICLE_ID);
      command.setParentId(PARENT_COMMENT_ID);
      command.setContent("这是一条回复");

      when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
      when(commentRepository.getById(PARENT_COMMENT_ID)).thenReturn(parentComment);
      when(commentRepository.save(any(ArticleComment.class)))
          .thenAnswer(
              invocation -> {
                ArticleComment comment = invocation.getArgument(0);
                comment.setId(TEST_COMMENT_ID);
                return true;
              });
      doNothing().when(commentMapper).incrementCommentCount(TEST_ARTICLE_ID);

      User user = User.builder().id(TEST_USER_ID).realName("测试用户").build();
      when(userRepository.getById(TEST_USER_ID)).thenReturn(user);

      // When
      ArticleCommentDTO result = commentAppService.createComment(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEqualTo("这是一条回复");
      assertThat(result.getParentId()).isEqualTo(PARENT_COMMENT_ID);
      verify(commentRepository).save(any(ArticleComment.class));
    }

    @Test
    @DisplayName("应该失败当文章不存在")
    void createComment_shouldFail_whenArticleNotExists() {
      // Given
      CreateArticleCommentCommand command = new CreateArticleCommentCommand();
      command.setArticleId(TEST_ARTICLE_ID);
      command.setContent("评论内容");

      when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString()))
          .thenThrow(new BusinessException("文章不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> commentAppService.createComment(command));
    }

    @Test
    @DisplayName("应该失败当父评论不存在")
    void createComment_shouldFail_whenParentCommentNotExists() {
      // Given
      KnowledgeArticle article = KnowledgeArticle.builder().id(TEST_ARTICLE_ID).build();

      CreateArticleCommentCommand command = new CreateArticleCommentCommand();
      command.setArticleId(TEST_ARTICLE_ID);
      command.setParentId(PARENT_COMMENT_ID);
      command.setContent("回复内容");

      when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
      when(commentRepository.getById(PARENT_COMMENT_ID)).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> commentAppService.createComment(command));
      assertThat(exception.getMessage()).contains("父评论不存在");
    }

    @Test
    @DisplayName("应该失败当父评论不属于该文章")
    void createComment_shouldFail_whenParentCommentNotBelongToArticle() {
      // Given
      KnowledgeArticle article = KnowledgeArticle.builder().id(TEST_ARTICLE_ID).build();

      ArticleComment parentComment =
          ArticleComment.builder()
              .id(PARENT_COMMENT_ID)
              .articleId(999L) // 不同的文章ID
              .build();

      CreateArticleCommentCommand command = new CreateArticleCommentCommand();
      command.setArticleId(TEST_ARTICLE_ID);
      command.setParentId(PARENT_COMMENT_ID);
      command.setContent("回复内容");

      when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
      when(commentRepository.getById(PARENT_COMMENT_ID)).thenReturn(parentComment);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> commentAppService.createComment(command));
      assertThat(exception.getMessage()).contains("父评论不存在或不属于该文章");
    }
  }

  @Nested
  @DisplayName("查询评论测试")
  class QueryCommentTests {

    @Test
    @DisplayName("应该成功获取文章的所有评论")
    void getArticleComments_shouldSuccess() {
      // Given
      ArticleComment topComment =
          ArticleComment.builder()
              .id(TEST_COMMENT_ID)
              .articleId(TEST_ARTICLE_ID)
              .userId(TEST_USER_ID)
              .content("顶级评论")
              .parentId(null)
              .likeCount(5)
              .build();

      ArticleComment replyComment =
          ArticleComment.builder()
              .id(PARENT_COMMENT_ID)
              .articleId(TEST_ARTICLE_ID)
              .userId(OTHER_USER_ID)
              .content("回复评论")
              .parentId(TEST_COMMENT_ID)
              .likeCount(2)
              .build();

      when(commentMapper.selectByArticleId(TEST_ARTICLE_ID))
          .thenReturn(List.of(topComment, replyComment));
      when(commentMapper.selectByParentId(TEST_COMMENT_ID)).thenReturn(List.of(replyComment));

      User user1 = User.builder().id(TEST_USER_ID).realName("用户1").build();
      User user2 = User.builder().id(OTHER_USER_ID).realName("用户2").build();
      when(userRepository.getById(TEST_USER_ID)).thenReturn(user1);
      when(userRepository.getById(OTHER_USER_ID)).thenReturn(user2);

      // When
      List<ArticleCommentDTO> result = commentAppService.getArticleComments(TEST_ARTICLE_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1); // 只返回顶级评论
      assertThat(result.get(0).getReplies()).hasSize(1); // 包含回复
      assertThat(result.get(0).getContent()).isEqualTo("顶级评论");
    }

    @Test
    @DisplayName("应该返回空列表当没有评论")
    void getArticleComments_shouldReturnEmpty_whenNoComments() {
      // Given
      when(commentMapper.selectByArticleId(TEST_ARTICLE_ID)).thenReturn(Collections.emptyList());

      // When
      List<ArticleCommentDTO> result = commentAppService.getArticleComments(TEST_ARTICLE_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("删除评论测试")
  class DeleteCommentTests {

    @Test
    @DisplayName("应该成功删除自己的评论")
    void deleteComment_shouldSuccess() {
      // Given
      ArticleComment comment =
          ArticleComment.builder()
              .id(TEST_COMMENT_ID)
              .articleId(TEST_ARTICLE_ID)
              .userId(TEST_USER_ID)
              .content("评论内容")
              .build();

      when(commentRepository.getByIdOrThrow(eq(TEST_COMMENT_ID), anyString())).thenReturn(comment);
      when(commentRepository.removeById(TEST_COMMENT_ID)).thenReturn(true);
      doNothing().when(commentMapper).decrementCommentCount(TEST_ARTICLE_ID);

      // When
      commentAppService.deleteComment(TEST_COMMENT_ID);

      // Then
      verify(commentRepository).removeById(TEST_COMMENT_ID);
      verify(commentMapper).decrementCommentCount(TEST_ARTICLE_ID);
    }

    @Test
    @DisplayName("应该失败当评论不存在")
    void deleteComment_shouldFail_whenCommentNotExists() {
      // Given
      when(commentRepository.getByIdOrThrow(eq(TEST_COMMENT_ID), anyString()))
          .thenThrow(new BusinessException("评论不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> commentAppService.deleteComment(TEST_COMMENT_ID));
    }

    @Test
    @DisplayName("应该失败当不是评论作者")
    void deleteComment_shouldFail_whenNotOwner() {
      // Given
      ArticleComment comment =
          ArticleComment.builder()
              .id(TEST_COMMENT_ID)
              .articleId(TEST_ARTICLE_ID)
              .userId(OTHER_USER_ID) // 其他用户
              .build();

      when(commentRepository.getByIdOrThrow(eq(TEST_COMMENT_ID), anyString())).thenReturn(comment);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> commentAppService.deleteComment(TEST_COMMENT_ID));
      assertThat(exception.getMessage()).contains("只能删除自己的评论");
    }
  }

  @Nested
  @DisplayName("点赞评论测试")
  class LikeCommentTests {

    @Test
    @DisplayName("应该成功点赞评论")
    void likeComment_shouldSuccess() {
      // Given
      ArticleComment comment = ArticleComment.builder().id(TEST_COMMENT_ID).build();

      when(commentRepository.getByIdOrThrow(eq(TEST_COMMENT_ID), anyString())).thenReturn(comment);
      doNothing().when(commentMapper).incrementLikeCount(TEST_COMMENT_ID);

      // When
      commentAppService.likeComment(TEST_COMMENT_ID);

      // Then
      verify(commentMapper).incrementLikeCount(TEST_COMMENT_ID);
    }

    @Test
    @DisplayName("应该失败当评论不存在")
    void likeComment_shouldFail_whenCommentNotExists() {
      // Given
      when(commentRepository.getByIdOrThrow(eq(TEST_COMMENT_ID), anyString()))
          .thenThrow(new BusinessException("评论不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> commentAppService.likeComment(TEST_COMMENT_ID));
    }
  }
}
