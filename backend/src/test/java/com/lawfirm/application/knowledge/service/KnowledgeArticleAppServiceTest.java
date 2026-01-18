package com.lawfirm.application.knowledge.service;

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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KnowledgeArticleAppService 单元测试
 * 测试知识文章管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeArticleAppService 知识文章服务测试")
class KnowledgeArticleAppServiceTest {

    private static final Long TEST_ARTICLE_ID = 100L;
    private static final Long TEST_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;

    @Mock
    private KnowledgeArticleRepository articleRepository;

    @Mock
    private KnowledgeArticleMapper articleMapper;

    @Mock
    private ArticleCollectionMapper articleCollectionMapper;

    @InjectMocks
    private KnowledgeArticleAppService articleAppService;

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
    @DisplayName("查询文章测试")
    class QueryArticleTests {

        @Test
        @DisplayName("应该成功分页查询文章")
        void listArticles_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("测试文章")
                    .authorId(TEST_USER_ID)
                    .status(KnowledgeArticle.STATUS_PUBLISHED)
                    .build();

            Page<KnowledgeArticle> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(article));
            page.setTotal(1L);

            when(articleMapper.selectArticlePage(any(Page.class), any(), any(), any(), any()))
                    .thenReturn(page);

            KnowledgeArticleQueryDTO query = new KnowledgeArticleQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<KnowledgeArticleDTO> result = articleAppService.listArticles(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1L);
        }

        @Test
        @DisplayName("应该成功获取文章详情")
        void getArticleById_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("测试文章")
                    .content("文章内容")
                    .authorId(TEST_USER_ID)
                    .status(KnowledgeArticle.STATUS_PUBLISHED)
                    .viewCount(10)
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
            when(articleMapper.incrementViewCount(TEST_ARTICLE_ID)).thenReturn(1);

            // When
            KnowledgeArticleDTO result = articleAppService.getArticleById(TEST_ARTICLE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("测试文章");
            verify(articleMapper).incrementViewCount(TEST_ARTICLE_ID);
        }
    }

    @Nested
    @DisplayName("创建文章测试")
    class CreateArticleTests {

        @Test
        @DisplayName("应该成功创建文章")
        void createArticle_shouldSuccess() {
            // Given
            CreateArticleCommand command = new CreateArticleCommand();
            command.setTitle("新文章");
            command.setContent("文章内容");
            command.setCategory("法律实务");
            command.setSummary("文章摘要");
            command.setTags("标签1,标签2");

            when(articleRepository.save(any(KnowledgeArticle.class))).thenAnswer(invocation -> {
                KnowledgeArticle article = invocation.getArgument(0);
                article.setId(TEST_ARTICLE_ID);
                return true;
            });

            // When
            KnowledgeArticleDTO result = articleAppService.createArticle(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("新文章");
            assertThat(result.getStatus()).isEqualTo(KnowledgeArticle.STATUS_DRAFT);
            verify(articleRepository).save(any(KnowledgeArticle.class));
        }
    }

    @Nested
    @DisplayName("更新文章测试")
    class UpdateArticleTests {

        @Test
        @DisplayName("应该成功更新自己的文章")
        void updateArticle_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("原标题")
                    .authorId(TEST_USER_ID)
                    .status(KnowledgeArticle.STATUS_DRAFT)
                    .build();

            CreateArticleCommand command = new CreateArticleCommand();
            command.setTitle("新标题");
            command.setContent("新内容");

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
            when(articleRepository.updateById(any(KnowledgeArticle.class))).thenReturn(true);

            // When
            KnowledgeArticleDTO result = articleAppService.updateArticle(TEST_ARTICLE_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(article.getTitle()).isEqualTo("新标题");
            assertThat(article.getContent()).isEqualTo("新内容");
            verify(articleRepository).updateById(article);
        }

        @Test
        @DisplayName("不能更新他人的文章")
        void updateArticle_shouldFail_whenNotOwner() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .authorId(OTHER_USER_ID) // 其他用户
                    .build();

            CreateArticleCommand command = new CreateArticleCommand();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleAppService.updateArticle(TEST_ARTICLE_ID, command));
            assertThat(exception.getMessage()).contains("只能编辑自己的文章");
        }
    }

    @Nested
    @DisplayName("删除文章测试")
    class DeleteArticleTests {

        @Test
        @DisplayName("应该成功删除自己的文章")
        void deleteArticle_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("测试文章")
                    .authorId(TEST_USER_ID)
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
            when(articleMapper.deleteById(TEST_ARTICLE_ID)).thenReturn(1);

            // When
            articleAppService.deleteArticle(TEST_ARTICLE_ID);

            // Then
            verify(articleMapper).deleteById(TEST_ARTICLE_ID);
        }

        @Test
        @DisplayName("不能删除他人的文章")
        void deleteArticle_shouldFail_whenNotOwner() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .authorId(OTHER_USER_ID) // 其他用户
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleAppService.deleteArticle(TEST_ARTICLE_ID));
            assertThat(exception.getMessage()).contains("只能删除自己的文章");
        }
    }

    @Nested
    @DisplayName("发布文章测试")
    class PublishArticleTests {

        @Test
        @DisplayName("应该成功发布自己的文章")
        void publishArticle_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("测试文章")
                    .authorId(TEST_USER_ID)
                    .status(KnowledgeArticle.STATUS_DRAFT)
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
            when(articleRepository.updateById(any(KnowledgeArticle.class))).thenReturn(true);

            // When
            KnowledgeArticleDTO result = articleAppService.publishArticle(TEST_ARTICLE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(article.getStatus()).isEqualTo(KnowledgeArticle.STATUS_PUBLISHED);
            assertThat(article.getPublishedAt()).isNotNull();
            verify(articleRepository).updateById(article);
        }

        @Test
        @DisplayName("不能发布他人的文章")
        void publishArticle_shouldFail_whenNotOwner() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .authorId(OTHER_USER_ID) // 其他用户
                    .status(KnowledgeArticle.STATUS_DRAFT)
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleAppService.publishArticle(TEST_ARTICLE_ID));
            assertThat(exception.getMessage()).contains("只能发布自己的文章");
        }

        @Test
        @DisplayName("已发布的文章不能重复发布")
        void publishArticle_shouldFail_whenAlreadyPublished() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .authorId(TEST_USER_ID)
                    .status(KnowledgeArticle.STATUS_PUBLISHED) // 已发布
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleAppService.publishArticle(TEST_ARTICLE_ID));
            assertThat(exception.getMessage()).contains("文章已发布");
        }
    }

    @Nested
    @DisplayName("归档文章测试")
    class ArchiveArticleTests {

        @Test
        @DisplayName("应该成功归档文章")
        void archiveArticle_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("测试文章")
                    .status(KnowledgeArticle.STATUS_PUBLISHED)
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
            when(articleRepository.updateById(any(KnowledgeArticle.class))).thenReturn(true);

            // When
            articleAppService.archiveArticle(TEST_ARTICLE_ID);

            // Then
            assertThat(article.getStatus()).isEqualTo(KnowledgeArticle.STATUS_ARCHIVED);
            verify(articleRepository).updateById(article);
        }
    }

    @Nested
    @DisplayName("点赞文章测试")
    class LikeArticleTests {

        @Test
        @DisplayName("应该成功点赞文章")
        void likeArticle_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .likeCount(10)
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
            when(articleMapper.incrementLikeCount(TEST_ARTICLE_ID)).thenReturn(1);

            // When
            articleAppService.likeArticle(TEST_ARTICLE_ID);

            // Then
            verify(articleMapper).incrementLikeCount(TEST_ARTICLE_ID);
        }
    }

    @Nested
    @DisplayName("我的文章测试")
    class MyArticlesTests {

        @Test
        @DisplayName("应该成功获取我的文章列表")
        void getMyArticles_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("我的文章")
                    .authorId(TEST_USER_ID)
                    .build();

            Page<KnowledgeArticle> page = new Page<>(1, 100);
            page.setRecords(Collections.singletonList(article));
            page.setTotal(1L);

            when(articleMapper.selectArticlePage(any(Page.class), eq(TEST_USER_ID), any(), any(), any()))
                    .thenReturn(page);

            // When
            List<KnowledgeArticleDTO> result = articleAppService.getMyArticles();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("我的文章");
        }
    }

    @Nested
    @DisplayName("收藏文章测试")
    class CollectArticleTests {

        @Test
        @DisplayName("应该成功收藏文章")
        void collectArticle_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("测试文章")
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
            when(articleCollectionMapper.countByUserAndArticle(eq(TEST_USER_ID), eq(TEST_ARTICLE_ID)))
                    .thenReturn(0); // 未收藏
            when(articleCollectionMapper.insert(any(ArticleCollection.class))).thenReturn(1);

            // When
            articleAppService.collectArticle(TEST_ARTICLE_ID);

            // Then
            verify(articleCollectionMapper).insert(any(ArticleCollection.class));
        }

        @Test
        @DisplayName("已收藏的文章不能重复收藏")
        void collectArticle_shouldFail_whenAlreadyCollected() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .build();

            ArticleCollection collection = ArticleCollection.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .articleId(TEST_ARTICLE_ID)
                    .build();

            when(articleRepository.getByIdOrThrow(eq(TEST_ARTICLE_ID), anyString())).thenReturn(article);
            when(articleCollectionMapper.countByUserAndArticle(eq(TEST_USER_ID), eq(TEST_ARTICLE_ID)))
                    .thenReturn(1); // 已收藏

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleAppService.collectArticle(TEST_ARTICLE_ID));
            assertThat(exception.getMessage()).contains("已收藏");
        }
    }

    @Nested
    @DisplayName("取消收藏测试")
    class UncollectTests {

        @Test
        @DisplayName("应该成功取消收藏")
        void uncollectArticle_shouldSuccess() {
            // Given
            when(articleCollectionMapper.delete(any())).thenReturn(1);

            // When
            articleAppService.uncollectArticle(TEST_ARTICLE_ID);

            // Then
            verify(articleCollectionMapper).delete(any());
        }
    }

    @Nested
    @DisplayName("获取收藏列表测试")
    class GetCollectedArticlesTests {

        @Test
        @DisplayName("应该成功获取我的收藏列表")
        void getMyCollectedArticles_shouldSuccess() {
            // Given
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(TEST_ARTICLE_ID)
                    .title("收藏的文章")
                    .build();

            when(articleCollectionMapper.selectArticleIdsByUserId(TEST_USER_ID))
                    .thenReturn(Collections.singletonList(TEST_ARTICLE_ID));
            when(articleRepository.getById(TEST_ARTICLE_ID)).thenReturn(article);

            // When
            List<KnowledgeArticleDTO> result = articleAppService.getMyCollectedArticles();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("收藏的文章");
        }

        @Test
        @DisplayName("没有收藏时应该返回空列表")
        void getMyCollectedArticles_shouldReturnEmpty() {
            // Given
            when(articleCollectionMapper.selectArticleIdsByUserId(TEST_USER_ID))
                    .thenReturn(Collections.emptyList());

            // When
            List<KnowledgeArticleDTO> result = articleAppService.getMyCollectedArticles();

            // Then
            assertThat(result).isEmpty();
        }
    }
}
