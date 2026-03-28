package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveMetadataTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        ArchiveMetadata metadata = ArchiveMetadata.builder()
                .id(1L)
                .archiveId(100L)
                .fieldCode("CUSTOM_FIELD_1")
                .fieldName("自定义字段1")
                .fieldValue("自定义值")
                .fieldType(ArchiveMetadata.TYPE_TEXT)
                .sortOrder(1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, metadata.getId());
        assertEquals(100L, metadata.getArchiveId());
        assertEquals("CUSTOM_FIELD_1", metadata.getFieldCode());
        assertEquals("自定义字段1", metadata.getFieldName());
        assertEquals("自定义值", metadata.getFieldValue());
        assertEquals(ArchiveMetadata.TYPE_TEXT, metadata.getFieldType());
        assertEquals(1, metadata.getSortOrder());
        assertEquals(now, metadata.getCreatedAt());
        assertEquals(now, metadata.getUpdatedAt());
    }

    @Test
    void testDefaultValues() {
        ArchiveMetadata metadata = ArchiveMetadata.builder().build();

        assertEquals(ArchiveMetadata.TYPE_TEXT, metadata.getFieldType());
        assertEquals(0, metadata.getSortOrder());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveMetadata metadata = new ArchiveMetadata();

        assertNull(metadata.getId());
        assertNull(metadata.getArchiveId());
    }

    @Test
    void testTypeConstants() {
        assertEquals("TEXT", ArchiveMetadata.TYPE_TEXT);
        assertEquals("NUMBER", ArchiveMetadata.TYPE_NUMBER);
        assertEquals("DATE", ArchiveMetadata.TYPE_DATE);
        assertEquals("BOOLEAN", ArchiveMetadata.TYPE_BOOLEAN);
    }

    @Test
    void testSettersAndGetters() {
        ArchiveMetadata metadata = new ArchiveMetadata();

        metadata.setId(2L);
        metadata.setArchiveId(200L);
        metadata.setFieldCode("AMOUNT");
        metadata.setFieldName("金额");
        metadata.setFieldValue("10000");
        metadata.setFieldType(ArchiveMetadata.TYPE_NUMBER);
        metadata.setSortOrder(5);

        assertEquals(2L, metadata.getId());
        assertEquals(200L, metadata.getArchiveId());
        assertEquals("AMOUNT", metadata.getFieldCode());
        assertEquals("金额", metadata.getFieldName());
        assertEquals("10000", metadata.getFieldValue());
        assertEquals(ArchiveMetadata.TYPE_NUMBER, metadata.getFieldType());
        assertEquals(5, metadata.getSortOrder());
    }

    @Test
    void testTextMetadata() {
        ArchiveMetadata metadata = ArchiveMetadata.builder()
                .fieldCode("REMARK")
                .fieldName("备注")
                .fieldValue("这是一条备注")
                .fieldType(ArchiveMetadata.TYPE_TEXT)
                .build();

        assertEquals(ArchiveMetadata.TYPE_TEXT, metadata.getFieldType());
    }

    @Test
    void testDateMetadata() {
        ArchiveMetadata metadata = ArchiveMetadata.builder()
                .fieldCode("SIGN_DATE")
                .fieldName("签署日期")
                .fieldValue("2026-01-15")
                .fieldType(ArchiveMetadata.TYPE_DATE)
                .build();

        assertEquals(ArchiveMetadata.TYPE_DATE, metadata.getFieldType());
        assertEquals("2026-01-15", metadata.getFieldValue());
    }

    @Test
    void testBooleanMetadata() {
        ArchiveMetadata metadata = ArchiveMetadata.builder()
                .fieldCode("IS_IMPORTANT")
                .fieldName("是否重要")
                .fieldValue("true")
                .fieldType(ArchiveMetadata.TYPE_BOOLEAN)
                .build();

        assertEquals(ArchiveMetadata.TYPE_BOOLEAN, metadata.getFieldType());
        assertEquals("true", metadata.getFieldValue());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveMetadata metadata1 = new ArchiveMetadata();
        metadata1.setId(1L);
        metadata1.setFieldCode("TEST");

        ArchiveMetadata metadata2 = new ArchiveMetadata();
        metadata2.setId(1L);
        metadata2.setFieldCode("TEST");

        assertEquals(metadata1, metadata2);
        assertEquals(metadata1.hashCode(), metadata2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveMetadata metadata = ArchiveMetadata.builder()
                .id(1L)
                .fieldCode("TEST")
                .build();

        String str = metadata.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveMetadata"));
    }
}
