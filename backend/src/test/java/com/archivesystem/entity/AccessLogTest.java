package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class AccessLogTest {

    @Test
    void testBuilder() {
        LocalDateTime accessedAt = LocalDateTime.of(2026, 1, 15, 10, 30);

        AccessLog log = AccessLog.builder()
                .id(1L)
                .archiveId(100L)
                .fileId(200L)
                .accessType(AccessLog.TYPE_VIEW)
                .accessIp("192.168.1.100")
                .accessUa("Mozilla/5.0")
                .userId(1L)
                .userName("张三")
                .accessedAt(accessedAt)
                .searchKeyword("合同")
                .searchResultCount(10)
                .duration(150L)
                .extraData("{\"key\":\"value\"}")
                .build();

        assertEquals(1L, log.getId());
        assertEquals(100L, log.getArchiveId());
        assertEquals(200L, log.getFileId());
        assertEquals(AccessLog.TYPE_VIEW, log.getAccessType());
        assertEquals("192.168.1.100", log.getAccessIp());
        assertEquals("Mozilla/5.0", log.getAccessUa());
        assertEquals(1L, log.getUserId());
        assertEquals("张三", log.getUserName());
        assertEquals(accessedAt, log.getAccessedAt());
        assertEquals("合同", log.getSearchKeyword());
        assertEquals(10, log.getSearchResultCount());
        assertEquals(150L, log.getDuration());
        assertEquals("{\"key\":\"value\"}", log.getExtraData());
    }

    @Test
    void testDefaultValues() {
        AccessLog log = AccessLog.builder().build();

        assertNotNull(log.getAccessedAt());
    }

    @Test
    void testNoArgsConstructor() {
        AccessLog log = new AccessLog();

        assertNull(log.getId());
        assertNull(log.getArchiveId());
    }

    @Test
    void testAccessTypeConstants() {
        assertEquals("VIEW", AccessLog.TYPE_VIEW);
        assertEquals("DOWNLOAD", AccessLog.TYPE_DOWNLOAD);
        assertEquals("PRINT", AccessLog.TYPE_PRINT);
        assertEquals("PREVIEW", AccessLog.TYPE_PREVIEW);
        assertEquals("SEARCH", AccessLog.TYPE_SEARCH);
    }

    @Test
    void testSettersAndGetters() {
        AccessLog log = new AccessLog();

        log.setId(2L);
        log.setArchiveId(300L);
        log.setFileId(400L);
        log.setAccessType(AccessLog.TYPE_DOWNLOAD);
        log.setAccessIp("10.0.0.1");
        log.setUserId(2L);
        log.setUserName("李四");
        log.setDuration(200L);

        assertEquals(2L, log.getId());
        assertEquals(300L, log.getArchiveId());
        assertEquals(400L, log.getFileId());
        assertEquals(AccessLog.TYPE_DOWNLOAD, log.getAccessType());
        assertEquals("10.0.0.1", log.getAccessIp());
        assertEquals(2L, log.getUserId());
        assertEquals("李四", log.getUserName());
        assertEquals(200L, log.getDuration());
    }

    @Test
    void testSearchLog() {
        AccessLog log = AccessLog.builder()
                .accessType(AccessLog.TYPE_SEARCH)
                .searchKeyword("劳动合同")
                .searchResultCount(25)
                .duration(50L)
                .build();

        assertEquals(AccessLog.TYPE_SEARCH, log.getAccessType());
        assertEquals("劳动合同", log.getSearchKeyword());
        assertEquals(25, log.getSearchResultCount());
    }

    @Test
    void testToString() {
        AccessLog log = AccessLog.builder()
                .id(1L)
                .accessType(AccessLog.TYPE_VIEW)
                .build();

        String str = log.toString();
        assertNotNull(str);
        assertTrue(str.contains("AccessLog"));
    }
}
