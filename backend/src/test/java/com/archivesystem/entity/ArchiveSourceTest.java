package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveSourceTest {

    @Test
    void testBuilder() {
        LocalDateTime lastSyncAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        Map<String, Object> extraConfig = new HashMap<>();
        extraConfig.put("timeout", 30000);

        ArchiveSource source = ArchiveSource.builder()
                .sourceCode("LAW-001")
                .sourceName("律所系统")
                .sourceType(ArchiveSource.TYPE_LAW_FIRM)
                .description("对接律所案件管理系统")
                .apiUrl("https://api.lawfirm.com/archive")
                .apiKey("key-abc123")
                .apiSecret("secret-xyz789")
                .authType(ArchiveSource.AUTH_API_KEY)
                .extraConfig(extraConfig)
                .enabled(true)
                .lastSyncAt(lastSyncAt)
                .lastSyncResult("SUCCESS")
                .lastSyncMessage("同步成功")
                .build();

        assertEquals("LAW-001", source.getSourceCode());
        assertEquals("律所系统", source.getSourceName());
        assertEquals(ArchiveSource.TYPE_LAW_FIRM, source.getSourceType());
        assertEquals("对接律所案件管理系统", source.getDescription());
        assertEquals("https://api.lawfirm.com/archive", source.getApiUrl());
        assertEquals("key-abc123", source.getApiKey());
        assertEquals("secret-xyz789", source.getApiSecret());
        assertEquals(ArchiveSource.AUTH_API_KEY, source.getAuthType());
        assertEquals(extraConfig, source.getExtraConfig());
        assertTrue(source.getEnabled());
        assertEquals(lastSyncAt, source.getLastSyncAt());
        assertEquals("SUCCESS", source.getLastSyncResult());
        assertEquals("同步成功", source.getLastSyncMessage());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveSource source = new ArchiveSource();

        assertNull(source.getSourceCode());
        assertNull(source.getSourceName());
    }

    @Test
    void testTypeConstants() {
        assertEquals("LAW_FIRM", ArchiveSource.TYPE_LAW_FIRM);
        assertEquals("COURT", ArchiveSource.TYPE_COURT);
        assertEquals("ENTERPRISE", ArchiveSource.TYPE_ENTERPRISE);
        assertEquals("OTHER", ArchiveSource.TYPE_OTHER);
    }

    @Test
    void testAuthTypeConstants() {
        assertEquals("API_KEY", ArchiveSource.AUTH_API_KEY);
        assertEquals("BEARER_TOKEN", ArchiveSource.AUTH_BEARER_TOKEN);
        assertEquals("BASIC", ArchiveSource.AUTH_BASIC);
    }

    @Test
    void testSettersAndGetters() {
        ArchiveSource source = new ArchiveSource();

        source.setSourceCode("COURT-001");
        source.setSourceName("法院系统");
        source.setSourceType(ArchiveSource.TYPE_COURT);
        source.setApiUrl("https://api.court.gov/archive");
        source.setApiKey("court-key");
        source.setApiSecret("court-secret");
        source.setAuthType(ArchiveSource.AUTH_BEARER_TOKEN);
        source.setEnabled(false);

        assertEquals("COURT-001", source.getSourceCode());
        assertEquals("法院系统", source.getSourceName());
        assertEquals(ArchiveSource.TYPE_COURT, source.getSourceType());
        assertEquals("https://api.court.gov/archive", source.getApiUrl());
        assertEquals("court-key", source.getApiKey());
        assertEquals("court-secret", source.getApiSecret());
        assertEquals(ArchiveSource.AUTH_BEARER_TOKEN, source.getAuthType());
        assertFalse(source.getEnabled());
    }

    @Test
    void testSyncStatus() {
        ArchiveSource source = ArchiveSource.builder()
                .sourceCode("TEST")
                .build();

        LocalDateTime syncTime = LocalDateTime.now();
        source.setLastSyncAt(syncTime);
        source.setLastSyncResult("FAILED");
        source.setLastSyncMessage("连接超时");

        assertEquals(syncTime, source.getLastSyncAt());
        assertEquals("FAILED", source.getLastSyncResult());
        assertEquals("连接超时", source.getLastSyncMessage());
    }

    @Test
    void testExtraConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("retryCount", 3);
        config.put("timeout", 5000);

        ArchiveSource source = ArchiveSource.builder()
                .extraConfig(config)
                .build();

        assertNotNull(source.getExtraConfig());
        assertEquals(3, source.getExtraConfig().get("retryCount"));
        assertEquals(5000, source.getExtraConfig().get("timeout"));
    }

    @Test
    void testEnterpriseSource() {
        ArchiveSource source = ArchiveSource.builder()
                .sourceCode("ENT-001")
                .sourceName("企业系统")
                .sourceType(ArchiveSource.TYPE_ENTERPRISE)
                .authType(ArchiveSource.AUTH_BASIC)
                .build();

        assertEquals(ArchiveSource.TYPE_ENTERPRISE, source.getSourceType());
        assertEquals(ArchiveSource.AUTH_BASIC, source.getAuthType());
    }

    @Test
    void testToString() {
        ArchiveSource source = ArchiveSource.builder()
                .sourceCode("TEST")
                .build();

        String str = source.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveSource"));
    }
}
