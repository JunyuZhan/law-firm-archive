package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DestructionRecordTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        DestructionRecord record = DestructionRecord.builder()
                .id(1L)
                .destructionBatchNo("DEST-2026-001")
                .archiveId(100L)
                .destructionReason("保管期限届满")
                .destructionMethod(DestructionRecord.METHOD_LOGICAL)
                .status(DestructionRecord.STATUS_PENDING)
                .proposerId(1L)
                .proposerName("张三")
                .proposedAt(now)
                .approverId(2L)
                .approverName("李四")
                .approvedAt(now)
                .approvalComment("同意销毁")
                .executorId(3L)
                .executorName("王五")
                .executedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, record.getId());
        assertEquals("DEST-2026-001", record.getDestructionBatchNo());
        assertEquals(100L, record.getArchiveId());
        assertEquals("保管期限届满", record.getDestructionReason());
        assertEquals(DestructionRecord.METHOD_LOGICAL, record.getDestructionMethod());
        assertEquals(DestructionRecord.STATUS_PENDING, record.getStatus());
        assertEquals(1L, record.getProposerId());
        assertEquals("张三", record.getProposerName());
        assertEquals(2L, record.getApproverId());
        assertEquals("李四", record.getApproverName());
        assertEquals("同意销毁", record.getApprovalComment());
        assertEquals(3L, record.getExecutorId());
        assertEquals("王五", record.getExecutorName());
    }

    @Test
    void testDefaultValues() {
        DestructionRecord record = DestructionRecord.builder().build();

        assertEquals(DestructionRecord.STATUS_PENDING, record.getStatus());
    }

    @Test
    void testNoArgsConstructor() {
        DestructionRecord record = new DestructionRecord();

        assertNull(record.getId());
        assertNull(record.getDestructionBatchNo());
    }

    @Test
    void testMethodConstants() {
        assertEquals("LOGICAL", DestructionRecord.METHOD_LOGICAL);
        assertEquals("PHYSICAL", DestructionRecord.METHOD_PHYSICAL);
    }

    @Test
    void testStatusConstants() {
        assertEquals("PENDING", DestructionRecord.STATUS_PENDING);
        assertEquals("APPROVED", DestructionRecord.STATUS_APPROVED);
        assertEquals("REJECTED", DestructionRecord.STATUS_REJECTED);
        assertEquals("EXECUTED", DestructionRecord.STATUS_EXECUTED);
    }

    @Test
    void testSettersAndGetters() {
        DestructionRecord record = new DestructionRecord();

        record.setId(2L);
        record.setDestructionBatchNo("DEST-2026-002");
        record.setArchiveId(200L);
        record.setDestructionReason("无保留价值");
        record.setDestructionMethod(DestructionRecord.METHOD_PHYSICAL);
        record.setStatus(DestructionRecord.STATUS_APPROVED);

        assertEquals(2L, record.getId());
        assertEquals("DEST-2026-002", record.getDestructionBatchNo());
        assertEquals(200L, record.getArchiveId());
        assertEquals("无保留价值", record.getDestructionReason());
        assertEquals(DestructionRecord.METHOD_PHYSICAL, record.getDestructionMethod());
        assertEquals(DestructionRecord.STATUS_APPROVED, record.getStatus());
    }

    @Test
    void testWorkflow_Approve() {
        DestructionRecord record = DestructionRecord.builder()
                .destructionBatchNo("DEST-2026-003")
                .status(DestructionRecord.STATUS_PENDING)
                .build();

        // 审批通过
        record.setStatus(DestructionRecord.STATUS_APPROVED);
        record.setApproverId(1L);
        record.setApproverName("审批人");
        record.setApprovedAt(LocalDateTime.now());

        assertEquals(DestructionRecord.STATUS_APPROVED, record.getStatus());
    }

    @Test
    void testWorkflow_Execute() {
        DestructionRecord record = DestructionRecord.builder()
                .destructionBatchNo("DEST-2026-004")
                .status(DestructionRecord.STATUS_APPROVED)
                .build();

        // 执行销毁
        record.setStatus(DestructionRecord.STATUS_EXECUTED);
        record.setExecutorId(1L);
        record.setExecutorName("执行人");
        record.setExecutedAt(LocalDateTime.now());

        assertEquals(DestructionRecord.STATUS_EXECUTED, record.getStatus());
        assertNotNull(record.getExecutedAt());
    }

    @Test
    void testToString() {
        DestructionRecord record = DestructionRecord.builder()
                .id(1L)
                .destructionBatchNo("TEST")
                .build();

        String str = record.toString();
        assertNotNull(str);
        assertTrue(str.contains("DestructionRecord"));
    }
}
