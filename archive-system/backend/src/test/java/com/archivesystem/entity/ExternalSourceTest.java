package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExternalSourceTest {

    @Test
    void testBuilder() {
        LocalDateTime lastSyncAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        Map<String, Object> extraConfig = new HashMap<>();
        extraConfig.put("timeout", 30000);

        ExternalSource source = ExternalSource.builder()
                .sourceCode("LAW-001")
                .sourceName("律所系统")
                .sourceType(ExternalSource.TYPE_LAW_FIRM)
                .description("对接律所案件管理系统")
                .apiUrl("https://api.lawfirm.com/archive")
                .apiKey("sk-abc123")
                .authType(ExternalSource.AUTH_API_KEY)
                .extraConfig(extraConfig)
                .enabled(true)
                .lastSyncAt(lastSyncAt)
                .lastSyncStatus("SUCCESS")
                .lastSyncMessage("同步成功")
                .build();

        assertEquals("LAW-001", source.getSourceCode());
        assertEquals("律所系统", source.getSourceName());
        assertEquals(ExternalSource.TYPE_LAW_FIRM, source.getSourceType());
        assertEquals("对接律所案件管理系统", source.getDescription());
        assertEquals("https://api.lawfirm.com/archive", source.getApiUrl());
        assertEquals("sk-abc123", source.getApiKey());
        assertEquals(ExternalSource.AUTH_API_KEY, source.getAuthType());
        assertEquals(extraConfig, source.getExtraConfig());
        assertTrue(source.getEnabled());
        assertEquals(lastSyncAt, source.getLastSyncAt());
        assertEquals("SUCCESS", source.getLastSyncStatus());
        assertEquals("同步成功", source.getLastSyncMessage());
    }

    @Test
    void testDefaultValues() {
        ExternalSource source = ExternalSource.builder().build();

        assertFalse(source.getEnabled());
    }

    @Test
    void testNoArgsConstructor() {
        ExternalSource source = new ExternalSource();

        assertNull(source.getSourceCode());
        assertNull(source.getSourceName());
    }

    @Test
    void testTypeConstants() {
        assertEquals("LAW_FIRM", ExternalSource.TYPE_LAW_FIRM);
        assertEquals("COURT", ExternalSource.TYPE_COURT);
        assertEquals("ENTERPRISE", ExternalSource.TYPE_ENTERPRISE);
        assertEquals("OTHER", ExternalSource.TYPE_OTHER);
    }

    @Test
    void testAuthTypeConstants() {
        assertEquals("API_KEY", ExternalSource.AUTH_API_KEY);
        assertEquals("OAUTH2", ExternalSource.AUTH_OAUTH2);
        assertEquals("BASIC", ExternalSource.AUTH_BASIC);
    }

    @Test
    void testSettersAndGetters() {
        ExternalSource source = new ExternalSource();

        source.setSourceCode("COURT-001");
        source.setSourceName("法院系统");
        source.setSourceType(ExternalSource.TYPE_COURT);
        source.setApiUrl("https://api.court.gov/archive");
        source.setApiKey("court-key-123");
        source.setAuthType(ExternalSource.AUTH_OAUTH2);
        source.setEnabled(true);

        assertEquals("COURT-001", source.getSourceCode());
        assertEquals("法院系统", source.getSourceName());
        assertEquals(ExternalSource.TYPE_COURT, source.getSourceType());
        assertEquals("https://api.court.gov/archive", source.getApiUrl());
        assertEquals("court-key-123", source.getApiKey());
        assertEquals(ExternalSource.AUTH_OAUTH2, source.getAuthType());
        assertTrue(source.getEnabled());
    }

    @Test
    void testSyncStatus() {
        ExternalSource source = ExternalSource.builder()
                .sourceCode("TEST")
                .build();

        LocalDateTime syncTime = LocalDateTime.now();
        source.setLastSyncAt(syncTime);
        source.setLastSyncStatus("FAILED");
        source.setLastSyncMessage("连接超时");

        assertEquals(syncTime, source.getLastSyncAt());
        assertEquals("FAILED", source.getLastSyncStatus());
        assertEquals("连接超时", source.getLastSyncMessage());
    }

    @Test
    void testExtraConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("retryCount", 3);
        config.put("timeout", 5000);

        ExternalSource source = ExternalSource.builder()
                .extraConfig(config)
                .build();

        assertNotNull(source.getExtraConfig());
        assertEquals(3, source.getExtraConfig().get("retryCount"));
        assertEquals(5000, source.getExtraConfig().get("timeout"));
    }

    @Test
    void testToString() {
        ExternalSource source = ExternalSource.builder()
                .sourceCode("TEST")
                .build();

        String str = source.toString();
        assertNotNull(str);
        assertTrue(str.contains("ExternalSource"));
    }
}
