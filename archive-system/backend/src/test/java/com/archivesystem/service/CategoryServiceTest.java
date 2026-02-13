package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.Category;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.CategoryMapper;
import com.archivesystem.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setCategoryCode("01");
        testCategory.setCategoryName("诉讼档案");
        testCategory.setArchiveType("LITIGATION");
        testCategory.setLevel(1);
        testCategory.setSortOrder(1);
        testCategory.setStatus(Category.STATUS_ACTIVE);
    }

    @Test
    void testCreateCategory_Success() {
        when(categoryMapper.selectByCategoryCode("01")).thenReturn(null);
        when(categoryMapper.insert(any(Category.class))).thenReturn(1);

        Category result = categoryService.create(testCategory);

        assertNotNull(result);
        verify(categoryMapper).insert(any(Category.class));
    }

    @Test
    void testCreateCategory_CodeExists() {
        when(categoryMapper.selectByCategoryCode("01")).thenReturn(testCategory);

        assertThrows(BusinessException.class, () -> categoryService.create(testCategory));
    }

    @Test
    void testCreateCategory_WithParent() {
        Category parentCategory = new Category();
        parentCategory.setId(0L);
        parentCategory.setCategoryName("档案");
        parentCategory.setLevel(1);
        parentCategory.setFullPath("档案");

        Category childCategory = new Category();
        childCategory.setParentId(0L);
        childCategory.setCategoryCode("0101");
        childCategory.setCategoryName("民事诉讼");

        when(categoryMapper.selectByCategoryCode("0101")).thenReturn(null);
        when(categoryMapper.selectById(0L)).thenReturn(parentCategory);
        when(categoryMapper.insert(any(Category.class))).thenReturn(1);

        Category result = categoryService.create(childCategory);

        assertNotNull(result);
        assertEquals(2, result.getLevel());
    }

    @Test
    void testGetById_Success() {
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);

        Category result = categoryService.getById(1L);

        assertNotNull(result);
        assertEquals("诉讼档案", result.getCategoryName());
    }

    @Test
    void testGetById_NotFound() {
        when(categoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> categoryService.getById(999L));
    }

    @Test
    void testGetTree() {
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryMapper.selectList(any())).thenReturn(categories);

        List<Category> result = categoryService.getTree();

        assertNotNull(result);
    }

    @Test
    void testGetChildren() {
        List<Category> children = Arrays.asList(testCategory);
        when(categoryMapper.selectByParentId(1L)).thenReturn(children);

        List<Category> result = categoryService.getChildren(1L);

        assertEquals(1, result.size());
    }

    @Test
    void testDeleteCategory_Success() {
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.selectByParentId(1L)).thenReturn(Collections.emptyList());
        when(archiveMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> categoryService.delete(1L));

        verify(categoryMapper).deleteById(1L);
    }

    @Test
    void testDeleteCategory_HasChildren() {
        List<Category> children = Arrays.asList(testCategory);
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.selectByParentId(1L)).thenReturn(children);

        assertThrows(BusinessException.class, () -> categoryService.delete(1L));
    }

    @Test
    void testDeleteCategory_HasArchives() {
        List<Category> children = Collections.emptyList();
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.selectByParentId(1L)).thenReturn(children);
        when(archiveMapper.selectCount(any())).thenReturn(5L);

        assertThrows(BusinessException.class, () -> categoryService.delete(1L));
    }

    @Test
    void testMoveCategory_Success() {
        Category targetCategory = new Category();
        targetCategory.setId(2L);
        targetCategory.setCategoryName("目标分类");
        targetCategory.setLevel(1);
        targetCategory.setFullPath("目标分类");

        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.selectById(2L)).thenReturn(targetCategory);
        when(categoryMapper.selectByParentId(1L)).thenReturn(Collections.emptyList());
        when(categoryMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> categoryService.move(1L, 2L));

        verify(categoryMapper).updateById(any(Category.class));
    }

    @Test
    void testMoveCategory_ToSelf() {
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);

        assertThrows(BusinessException.class, () -> categoryService.move(1L, 1L));
    }

    @Test
    void testCountArchives() {
        when(archiveMapper.selectCount(any())).thenReturn(10L);

        long count = categoryService.countArchives(1L);

        assertEquals(10L, count);
    }
}
