package com.archivesystem.dto.archive;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class ArchiveReceiveResponseTest {

    @Test
    void testBuilder() {
        LocalDateTime receivedAt = LocalDateTime.of(2026, 1, 15, 10, 30);

        ArchiveReceiveResponse response = ArchiveReceiveResponse.builder()
                .archiveId(1L)
                .archiveNo("ARCH-2026-001")
                .status("RECEIVED")
                .receivedAt(receivedAt)
                .fileCount(5)
                .message("档案接收成功")
                .build();

        assertEquals(1L, response.getArchiveId());
        assertEquals("ARCH-2026-001", response.getArchiveNo());
        assertEquals("RECEIVED", response.getStatus());
        assertEquals(receivedAt, response.getReceivedAt());
        assertEquals(5, response.getFileCount());
        assertEquals("档案接收成功", response.getMessage());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveReceiveResponse response = new ArchiveReceiveResponse();

        assertNull(response.getArchiveId());
        assertNull(response.getArchiveNo());
        assertNull(response.getStatus());
        assertNull(response.getReceivedAt());
        assertNull(response.getFileCount());
        assertNull(response.getMessage());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();

        ArchiveReceiveResponse response = new ArchiveReceiveResponse(
                2L, "ARCH-002", "PROCESSING", now, 3, "处理中"
        );

        assertEquals(2L, response.getArchiveId());
        assertEquals("ARCH-002", response.getArchiveNo());
        assertEquals("PROCESSING", response.getStatus());
        assertEquals(now, response.getReceivedAt());
        assertEquals(3, response.getFileCount());
        assertEquals("处理中", response.getMessage());
    }

    @Test
    void testSettersAndGetters() {
        ArchiveReceiveResponse response = new ArchiveReceiveResponse();
        LocalDateTime receivedAt = LocalDateTime.of(2026, 2, 1, 14, 0);

        response.setArchiveId(3L);
        response.setArchiveNo("ARCH-003");
        response.setStatus("COMPLETED");
        response.setReceivedAt(receivedAt);
        response.setFileCount(10);
        response.setMessage("处理完成");

        assertEquals(3L, response.getArchiveId());
        assertEquals("ARCH-003", response.getArchiveNo());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(receivedAt, response.getReceivedAt());
        assertEquals(10, response.getFileCount());
        assertEquals("处理完成", response.getMessage());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();

        ArchiveReceiveResponse response1 = ArchiveReceiveResponse.builder()
                .archiveId(1L)
                .archiveNo("ARCH-001")
                .receivedAt(now)
                .build();

        ArchiveReceiveResponse response2 = ArchiveReceiveResponse.builder()
                .archiveId(1L)
                .archiveNo("ARCH-001")
                .receivedAt(now)
                .build();

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void testNotEquals() {
        ArchiveReceiveResponse response1 = ArchiveReceiveResponse.builder()
                .archiveId(1L)
                .build();

        ArchiveReceiveResponse response2 = ArchiveReceiveResponse.builder()
                .archiveId(2L)
                .build();

        assertNotEquals(response1, response2);
    }

    @Test
    void testToString() {
        ArchiveReceiveResponse response = ArchiveReceiveResponse.builder()
                .archiveId(1L)
                .message("测试")
                .build();

        String str = response.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveReceiveResponse"));
    }
}
