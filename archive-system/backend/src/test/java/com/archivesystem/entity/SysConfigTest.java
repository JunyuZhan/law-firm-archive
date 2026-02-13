package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SysConfigTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        SysConfig config = SysConfig.builder()
                .id(1L)
                .configKey("archive.no.prefix")
                .configValue("ARCH")
                .configType(SysConfig.TYPE_STRING)
                .configGroup(SysConfig.GROUP_ARCHIVE_NO)
                .description("档案号前缀")
                .editable(true)
                .sortOrder(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(1L)
                .updatedBy(1L)
                .build();

        assertEquals(1L, config.getId());
        assertEquals("archive.no.prefix", config.getConfigKey());
        assertEquals("ARCH", config.getConfigValue());
        assertEquals(SysConfig.TYPE_STRING, config.getConfigType());
        assertEquals(SysConfig.GROUP_ARCHIVE_NO, config.getConfigGroup());
        assertEquals("档案号前缀", config.getDescription());
        assertTrue(config.getEditable());
        assertEquals(1, config.getSortOrder());
        assertEquals(now, config.getCreatedAt());
        assertEquals(now, config.getUpdatedAt());
        assertEquals(1L, config.getCreatedBy());
        assertEquals(1L, config.getUpdatedBy());
    }

    @Test
    void testDefaultValues() {
        SysConfig config = SysConfig.builder().build();

        assertTrue(config.getEditable());
        assertEquals(0, config.getSortOrder());
    }

    @Test
    void testNoArgsConstructor() {
        SysConfig config = new SysConfig();

        assertNull(config.getId());
        assertNull(config.getConfigKey());
    }

    @Test
    void testTypeConstants() {
        assertEquals("STRING", SysConfig.TYPE_STRING);
        assertEquals("NUMBER", SysConfig.TYPE_NUMBER);
        assertEquals("BOOLEAN", SysConfig.TYPE_BOOLEAN);
        assertEquals("JSON", SysConfig.TYPE_JSON);
    }

    @Test
    void testGroupConstants() {
        assertEquals("ARCHIVE_NO", SysConfig.GROUP_ARCHIVE_NO);
        assertEquals("RETENTION", SysConfig.GROUP_RETENTION);
        assertEquals("SYSTEM", SysConfig.GROUP_SYSTEM);
    }

    @Test
    void testSettersAndGetters() {
        SysConfig config = new SysConfig();

        config.setId(2L);
        config.setConfigKey("retention.default");
        config.setConfigValue("PERMANENT");
        config.setConfigType(SysConfig.TYPE_STRING);
        config.setConfigGroup(SysConfig.GROUP_RETENTION);
        config.setDescription("默认保管期限");
        config.setEditable(false);
        config.setSortOrder(5);

        assertEquals(2L, config.getId());
        assertEquals("retention.default", config.getConfigKey());
        assertEquals("PERMANENT", config.getConfigValue());
        assertEquals(SysConfig.TYPE_STRING, config.getConfigType());
        assertEquals(SysConfig.GROUP_RETENTION, config.getConfigGroup());
        assertEquals("默认保管期限", config.getDescription());
        assertFalse(config.getEditable());
        assertEquals(5, config.getSortOrder());
    }

    @Test
    void testEqualsAndHashCode() {
        SysConfig config1 = new SysConfig();
        config1.setId(1L);
        config1.setConfigKey("test.key");

        SysConfig config2 = new SysConfig();
        config2.setId(1L);
        config2.setConfigKey("test.key");

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testToString() {
        SysConfig config = SysConfig.builder()
                .id(1L)
                .configKey("test")
                .build();

        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("SysConfig"));
    }

    @Test
    void testBooleanConfig() {
        SysConfig config = SysConfig.builder()
                .configKey("feature.enabled")
                .configValue("true")
                .configType(SysConfig.TYPE_BOOLEAN)
                .build();

        assertEquals(SysConfig.TYPE_BOOLEAN, config.getConfigType());
        assertEquals("true", config.getConfigValue());
    }

    @Test
    void testNumberConfig() {
        SysConfig config = SysConfig.builder()
                .configKey("max.upload.size")
                .configValue("104857600")
                .configType(SysConfig.TYPE_NUMBER)
                .build();

        assertEquals(SysConfig.TYPE_NUMBER, config.getConfigType());
        assertEquals("104857600", config.getConfigValue());
    }
}
