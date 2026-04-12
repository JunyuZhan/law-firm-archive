package com.archivesystem.controller;

import com.archivesystem.entity.Category;
import com.archivesystem.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private ObjectMapper objectMapper;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        objectMapper = new ObjectMapper();

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setCategoryCode("CAT001");
        testCategory.setCategoryName("测试分类");
        testCategory.setArchiveType("DOCUMENT");
        testCategory.setParentId(0L);
        testCategory.setLevel(1);
        testCategory.setSortOrder(1);
        testCategory.setStatus("ACTIVE");
    }

    @Test
    void testCreate_Success() throws Exception {
        Category newCategory = new Category();
        newCategory.setCategoryCode("CAT002");
        newCategory.setCategoryName("新分类");
        newCategory.setArchiveType("DOCUMENT");

        when(categoryService.create(any(Category.class))).thenReturn(testCategory);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.categoryCode").value("CAT001"));
    }

    @Test
    void testUpdate_Success() throws Exception {
        Category updateCategory = new Category();
        updateCategory.setCategoryCode("CAT001");
        updateCategory.setCategoryName("更新分类");
        updateCategory.setArchiveType("DOCUMENT");

        when(categoryService.update(eq(1L), any(Category.class))).thenReturn(testCategory);

        mockMvc.perform(put("/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetById_Success() throws Exception {
        when(categoryService.getById(1L)).thenReturn(testCategory);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.categoryCode").value("CAT001"));
    }

    @Test
    void testGetTree_Success() throws Exception {
        Category childCategory = new Category();
        childCategory.setId(2L);
        childCategory.setCategoryCode("CAT001-01");
        childCategory.setCategoryName("子分类");
        childCategory.setParentId(1L);
        childCategory.setLevel(2);

        testCategory.setChildren(Arrays.asList(childCategory));
        
        when(categoryService.getTree()).thenReturn(Arrays.asList(testCategory));

        mockMvc.perform(get("/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].categoryCode").value("CAT001"));
    }

    @Test
    void testGetTree_ByArchiveType() throws Exception {
        when(categoryService.getTreeByArchiveType("DOCUMENT")).thenReturn(Arrays.asList(testCategory));

        mockMvc.perform(get("/categories/tree")
                        .param("archiveType", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(categoryService).getTreeByArchiveType("DOCUMENT");
    }

    @Test
    void testGetTree_EmptyArchiveType() throws Exception {
        when(categoryService.getTree()).thenReturn(Arrays.asList(testCategory));

        mockMvc.perform(get("/categories/tree")
                        .param("archiveType", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(categoryService).getTree();
    }

    @Test
    void testGetChildren_Success() throws Exception {
        Category childCategory = new Category();
        childCategory.setId(2L);
        childCategory.setCategoryCode("CAT001-01");
        childCategory.setCategoryName("子分类");
        childCategory.setParentId(1L);

        when(categoryService.getChildren(1L)).thenReturn(Arrays.asList(childCategory));

        mockMvc.perform(get("/categories/1/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].parentId").value(1));
    }

    @Test
    void testGetChildren_Empty() throws Exception {
        when(categoryService.getChildren(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/categories/1/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testDelete_Success() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(categoryService).delete(1L);
    }

    @Test
    void testMove_Success() throws Exception {
        doNothing().when(categoryService).move(1L, 2L);

        mockMvc.perform(put("/categories/1/move")
                        .param("newParentId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(categoryService).move(1L, 2L);
    }

    @Test
    void testMove_ToRoot() throws Exception {
        doNothing().when(categoryService).move(1L, null);

        mockMvc.perform(put("/categories/1/move"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(categoryService).move(1L, null);
    }

    @Test
    void testStatistics_Success() throws Exception {
        when(categoryService.getById(1L)).thenReturn(testCategory);
        when(categoryService.countArchives(1L)).thenReturn(100L);

        mockMvc.perform(get("/categories/1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.archiveCount").value(100));
    }
}
