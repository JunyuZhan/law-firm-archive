package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AppraisalRecordTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        AppraisalRecord record = AppraisalRecord.builder()
                .archiveId(100L)
                .appraisalType(AppraisalRecord.TYPE_RETENTION)
                .originalValue("Y10")
                .newValue("PERMANENT")
                .appraisalReason("档案具有永久保存价值")
                .appraisalOpinion("建议延长保管期限")
                .status(AppraisalRecord.STATUS_PENDING)
                .appraiserId(1L)
                .appraiserName("张三")
                .appraisedAt(now)
                .approverId(2L)
                .approverName("李四")
                .approvedAt(now)
                .approvalComment("同意")
                .build();

        assertEquals(100L, record.getArchiveId());
        assertEquals(AppraisalRecord.TYPE_RETENTION, record.getAppraisalType());
        assertEquals("Y10", record.getOriginalValue());
        assertEquals("PERMANENT", record.getNewValue());
        assertEquals("档案具有永久保存价值", record.getAppraisalReason());
        assertEquals("建议延长保管期限", record.getAppraisalOpinion());
        assertEquals(AppraisalRecord.STATUS_PENDING, record.getStatus());
        assertEquals(1L, record.getAppraiserId());
        assertEquals("张三", record.getAppraiserName());
        assertEquals(2L, record.getApproverId());
        assertEquals("李四", record.getApproverName());
        assertEquals("同意", record.getApprovalComment());
    }

    @Test
    void testDefaultValues() {
        AppraisalRecord record = AppraisalRecord.builder().build();

        assertEquals(AppraisalRecord.STATUS_PENDING, record.getStatus());
    }

    @Test
    void testNoArgsConstructor() {
        AppraisalRecord record = new AppraisalRecord();

        assertNull(record.getArchiveId());
        assertNull(record.getAppraisalType());
    }

    @Test
    void testTypeConstants() {
        assertEquals("VALUE", AppraisalRecord.TYPE_VALUE);
        assertEquals("SECURITY", AppraisalRecord.TYPE_SECURITY);
        assertEquals("OPEN", AppraisalRecord.TYPE_OPEN);
        assertEquals("RETENTION", AppraisalRecord.TYPE_RETENTION);
    }

    @Test
    void testStatusConstants() {
        assertEquals("PENDING", AppraisalRecord.STATUS_PENDING);
        assertEquals("APPROVED", AppraisalRecord.STATUS_APPROVED);
        assertEquals("REJECTED", AppraisalRecord.STATUS_REJECTED);
    }

    @Test
    void testSettersAndGetters() {
        AppraisalRecord record = new AppraisalRecord();

        record.setArchiveId(200L);
        record.setAppraisalType(AppraisalRecord.TYPE_SECURITY);
        record.setOriginalValue("CONFIDENTIAL");
        record.setNewValue("PUBLIC");
        record.setAppraisalReason("解密期满");
        record.setStatus(AppraisalRecord.STATUS_APPROVED);

        assertEquals(200L, record.getArchiveId());
        assertEquals(AppraisalRecord.TYPE_SECURITY, record.getAppraisalType());
        assertEquals("CONFIDENTIAL", record.getOriginalValue());
        assertEquals("PUBLIC", record.getNewValue());
        assertEquals("解密期满", record.getAppraisalReason());
        assertEquals(AppraisalRecord.STATUS_APPROVED, record.getStatus());
    }

    @Test
    void testValueAppraisal() {
        AppraisalRecord record = AppraisalRecord.builder()
                .appraisalType(AppraisalRecord.TYPE_VALUE)
                .originalValue("普通")
                .newValue("珍贵")
                .appraisalReason("档案具有重要历史价值")
                .build();

        assertEquals(AppraisalRecord.TYPE_VALUE, record.getAppraisalType());
    }

    @Test
    void testOpenAppraisal() {
        AppraisalRecord record = AppraisalRecord.builder()
                .appraisalType(AppraisalRecord.TYPE_OPEN)
                .originalValue("不公开")
                .newValue("公开")
                .appraisalReason("不再涉及敏感信息")
                .build();

        assertEquals(AppraisalRecord.TYPE_OPEN, record.getAppraisalType());
    }

    @Test
    void testWorkflow_Reject() {
        AppraisalRecord record = AppraisalRecord.builder()
                .status(AppraisalRecord.STATUS_PENDING)
                .build();

        // 拒绝
        record.setStatus(AppraisalRecord.STATUS_REJECTED);
        record.setApprovalComment("证据不足");

        assertEquals(AppraisalRecord.STATUS_REJECTED, record.getStatus());
        assertEquals("证据不足", record.getApprovalComment());
    }

    @Test
    void testToString() {
        AppraisalRecord record = AppraisalRecord.builder()
                .archiveId(1L)
                .appraisalType("TEST")
                .build();

        String str = record.toString();
        assertNotNull(str);
        assertTrue(str.contains("AppraisalRecord"));
    }
}
