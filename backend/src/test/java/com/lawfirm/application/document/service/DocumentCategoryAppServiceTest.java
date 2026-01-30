package com.lawfirm.application.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.document.dto.DocumentCategoryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.domain.document.repository.DocumentCategoryRepository;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** DocumentCategoryAppService 单元测试 测试文档分类管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentCategoryAppService 文档分类服务测试")
class DocumentCategoryAppServiceTest {

  private static final Long TEST_CATEGORY_ID = 100L;
  private static final Long TEST_PARENT_ID = 200L;

  @Mock private DocumentCategoryRepository categoryRepository;

  @Mock private DocumentMapper documentMapper;

  @InjectMocks private DocumentCategoryAppService documentCategoryAppService;

  @Nested
  @DisplayName("创建分类测试")
  class CreateCategoryTests {

    @Test
    @DisplayName("应该成功创建分类")
    void createCategory_shouldSuccess() {
      // Given
      when(categoryRepository.findByParentId(0L)).thenReturn(Collections.emptyList());
      when(categoryRepository.save(any(DocumentCategory.class)))
          .thenAnswer(
              invocation -> {
                DocumentCategory category = invocation.getArgument(0);
                category.setId(TEST_CATEGORY_ID);
                return true;
              });

      // When
      DocumentCategoryDTO result = documentCategoryAppService.createCategory("合同类", null, "合同相关文档");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getName()).isEqualTo("合同类");
      verify(categoryRepository).save(any(DocumentCategory.class));
    }

    @Test
    @DisplayName("分类名称不能为空")
    void createCategory_shouldFail_whenNameEmpty() {
      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> documentCategoryAppService.createCategory("", null, null));
      assertThat(exception.getMessage()).contains("分类名称不能为空");
    }

    @Test
    @DisplayName("应该成功创建子分类")
    void createCategory_shouldSuccess_whenHasParent() {
      // Given
      DocumentCategory sibling = DocumentCategory.builder().id(300L).sortOrder(5).build();

      when(categoryRepository.findByParentId(TEST_PARENT_ID))
          .thenReturn(Collections.singletonList(sibling));
      when(categoryRepository.save(any(DocumentCategory.class)))
          .thenAnswer(
              invocation -> {
                DocumentCategory category = invocation.getArgument(0);
                category.setId(TEST_CATEGORY_ID);
                return true;
              });

      // When
      DocumentCategoryDTO result =
          documentCategoryAppService.createCategory("子分类", TEST_PARENT_ID, null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getParentId()).isEqualTo(TEST_PARENT_ID);
    }
  }

  @Nested
  @DisplayName("更新分类测试")
  class UpdateCategoryTests {

    @Test
    @DisplayName("应该成功更新分类")
    void updateCategory_shouldSuccess() {
      // Given
      DocumentCategory category =
          DocumentCategory.builder()
              .id(TEST_CATEGORY_ID)
              .name("原名称")
              .description("原描述")
              .sortOrder(1)
              .build();

      when(categoryRepository.getByIdOrThrow(eq(TEST_CATEGORY_ID), anyString()))
          .thenReturn(category);
      lenient().when(categoryRepository.updateById(any(DocumentCategory.class))).thenReturn(true);

      // When
      DocumentCategoryDTO result =
          documentCategoryAppService.updateCategory(TEST_CATEGORY_ID, "新名称", "新描述", 2);

      // Then
      assertThat(result).isNotNull();
      assertThat(category.getName()).isEqualTo("新名称");
      assertThat(category.getDescription()).isEqualTo("新描述");
      assertThat(category.getSortOrder()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("删除分类测试")
  class DeleteCategoryTests {

    @Test
    @DisplayName("应该成功删除分类")
    void deleteCategory_shouldSuccess() {
      // Given
      DocumentCategory category =
          DocumentCategory.builder().id(TEST_CATEGORY_ID).name("分类1").build();

      when(categoryRepository.getByIdOrThrow(eq(TEST_CATEGORY_ID), anyString()))
          .thenReturn(category);
      when(categoryRepository.findByParentId(TEST_CATEGORY_ID)).thenReturn(Collections.emptyList());
      when(documentMapper.countByCategoryId(TEST_CATEGORY_ID)).thenReturn(0);
      lenient().when(categoryRepository.removeById(TEST_CATEGORY_ID)).thenReturn(true);

      // When
      documentCategoryAppService.deleteCategory(TEST_CATEGORY_ID);

      // Then
      verify(categoryRepository).removeById(TEST_CATEGORY_ID);
    }

    @Test
    @DisplayName("有子分类的分类不能删除")
    void deleteCategory_shouldFail_whenHasChildren() {
      // Given
      DocumentCategory category = DocumentCategory.builder().id(TEST_CATEGORY_ID).build();

      DocumentCategory child =
          DocumentCategory.builder().id(300L).parentId(TEST_CATEGORY_ID).build();

      when(categoryRepository.getByIdOrThrow(eq(TEST_CATEGORY_ID), anyString()))
          .thenReturn(category);
      when(categoryRepository.findByParentId(TEST_CATEGORY_ID))
          .thenReturn(Collections.singletonList(child));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> documentCategoryAppService.deleteCategory(TEST_CATEGORY_ID));
      assertThat(exception.getMessage()).contains("子分类");
    }

    @Test
    @DisplayName("有关联文档的分类不能删除")
    void deleteCategory_shouldFail_whenHasDocuments() {
      // Given
      DocumentCategory category = DocumentCategory.builder().id(TEST_CATEGORY_ID).build();

      when(categoryRepository.getByIdOrThrow(eq(TEST_CATEGORY_ID), anyString()))
          .thenReturn(category);
      when(categoryRepository.findByParentId(TEST_CATEGORY_ID)).thenReturn(Collections.emptyList());
      when(documentMapper.countByCategoryId(TEST_CATEGORY_ID)).thenReturn(5);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> documentCategoryAppService.deleteCategory(TEST_CATEGORY_ID));
      assertThat(exception.getMessage()).contains("存在文档");
    }
  }

  @Nested
  @DisplayName("查询分类测试")
  class QueryCategoryTests {

    @Test
    @DisplayName("应该成功获取分类树")
    void getCategoryTree_shouldSuccess() {
      // Given
      DocumentCategory root =
          DocumentCategory.builder().id(TEST_CATEGORY_ID).name("根分类").parentId(0L).build();

      DocumentCategory child =
          DocumentCategory.builder().id(300L).name("子分类").parentId(TEST_CATEGORY_ID).build();

      when(categoryRepository.findAll()).thenReturn(List.of(root, child));

      // When
      List<DocumentCategoryDTO> result = documentCategoryAppService.getCategoryTree();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("根分类");
      assertThat(result.get(0).getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("应该成功获取子分类")
    void getChildren_shouldSuccess() {
      // Given
      DocumentCategory child =
          DocumentCategory.builder().id(300L).name("子分类").parentId(TEST_PARENT_ID).build();

      when(categoryRepository.findByParentId(TEST_PARENT_ID))
          .thenReturn(Collections.singletonList(child));

      // When
      List<DocumentCategoryDTO> result = documentCategoryAppService.getChildren(TEST_PARENT_ID);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("子分类");
    }
  }
}
