package com.archivesystem.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PageResultTest {

    @Test
    void testBuilder() {
        List<String> records = Arrays.asList("item1", "item2");
        
        PageResult<String> result = PageResult.<String>builder()
                .current(1L)
                .size(10L)
                .total(100L)
                .pages(10L)
                .records(records)
                .build();

        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(100L, result.getTotal());
        assertEquals(10L, result.getPages());
        assertEquals(records, result.getRecords());
    }

    @Test
    void testNoArgsConstructor() {
        PageResult<String> result = new PageResult<>();

        assertNull(result.getCurrent());
        assertNull(result.getSize());
        assertNull(result.getTotal());
        assertNull(result.getPages());
        assertNull(result.getRecords());
    }

    @Test
    void testAllArgsConstructor() {
        List<String> records = Arrays.asList("a", "b", "c");
        
        PageResult<String> result = new PageResult<>(2L, 20L, 50L, 3L, records);

        assertEquals(2L, result.getCurrent());
        assertEquals(20L, result.getSize());
        assertEquals(50L, result.getTotal());
        assertEquals(3L, result.getPages());
        assertEquals(records, result.getRecords());
    }

    @Test
    void testSettersAndGetters() {
        PageResult<Integer> result = new PageResult<>();
        List<Integer> records = Arrays.asList(1, 2, 3);

        result.setCurrent(5L);
        result.setSize(15L);
        result.setTotal(75L);
        result.setPages(5L);
        result.setRecords(records);

        assertEquals(5L, result.getCurrent());
        assertEquals(15L, result.getSize());
        assertEquals(75L, result.getTotal());
        assertEquals(5L, result.getPages());
        assertEquals(records, result.getRecords());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOfFromIPage() {
        IPage<String> page = mock(IPage.class);
        List<String> records = Arrays.asList("record1", "record2");

        when(page.getCurrent()).thenReturn(1L);
        when(page.getSize()).thenReturn(10L);
        when(page.getTotal()).thenReturn(20L);
        when(page.getPages()).thenReturn(2L);
        when(page.getRecords()).thenReturn(records);

        PageResult<String> result = PageResult.of(page);

        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(20L, result.getTotal());
        assertEquals(2L, result.getPages());
        assertEquals(records, result.getRecords());
    }

    @Test
    void testEmpty() {
        PageResult<Object> result = PageResult.empty();

        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(0L, result.getTotal());
        assertEquals(0L, result.getPages());
        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void testOfWithParameters_SinglePage() {
        List<String> records = Arrays.asList("a", "b", "c");
        
        PageResult<String> result = PageResult.of(1, 10, 3, records);

        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(3L, result.getTotal());
        assertEquals(1L, result.getPages()); // 3 items, 10 per page = 1 page
        assertEquals(records, result.getRecords());
    }

    @Test
    void testOfWithParameters_MultiplePages() {
        List<String> records = Arrays.asList("a", "b", "c", "d", "e");
        
        PageResult<String> result = PageResult.of(1, 10, 25, records);

        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(25L, result.getTotal());
        assertEquals(3L, result.getPages()); // 25 items, 10 per page = 3 pages
        assertEquals(records, result.getRecords());
    }

    @Test
    void testOfWithParameters_ExactPages() {
        List<String> records = Arrays.asList("a", "b");
        
        PageResult<String> result = PageResult.of(1, 5, 20, records);

        assertEquals(1L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(20L, result.getTotal());
        assertEquals(4L, result.getPages()); // 20 items, 5 per page = 4 pages
        assertEquals(records, result.getRecords());
    }

    @Test
    void testOfWithParameters_ZeroTotal() {
        List<String> records = Collections.emptyList();
        
        PageResult<String> result = PageResult.of(1, 10, 0, records);

        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(0L, result.getTotal());
        assertEquals(0L, result.getPages());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void testEquals() {
        List<String> records = Arrays.asList("a", "b");
        
        PageResult<String> result1 = PageResult.of(1, 10, 2, records);
        PageResult<String> result2 = PageResult.of(1, 10, 2, records);

        assertEquals(result1, result2);
    }

    @Test
    void testHashCode() {
        List<String> records = Arrays.asList("a", "b");
        
        PageResult<String> result1 = PageResult.of(1, 10, 2, records);
        PageResult<String> result2 = PageResult.of(1, 10, 2, records);

        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testToString() {
        PageResult<String> result = PageResult.of(1, 10, 2, Arrays.asList("a", "b"));

        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("PageResult"));
    }
}
