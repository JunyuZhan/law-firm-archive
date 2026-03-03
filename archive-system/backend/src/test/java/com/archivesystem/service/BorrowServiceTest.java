package com.archivesystem.service;

import com.archivesystem.TestLambdaCacheInitializer;
import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.impl.BorrowServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class BorrowServiceTest {

    @BeforeAll
    static void initLambdaCache() {
        TestLambdaCacheInitializer.initTableInfo(BorrowApplication.class, Archive.class);
    }

    @Mock
    private BorrowApplicationMapper borrowMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private Archive testArchive;
    private BorrowApplication testApplication;

    @BeforeEach
    void setUp() {
        testArchive = new Archive();
        testArchive.setId(1L);
        testArchive.setArchiveNo("ARC-20260213-0001");
        testArchive.setTitle("测试档案");
        testArchive.setStatus(Archive.STATUS_STORED);

        testApplication = new BorrowApplication();
        testApplication.setId(1L);
        testApplication.setApplicationNo("BR-20260213-0001");
        testApplication.setArchiveId(1L);
        testApplication.setArchiveNo("ARC-20260213-0001");
        testApplication.setArchiveTitle("测试档案");
        testApplication.setApplicantId(100L);
        testApplication.setApplicantName("测试用户");
        testApplication.setBorrowPurpose("研究");
        testApplication.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testApplication.setStatus(BorrowApplication.STATUS_PENDING);
        testApplication.setApplyTime(LocalDateTime.now());
        testApplication.setDeleted(false);
    }

    @Test
    void testApply_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(100L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");

            when(archiveMapper.selectById(1L)).thenReturn(testArchive);
            when(borrowMapper.selectOne(any())).thenReturn(null);
            when(borrowMapper.insert(any(BorrowApplication.class))).thenReturn(1);

            BorrowApplication result = borrowService.apply(1L, "研究", LocalDate.now().plusDays(7), null);

            assertNotNull(result);
            assertEquals(BorrowApplication.STATUS_PENDING, result.getStatus());
            verify(borrowMapper).insert(any(BorrowApplication.class));
        }
    }

    @Test
    void testApply_ArchiveNotFound() {
        when(archiveMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> 
            borrowService.apply(999L, "研究", LocalDate.now().plusDays(7), null));
    }

    @Test
    void testApply_ArchiveAlreadyBorrowed() {
        testArchive.setStatus(Archive.STATUS_BORROWED);
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);

        assertThrows(BusinessException.class, () -> 
            borrowService.apply(1L, "研究", LocalDate.now().plusDays(7), null));
    }

    @Test
    void testApply_ExistingApplicationInProgress() {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(borrowMapper.selectOne(any())).thenReturn(testApplication);

        assertThrows(BusinessException.class, () -> 
            borrowService.apply(1L, "研究", LocalDate.now().plusDays(7), null));
    }

    @Test
    void testGetById_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(100L);
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVIST")).thenReturn(false);

            when(borrowMapper.selectById(1L)).thenReturn(testApplication);

            BorrowApplication result = borrowService.getById(1L);

            assertNotNull(result);
            assertEquals("BR-20260213-0001", result.getApplicationNo());
        }
    }

    @Test
    void testGetById_NotFound() {
        when(borrowMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> borrowService.getById(999L));
    }

    @Test
    void testGetMyApplications() {
        Page<BorrowApplication> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testApplication));
        page.setTotal(1);

        when(borrowMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<BorrowApplication> result = borrowService.getMyApplications(100L, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void testGetMyApplications_WithStatusFilter() {
        Page<BorrowApplication> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testApplication));
        page.setTotal(1);

        when(borrowMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<BorrowApplication> result = borrowService.getMyApplications(100L, "PENDING", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void testCancel_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(100L);

            when(borrowMapper.selectById(1L)).thenReturn(testApplication);
            when(borrowMapper.updateById(any(BorrowApplication.class))).thenReturn(1);

            assertDoesNotThrow(() -> borrowService.cancel(1L));

            verify(borrowMapper).updateById(argThat(app -> 
                BorrowApplication.STATUS_CANCELLED.equals(app.getStatus())));
        }
    }

    @Test
    void testCancel_NotPendingStatus() {
        testApplication.setStatus(BorrowApplication.STATUS_APPROVED);
        when(borrowMapper.selectById(1L)).thenReturn(testApplication);

        assertThrows(BusinessException.class, () -> borrowService.cancel(1L));
    }

    @Test
    void testCancel_NotOwner() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(999L);

            when(borrowMapper.selectById(1L)).thenReturn(testApplication);

            assertThrows(BusinessException.class, () -> borrowService.cancel(1L));
        }
    }

    @Test
    void testGetPendingList() {
        Page<BorrowApplication> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testApplication));
        page.setTotal(1);

        when(borrowMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<BorrowApplication> result = borrowService.getPendingList(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testApprove_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(200L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("审批人");
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVIST")).thenReturn(true);

            when(borrowMapper.selectById(1L)).thenReturn(testApplication);
            when(borrowMapper.update(isNull(), any())).thenReturn(1);

            assertDoesNotThrow(() -> borrowService.approve(1L, "同意"));

            verify(borrowMapper).update(isNull(), any());
        }
    }

    @Test
    void testApprove_NotPendingStatus() {
        testApplication.setStatus(BorrowApplication.STATUS_APPROVED);
        when(borrowMapper.selectById(1L)).thenReturn(testApplication);

        assertThrows(BusinessException.class, () -> borrowService.approve(1L, "同意"));
    }

    @Test
    void testReject_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(200L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("审批人");
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVIST")).thenReturn(true);

            when(borrowMapper.selectById(1L)).thenReturn(testApplication);
            when(borrowMapper.update(isNull(), any())).thenReturn(1);

            assertDoesNotThrow(() -> borrowService.reject(1L, "不符合条件"));

            verify(borrowMapper).update(isNull(), any());
        }
    }

    @Test
    void testReject_NotPendingStatus() {
        testApplication.setStatus(BorrowApplication.STATUS_BORROWED);
        when(borrowMapper.selectById(1L)).thenReturn(testApplication);

        assertThrows(BusinessException.class, () -> borrowService.reject(1L, "不符合条件"));
    }

    @Test
    void testLend_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(200L);
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVIST")).thenReturn(true);

            testApplication.setStatus(BorrowApplication.STATUS_APPROVED);
            when(borrowMapper.selectById(1L)).thenReturn(testApplication);
            when(borrowMapper.update(isNull(), any())).thenReturn(1);
            when(archiveMapper.selectById(1L)).thenReturn(testArchive);
            when(archiveMapper.update(isNull(), any())).thenReturn(1);

            assertDoesNotThrow(() -> borrowService.lend(1L));

            verify(borrowMapper).update(isNull(), any());
        }
    }

    @Test
    void testLend_NotApproved() {
        when(borrowMapper.selectById(1L)).thenReturn(testApplication);

        assertThrows(BusinessException.class, () -> borrowService.lend(1L));
    }

    @Test
    void testReturnArchive_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(200L);
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVIST")).thenReturn(true);

            testApplication.setStatus(BorrowApplication.STATUS_BORROWED);
            when(borrowMapper.selectById(1L)).thenReturn(testApplication);
            when(borrowMapper.update(isNull(), any())).thenReturn(1);
            when(archiveMapper.selectById(1L)).thenReturn(testArchive);
            when(archiveMapper.update(isNull(), any())).thenReturn(1);

            assertDoesNotThrow(() -> borrowService.returnArchive(1L, "已归还"));

            verify(borrowMapper).update(isNull(), any());
        }
    }

    @Test
    void testReturnArchive_NotBorrowed() {
        when(borrowMapper.selectById(1L)).thenReturn(testApplication);

        assertThrows(BusinessException.class, () -> borrowService.returnArchive(1L, "已归还"));
    }

    @Test
    void testRenew_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(200L);
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVIST")).thenReturn(true);

            testApplication.setStatus(BorrowApplication.STATUS_BORROWED);
            testApplication.setExpectedReturnDate(LocalDate.now().plusDays(7));
            when(borrowMapper.selectById(1L)).thenReturn(testApplication);
            when(borrowMapper.updateById(any(BorrowApplication.class))).thenReturn(1);

            LocalDate newDate = LocalDate.now().plusDays(14);
            assertDoesNotThrow(() -> borrowService.renew(1L, newDate));

            verify(borrowMapper).updateById(argThat(app -> 
                app.getExpectedReturnDate().equals(newDate) &&
                app.getRenewCount() != null && app.getRenewCount() >= 1));
        }
    }

    @Test
    void testRenew_NotBorrowed() {
        when(borrowMapper.selectById(1L)).thenReturn(testApplication);

        assertThrows(BusinessException.class, () -> 
            borrowService.renew(1L, LocalDate.now().plusDays(14)));
    }

    @Test
    void testRenew_EarlierDate() {
        testApplication.setStatus(BorrowApplication.STATUS_BORROWED);
        testApplication.setExpectedReturnDate(LocalDate.now().plusDays(7));
        when(borrowMapper.selectById(1L)).thenReturn(testApplication);

        assertThrows(BusinessException.class, () -> 
            borrowService.renew(1L, LocalDate.now().plusDays(3)));
    }

    @Test
    void testGetOverdueList() {
        testApplication.setStatus(BorrowApplication.STATUS_BORROWED);
        testApplication.setExpectedReturnDate(LocalDate.now().minusDays(3));

        when(borrowMapper.selectList(any())).thenReturn(Arrays.asList(testApplication));

        List<BorrowApplication> result = borrowService.getOverdueList();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetCurrentByArchiveId() {
        when(borrowMapper.selectOne(any())).thenReturn(testApplication);

        BorrowApplication result = borrowService.getCurrentByArchiveId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getArchiveId());
    }

    @Test
    void testGetCurrentByArchiveId_NotFound() {
        when(borrowMapper.selectOne(any())).thenReturn(null);

        BorrowApplication result = borrowService.getCurrentByArchiveId(999L);

        assertNull(result);
    }
}
