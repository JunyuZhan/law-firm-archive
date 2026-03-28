package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveBorrowTest {

    @Test
    void testBuilder() {
        LocalDate expectedReturn = LocalDate.of(2026, 2, 28);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 15, 10, 30);

        ArchiveBorrow borrow = ArchiveBorrow.builder()
                .borrowNo("BOR-2026-001")
                .archiveId(100L)
                .borrowerId(1L)
                .borrowerName("张三")
                .borrowerDept("法务部")
                .borrowerContact("13800138000")
                .borrowReason("案件审理需要")
                .expectedReturnDate(expectedReturn)
                .status(ArchiveBorrow.STATUS_APPROVED)
                .approverId(2L)
                .approverName("李四")
                .approvedAt(approvedAt)
                .approvalComment("同意借阅")
                .returnCondition("保持完好")
                .remarks("重要文件")
                .build();

        assertEquals("BOR-2026-001", borrow.getBorrowNo());
        assertEquals(100L, borrow.getArchiveId());
        assertEquals(1L, borrow.getBorrowerId());
        assertEquals("张三", borrow.getBorrowerName());
        assertEquals("法务部", borrow.getBorrowerDept());
        assertEquals("13800138000", borrow.getBorrowerContact());
        assertEquals("案件审理需要", borrow.getBorrowReason());
        assertEquals(expectedReturn, borrow.getExpectedReturnDate());
        assertEquals(ArchiveBorrow.STATUS_APPROVED, borrow.getStatus());
        assertEquals(2L, borrow.getApproverId());
        assertEquals("李四", borrow.getApproverName());
        assertEquals(approvedAt, borrow.getApprovedAt());
        assertEquals("同意借阅", borrow.getApprovalComment());
        assertEquals("保持完好", borrow.getReturnCondition());
        assertEquals("重要文件", borrow.getRemarks());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveBorrow borrow = new ArchiveBorrow();

        assertNull(borrow.getBorrowNo());
        assertNull(borrow.getArchiveId());
    }

    @Test
    void testStatusConstants() {
        assertEquals("PENDING", ArchiveBorrow.STATUS_PENDING);
        assertEquals("APPROVED", ArchiveBorrow.STATUS_APPROVED);
        assertEquals("REJECTED", ArchiveBorrow.STATUS_REJECTED);
        assertEquals("BORROWED", ArchiveBorrow.STATUS_BORROWED);
        assertEquals("RETURNED", ArchiveBorrow.STATUS_RETURNED);
        assertEquals("OVERDUE", ArchiveBorrow.STATUS_OVERDUE);
    }

    @Test
    void testSettersAndGetters() {
        ArchiveBorrow borrow = new ArchiveBorrow();

        borrow.setBorrowNo("BOR-2026-002");
        borrow.setArchiveId(200L);
        borrow.setBorrowerId(3L);
        borrow.setBorrowerName("王五");
        borrow.setStatus(ArchiveBorrow.STATUS_PENDING);

        assertEquals("BOR-2026-002", borrow.getBorrowNo());
        assertEquals(200L, borrow.getArchiveId());
        assertEquals(3L, borrow.getBorrowerId());
        assertEquals("王五", borrow.getBorrowerName());
        assertEquals(ArchiveBorrow.STATUS_PENDING, borrow.getStatus());
    }

    @Test
    void testBorrowWorkflow_Approve() {
        ArchiveBorrow borrow = ArchiveBorrow.builder()
                .borrowNo("BOR-2026-003")
                .status(ArchiveBorrow.STATUS_PENDING)
                .build();

        // 审批
        borrow.setStatus(ArchiveBorrow.STATUS_APPROVED);
        borrow.setApproverId(1L);
        borrow.setApproverName("审批人");
        borrow.setApprovedAt(LocalDateTime.now());

        assertEquals(ArchiveBorrow.STATUS_APPROVED, borrow.getStatus());
        assertNotNull(borrow.getApprovedAt());
    }

    @Test
    void testBorrowWorkflow_Reject() {
        ArchiveBorrow borrow = ArchiveBorrow.builder()
                .borrowNo("BOR-2026-004")
                .status(ArchiveBorrow.STATUS_PENDING)
                .build();

        // 拒绝
        borrow.setStatus(ArchiveBorrow.STATUS_REJECTED);
        borrow.setApprovalComment("权限不足");

        assertEquals(ArchiveBorrow.STATUS_REJECTED, borrow.getStatus());
        assertEquals("权限不足", borrow.getApprovalComment());
    }

    @Test
    void testBorrowWorkflow_Return() {
        ArchiveBorrow borrow = ArchiveBorrow.builder()
                .borrowNo("BOR-2026-005")
                .status(ArchiveBorrow.STATUS_BORROWED)
                .expectedReturnDate(LocalDate.of(2026, 3, 1))
                .build();

        // 归还
        borrow.setStatus(ArchiveBorrow.STATUS_RETURNED);
        borrow.setActualReturnDate(LocalDate.of(2026, 2, 28));

        assertEquals(ArchiveBorrow.STATUS_RETURNED, borrow.getStatus());
        assertEquals(LocalDate.of(2026, 2, 28), borrow.getActualReturnDate());
    }

    @Test
    void testOverdue() {
        LocalDate expectedReturn = LocalDate.of(2026, 1, 15);
        LocalDate today = LocalDate.of(2026, 1, 20);

        ArchiveBorrow borrow = ArchiveBorrow.builder()
                .status(ArchiveBorrow.STATUS_BORROWED)
                .expectedReturnDate(expectedReturn)
                .build();

        // 检查是否逾期
        boolean isOverdue = today.isAfter(expectedReturn);
        assertTrue(isOverdue);

        if (isOverdue) {
            borrow.setStatus(ArchiveBorrow.STATUS_OVERDUE);
        }

        assertEquals(ArchiveBorrow.STATUS_OVERDUE, borrow.getStatus());
    }

    @Test
    void testToString() {
        ArchiveBorrow borrow = ArchiveBorrow.builder()
                .borrowNo("TEST")
                .build();

        String str = borrow.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveBorrow"));
    }
}
