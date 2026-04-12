package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class CategoryTest {

    @Test
    void testBuilder() {
        List<Category> children = new ArrayList<>();

        Category category = Category.builder()
                .parentId(0L)
                .categoryCode("CAT-001")
                .categoryName("文书档案")
                .archiveType(Category.TYPE_DOCUMENT)
                .level(1)
                .sortOrder(1)
                .retentionPeriod("PERMANENT")
                .description("文书类档案")
                .status(Category.STATUS_ACTIVE)
                .fullPath("/CAT-001")
                .children(children)
                .build();

        assertEquals(0L, category.getParentId());
        assertEquals("CAT-001", category.getCategoryCode());
        assertEquals("文书档案", category.getCategoryName());
        assertEquals(Category.TYPE_DOCUMENT, category.getArchiveType());
        assertEquals(1, category.getLevel());
        assertEquals(1, category.getSortOrder());
        assertEquals("PERMANENT", category.getRetentionPeriod());
        assertEquals("文书类档案", category.getDescription());
        assertEquals(Category.STATUS_ACTIVE, category.getStatus());
        assertEquals("/CAT-001", category.getFullPath());
        assertEquals(children, category.getChildren());
    }

    @Test
    void testDefaultValues() {
        Category category = Category.builder().build();

        assertEquals(1, category.getLevel());
        assertEquals(0, category.getSortOrder());
        assertEquals(Category.STATUS_ACTIVE, category.getStatus());
    }

    @Test
    void testNoArgsConstructor() {
        Category category = new Category();

        assertNull(category.getCategoryCode());
        assertNull(category.getCategoryName());
    }

    @Test
    void testStatusConstants() {
        assertEquals("ACTIVE", Category.STATUS_ACTIVE);
        assertEquals("INACTIVE", Category.STATUS_INACTIVE);
    }

    @Test
    void testTypeConstants() {
        assertEquals("DOCUMENT", Category.TYPE_DOCUMENT);
        assertEquals("SCIENCE", Category.TYPE_SCIENCE);
        assertEquals("ACCOUNTING", Category.TYPE_ACCOUNTING);
        assertEquals("PERSONNEL", Category.TYPE_PERSONNEL);
        assertEquals("SPECIAL", Category.TYPE_SPECIAL);
        assertEquals("AUDIOVISUAL", Category.TYPE_AUDIOVISUAL);
    }

    @Test
    void testSettersAndGetters() {
        Category category = new Category();

        category.setParentId(1L);
        category.setCategoryCode("CAT-002");
        category.setCategoryName("科技档案");
        category.setArchiveType(Category.TYPE_SCIENCE);
        category.setLevel(2);
        category.setSortOrder(5);
        category.setRetentionPeriod("TEN_YEARS");
        category.setDescription("科技类档案");
        category.setStatus(Category.STATUS_INACTIVE);
        category.setFullPath("/CAT-001/CAT-002");

        assertEquals(1L, category.getParentId());
        assertEquals("CAT-002", category.getCategoryCode());
        assertEquals("科技档案", category.getCategoryName());
        assertEquals(Category.TYPE_SCIENCE, category.getArchiveType());
        assertEquals(2, category.getLevel());
        assertEquals(5, category.getSortOrder());
        assertEquals("TEN_YEARS", category.getRetentionPeriod());
        assertEquals("科技类档案", category.getDescription());
        assertEquals(Category.STATUS_INACTIVE, category.getStatus());
        assertEquals("/CAT-001/CAT-002", category.getFullPath());
    }

    @Test
    void testChildrenList() {
        Category parent = Category.builder()
                .categoryCode("PARENT")
                .categoryName("父分类")
                .build();

        List<Category> children = new ArrayList<>();
        children.add(Category.builder().categoryCode("CHILD-1").build());
        children.add(Category.builder().categoryCode("CHILD-2").build());

        parent.setChildren(children);

        assertEquals(2, parent.getChildren().size());
        assertEquals("CHILD-1", parent.getChildren().get(0).getCategoryCode());
    }

    @Test
    void testToString() {
        Category category = Category.builder()
                .categoryCode("TEST")
                .build();

        String str = category.toString();
        assertNotNull(str);
        assertTrue(str.contains("Category"));
    }
}
