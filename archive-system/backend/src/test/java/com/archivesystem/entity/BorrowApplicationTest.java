package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BorrowApplicationTest {

    @Test
    void testBuilder() {
        LocalDateTime applyTime = LocalDateTime.of(2026, 1, 15, 10, 30);
        LocalDate expectedReturnDate = LocalDate.of(2026, 2, 15);

        BorrowApplication application = BorrowApplication.builder()
                .applicationNo("BRW-2026-001")
                .archiveId(100L)
                .archiveNo("ARCH-001")
                .archiveTitle("测试档案")
                .applicantId(1L)
                .applicantName("张三")
                .applicantDept("研发部")
                .applicantPhone("13800138000")
                .applyTime(applyTime)
                .borrowPurpose("查阅案件资料")
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .expectedReturnDate(expectedReturnDate)
                .actualReturnDate(null)
                .borrowTime(null)
                .renewCount(0)
                .status(BorrowApplication.STATUS_PENDING)
                .approverId(null)
                .approverName(null)
                .approveTime(null)
                .approveRemarks(null)
                .rejectReason(null)
                .returnRemarks(null)
                .downloadCount(0)
                .viewCount(0)
                .lastAccessAt(null)
                .remarks("备注")
                .build();

        assertEquals("BRW-2026-001", application.getApplicationNo());
        assertEquals(100L, application.getArchiveId());
        assertEquals("ARCH-001", application.getArchiveNo());
        assertEquals("测试档案", application.getArchiveTitle());
        assertEquals(1L, application.getApplicantId());
        assertEquals("张三", application.getApplicantName());
        assertEquals("研发部", application.getApplicantDept());
        assertEquals("13800138000", application.getApplicantPhone());
        assertEquals(applyTime, application.getApplyTime());
        assertEquals("查阅案件资料", application.getBorrowPurpose());
        assertEquals(BorrowApplication.TYPE_ONLINE, application.getBorrowType());
        assertEquals(expectedReturnDate, application.getExpectedReturnDate());
        assertEquals(0, application.getRenewCount());
        assertEquals(BorrowApplication.STATUS_PENDING, application.getStatus());
        assertEquals(0, application.getDownloadCount());
        assertEquals(0, application.getViewCount());
        assertEquals("备注", application.getRemarks());
    }

    @Test
    void testDefaultValues() {
        BorrowApplication application = BorrowApplication.builder().build();

        assertEquals(BorrowApplication.TYPE_ONLINE, application.getBorrowType());
        assertEquals(0, application.getRenewCount());
        assertEquals(BorrowApplication.STATUS_PENDING, application.getStatus());
        assertEquals(0, application.getDownloadCount());
        assertEquals(0, application.getViewCount());
    }

    @Test
    void testNoArgsConstructor() {
        BorrowApplication application = new BorrowApplication();

        assertNull(application.getApplicationNo());
        assertNull(application.getArchiveId());
    }

    @Test
    void testStatusConstants() {
        assertEquals("PENDING", BorrowApplication.STATUS_PENDING);
        assertEquals("APPROVED", BorrowApplication.STATUS_APPROVED);
        assertEquals("REJECTED", BorrowApplication.STATUS_REJECTED);
        assertEquals("BORROWED", BorrowApplication.STATUS_BORROWED);
        assertEquals("RETURNED", BorrowApplication.STATUS_RETURNED);
        assertEquals("CANCELLED", BorrowApplication.STATUS_CANCELLED);
    }

    @Test
    void testBorrowTypeConstants() {
        assertEquals("ONLINE", BorrowApplication.TYPE_ONLINE);
        assertEquals("DOWNLOAD", BorrowApplication.TYPE_DOWNLOAD);
        assertEquals("COPY", BorrowApplication.TYPE_COPY);
    }

    @Test
    void testSettersAndGetters() {
        BorrowApplication application = new BorrowApplication();
        LocalDateTime now = LocalDateTime.now();

        application.setApplicationNo("BRW-002");
        application.setArchiveId(200L);
        application.setApplicantName("李四");
        application.setStatus(BorrowApplication.STATUS_APPROVED);
        application.setApproverId(10L);
        application.setApproverName("审批人");
        application.setApproveTime(now);
        application.setApproveRemarks("同意");
        application.setDownloadCount(5);
        application.setViewCount(10);

        assertEquals("BRW-002", application.getApplicationNo());
        assertEquals(200L, application.getArchiveId());
        assertEquals("李四", application.getApplicantName());
        assertEquals(BorrowApplication.STATUS_APPROVED, application.getStatus());
        assertEquals(10L, application.getApproverId());
        assertEquals("审批人", application.getApproverName());
        assertEquals(now, application.getApproveTime());
        assertEquals("同意", application.getApproveRemarks());
        assertEquals(5, application.getDownloadCount());
        assertEquals(10, application.getViewCount());
    }

    @Test
    void testApprovalFlow() {
        BorrowApplication application = BorrowApplication.builder()
                .applicationNo("BRW-003")
                .status(BorrowApplication.STATUS_PENDING)
                .build();

        // 模拟审批流程
        assertEquals(BorrowApplication.STATUS_PENDING, application.getStatus());

        application.setStatus(BorrowApplication.STATUS_APPROVED);
        application.setApproverId(1L);
        application.setApproverName("管理员");
        application.setApproveTime(LocalDateTime.now());

        assertEquals(BorrowApplication.STATUS_APPROVED, application.getStatus());
        assertNotNull(application.getApproveTime());
    }

    @Test
    void testRejection() {
        BorrowApplication application = BorrowApplication.builder()
                .applicationNo("BRW-004")
                .status(BorrowApplication.STATUS_PENDING)
                .build();

        application.setStatus(BorrowApplication.STATUS_REJECTED);
        application.setRejectReason("档案正在维护中");

        assertEquals(BorrowApplication.STATUS_REJECTED, application.getStatus());
        assertEquals("档案正在维护中", application.getRejectReason());
    }

    @Test
    void testToString() {
        BorrowApplication application = BorrowApplication.builder()
                .applicationNo("BRW-001")
                .build();

        String str = application.toString();
        assertNotNull(str);
        assertTrue(str.contains("BorrowApplication"));
    }
}
