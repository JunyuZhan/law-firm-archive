package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OperationLogTest {

    @Test
    void testBuilder() {
        LocalDateTime operatedAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        Map<String, Object> detail = new HashMap<>();
        detail.put("oldValue", "old");
        detail.put("newValue", "new");

        OperationLog log = OperationLog.builder()
                .id(1L)
                .archiveId(100L)
                .objectType(OperationLog.OBJ_ARCHIVE)
                .objectId("ARCH-001")
                .operationType(OperationLog.OP_CREATE)
                .operationDesc("创建档案")
                .operationDetail(detail)
                .operatorId(1L)
                .operatorName("管理员")
                .operatorIp("192.168.1.100")
                .operatorUa("Mozilla/5.0")
                .operatedAt(operatedAt)
                .build();

        assertEquals(1L, log.getId());
        assertEquals(100L, log.getArchiveId());
        assertEquals(OperationLog.OBJ_ARCHIVE, log.getObjectType());
        assertEquals("ARCH-001", log.getObjectId());
        assertEquals(OperationLog.OP_CREATE, log.getOperationType());
        assertEquals("创建档案", log.getOperationDesc());
        assertEquals(detail, log.getOperationDetail());
        assertEquals(1L, log.getOperatorId());
        assertEquals("管理员", log.getOperatorName());
        assertEquals("192.168.1.100", log.getOperatorIp());
        assertEquals("Mozilla/5.0", log.getOperatorUa());
        assertEquals(operatedAt, log.getOperatedAt());
    }

    @Test
    void testDefaultValues() {
        OperationLog log = OperationLog.builder().build();

        assertNotNull(log.getOperatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        OperationLog log = new OperationLog();

        assertNull(log.getId());
        assertNull(log.getArchiveId());
    }

    @Test
    void testObjectTypeConstants() {
        assertEquals("ARCHIVE", OperationLog.OBJ_ARCHIVE);
        assertEquals("FILE", OperationLog.OBJ_FILE);
        assertEquals("BORROW", OperationLog.OBJ_BORROW);
        assertEquals("APPRAISAL", OperationLog.OBJ_APPRAISAL);
        assertEquals("SYSTEM", OperationLog.OBJ_SYSTEM);
    }

    @Test
    void testOperationTypeConstants() {
        assertEquals("CREATE", OperationLog.OP_CREATE);
        assertEquals("UPDATE", OperationLog.OP_UPDATE);
        assertEquals("DELETE", OperationLog.OP_DELETE);
        assertEquals("VIEW", OperationLog.OP_VIEW);
        assertEquals("DOWNLOAD", OperationLog.OP_DOWNLOAD);
        assertEquals("PRINT", OperationLog.OP_PRINT);
        assertEquals("EXPORT", OperationLog.OP_EXPORT);
    }

    @Test
    void testSettersAndGetters() {
        OperationLog log = new OperationLog();

        log.setId(2L);
        log.setArchiveId(200L);
        log.setObjectType(OperationLog.OBJ_FILE);
        log.setObjectId("FILE-001");
        log.setOperationType(OperationLog.OP_DOWNLOAD);
        log.setOperationDesc("下载文件");
        log.setOperatorId(2L);
        log.setOperatorName("用户");
        log.setOperatorIp("10.0.0.1");

        assertEquals(2L, log.getId());
        assertEquals(200L, log.getArchiveId());
        assertEquals(OperationLog.OBJ_FILE, log.getObjectType());
        assertEquals("FILE-001", log.getObjectId());
        assertEquals(OperationLog.OP_DOWNLOAD, log.getOperationType());
        assertEquals("下载文件", log.getOperationDesc());
        assertEquals(2L, log.getOperatorId());
        assertEquals("用户", log.getOperatorName());
        assertEquals("10.0.0.1", log.getOperatorIp());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime fixedTime = LocalDateTime.of(2026, 1, 15, 10, 0);
        
        OperationLog log1 = new OperationLog();
        log1.setId(1L);
        log1.setOperationType(OperationLog.OP_CREATE);
        log1.setOperatedAt(fixedTime);

        OperationLog log2 = new OperationLog();
        log2.setId(1L);
        log2.setOperationType(OperationLog.OP_CREATE);
        log2.setOperatedAt(fixedTime);

        assertEquals(log1, log2);
        assertEquals(log1.hashCode(), log2.hashCode());
    }

    @Test
    void testToString() {
        OperationLog log = OperationLog.builder()
                .id(1L)
                .operationType(OperationLog.OP_VIEW)
                .build();

        String str = log.toString();
        assertNotNull(str);
        assertTrue(str.contains("OperationLog"));
    }
}
