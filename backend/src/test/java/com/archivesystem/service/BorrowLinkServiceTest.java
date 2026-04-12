package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.dto.borrow.BorrowLinkApplyRequest;
import com.archivesystem.dto.borrow.BorrowLinkApplyResponse;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.repository.BorrowLinkMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.entity.BorrowLink;
import com.archivesystem.service.impl.BorrowLinkServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class BorrowLinkServiceTest {

    @Mock
    private BorrowLinkMapper borrowLinkMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private DigitalFileMapper digitalFileMapper;

    @Mock
    private BorrowApplicationMapper borrowApplicationMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private BorrowLinkServiceImpl borrowLinkService;

    @Test
    void testGetStats_UsesAggregateQuery() {
        when(borrowLinkMapper.selectAggregateStats()).thenReturn(Map.of(
                "totalCount", 100L,
                "activeCount", 80L,
                "expiredCount", 15L,
                "revokedCount", 5L,
                "totalAccessCount", 500L,
                "totalDownloadCount", 200L
        ));

        BorrowLinkService.BorrowLinkStats stats = borrowLinkService.getStats();

        assertEquals(100L, stats.totalCount());
        assertEquals(80L, stats.activeCount());
        assertEquals(15L, stats.expiredCount());
        assertEquals(5L, stats.revokedCount());
        assertEquals(500L, stats.totalAccessCount());
        assertEquals(200L, stats.totalDownloadCount());
    }

    @Test
    void testGetFileAccessUrl_UsesShortExpiryForPreview() {
        BorrowLink link = BorrowLink.builder()
                .archiveId(100L)
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(true)
                .accessCount(0)
                .expireAt(java.time.LocalDateTime.now().plusSeconds(30))
                .build();
        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);
        when(fileStorageService.getBorrowPreviewUrl(eq(100L), eq(8L), anyInt())).thenReturn("preview-url");

        assertEquals("preview-url", borrowLinkService.getFileAccessUrl("token", 8L, false));
        ArgumentCaptor<Integer> expiryCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(fileStorageService).getBorrowPreviewUrl(eq(100L), eq(8L), expiryCaptor.capture());
        assertTrue(expiryCaptor.getValue() > 0 && expiryCaptor.getValue() <= 30);
    }

    @Test
    void testRecordDownload_RejectsFileOutsideArchive() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .archiveId(100L)
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(true)
                .expireAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build();
        DigitalFile file = DigitalFile.builder()
                .id(8L)
                .archiveId(200L)
                .build();

        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);
        when(digitalFileMapper.selectById(8L)).thenReturn(file);

        assertThrows(BusinessException.class,
                () -> borrowLinkService.recordDownload("token", 8L, "127.0.0.1"));
    }

    @Test
    void testRecordDownload_RejectsWhenDownloadNotAllowed() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .archiveId(100L)
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(false)
                .expireAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build();

        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);

        assertThrows(BusinessException.class,
                () -> borrowLinkService.recordDownload("token", 8L, "127.0.0.1"));
    }

    @Test
    void testGenerateLinkForBorrow_RejectsPendingApplication() {
        BorrowApplication application = BorrowApplication.builder()
                .archiveId(100L)
                .status(BorrowApplication.STATUS_PENDING)
                .borrowType(BorrowApplication.TYPE_DOWNLOAD)
                .build();
        application.setId(1L);
        when(borrowApplicationMapper.selectById(1L)).thenReturn(application);

        assertThrows(BusinessException.class,
                () -> borrowLinkService.generateLinkForBorrow(1L, 7, true));
    }

    @Test
    void testGenerateLinkForBorrow_DisablesDownloadForOnlineBorrow() {
        BorrowApplication application = BorrowApplication.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .applicantName("张三")
                .borrowPurpose("阅卷")
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .status(BorrowApplication.STATUS_APPROVED)
                .expectedReturnDate(LocalDate.now().plusDays(3))
                .build();
        application.setId(1L);
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .build();
        archive.setId(100L);
        when(borrowApplicationMapper.selectById(1L)).thenReturn(application);
        when(archiveMapper.selectById(100L)).thenReturn(archive);

        BorrowLink link = borrowLinkService.generateLinkForBorrow(1L, 7, true);

        assertFalse(Boolean.TRUE.equals(link.getAllowDownload()));
        verify(borrowLinkMapper).insert(any(BorrowLink.class));
    }

    @Test
    void testValidateAndAccess_UpdatesBorrowViewStats() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .borrowId(1L)
                .archiveId(100L)
                .archiveNo("ARC-001")
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(false)
                .accessCount(0)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .sourceUserName("张三")
                .borrowPurpose("阅卷")
                .build();
        BorrowApplication application = BorrowApplication.builder()
                .archiveId(100L)
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .expectedReturnDate(LocalDate.now().plusDays(2))
                .viewCount(0)
                .downloadCount(0)
                .build();
        application.setId(1L);
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .archiveType("DOCUMENT")
                .build();
        archive.setId(100L);

        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);
        when(borrowApplicationMapper.selectById(1L)).thenReturn(application);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(digitalFileMapper.selectByArchiveId(100L)).thenReturn(java.util.List.of());

        borrowLinkService.validateAndAccess("token", "127.0.0.1");

        assertEquals(1, application.getViewCount());
        verify(borrowApplicationMapper).updateById(application);
    }

    @Test
    void testApplyLink_RejectsMismatchedArchiveIdAndArchiveNo() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .build();
        archive.setId(100L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-999")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .build();

        assertThrows(BusinessException.class, () -> borrowLinkService.applyLink(request));
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testApplyLink_ReusesExistingExternalLinkForSameRequest() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .build();
        archive.setId(100L);
        BorrowLink existing = BorrowLink.builder()
                .id(88L)
                .archiveId(100L)
                .archiveNo("ARC-001")
                .accessToken("existing-token")
                .sourceType(BorrowLink.SOURCE_TYPE_LAW_FIRM)
                .sourceUserId("u1")
                .sourceUserName("张三")
                .borrowPurpose("阅卷")
                .allowDownload(true)
                .status(BorrowLink.STATUS_ACTIVE)
                .expireAt(LocalDateTime.now().plusDays(3))
                .build();
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowLinkMapper.selectBySourceUser("u1", BorrowLink.SOURCE_TYPE_LAW_FIRM))
                .thenReturn(List.of(existing));

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .allowDownload(true)
                .build();

        BorrowLinkApplyResponse response = borrowLinkService.applyLink(request);

        assertEquals(88L, response.getLinkId());
        assertEquals("existing-token", response.getAccessToken());
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }
}
