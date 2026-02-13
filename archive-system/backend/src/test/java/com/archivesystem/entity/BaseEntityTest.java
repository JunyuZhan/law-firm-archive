package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    // 创建一个具体的BaseEntity实现用于测试
    private static class TestEntity extends BaseEntity {
        // 继承所有BaseEntity的字段
    }

    @Test
    void testSettersAndGetters() {
        TestEntity entity = new TestEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setId(1L);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(100L);
        entity.setUpdatedBy(200L);
        entity.setDeleted(false);

        assertEquals(1L, entity.getId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
        assertEquals(100L, entity.getCreatedBy());
        assertEquals(200L, entity.getUpdatedBy());
        assertFalse(entity.getDeleted());
    }

    @Test
    void testDefaultDeletedValue() {
        TestEntity entity = new TestEntity();

        assertFalse(entity.getDeleted());
    }

    @Test
    void testNullValues() {
        TestEntity entity = new TestEntity();

        assertNull(entity.getId());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
        assertNull(entity.getCreatedBy());
        assertNull(entity.getUpdatedBy());
    }

    @Test
    void testSetDeleted() {
        TestEntity entity = new TestEntity();

        entity.setDeleted(true);
        assertTrue(entity.getDeleted());

        entity.setDeleted(false);
        assertFalse(entity.getDeleted());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        
        TestEntity entity1 = new TestEntity();
        entity1.setId(1L);
        entity1.setCreatedAt(now);
        entity1.setDeleted(false);

        TestEntity entity2 = new TestEntity();
        entity2.setId(1L);
        entity2.setCreatedAt(now);
        entity2.setDeleted(false);

        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testToString() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);

        String str = entity.toString();
        assertNotNull(str);
    }

    @Test
    void testAuditFields() {
        TestEntity entity = new TestEntity();
        LocalDateTime createTime = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime updateTime = LocalDateTime.of(2026, 1, 15, 10, 30);

        entity.setCreatedAt(createTime);
        entity.setUpdatedAt(updateTime);
        entity.setCreatedBy(1L);
        entity.setUpdatedBy(2L);

        assertEquals(createTime, entity.getCreatedAt());
        assertEquals(updateTime, entity.getUpdatedAt());
        assertEquals(1L, entity.getCreatedBy());
        assertEquals(2L, entity.getUpdatedBy());
    }
}
