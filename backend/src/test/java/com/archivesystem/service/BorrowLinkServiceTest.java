package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.dto.borrow.BorrowLinkApplyRequest;
import com.archivesystem.dto.borrow.BorrowLinkApplyResponse;
import com.archivesystem.dto.borrow.BorrowLinkResponse;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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

    @Mock
    private AccessLogService accessLogService;

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
        DigitalFile file = DigitalFile.builder()
                .id(8L)
                .archiveId(100L)
                .build();
        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);
        when(digitalFileMapper.selectById(8L)).thenReturn(file);
        when(fileStorageService.getBorrowPreviewUrl(eq(100L), eq(8L), anyInt())).thenReturn("preview-url");

        assertEquals("preview-url", borrowLinkService.getFileAccessUrl("token", 8L, false, "127.0.0.1"));
        ArgumentCaptor<Integer> expiryCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(fileStorageService).getBorrowPreviewUrl(eq(100L), eq(8L), expiryCaptor.capture());
        assertEquals(1, link.getAccessCount());
        verify(accessLogService).logPreview(100L, 8L, "127.0.0.1");
        assertTrue(expiryCaptor.getValue() > 0 && expiryCaptor.getValue() <= 30);
    }

    @Test
    void testGetFileAccessUrl_Download_ShouldTrackAccessAndDownloadAudit() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .archiveId(100L)
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(true)
                .accessCount(0)
                .downloadCount(0)
                .expireAt(java.time.LocalDateTime.now().plusSeconds(30))
                .build();
        DigitalFile file = DigitalFile.builder()
                .id(8L)
                .archiveId(100L)
                .build();
        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);
        when(digitalFileMapper.selectById(8L)).thenReturn(file);
        when(fileStorageService.getBorrowDownloadUrl(eq(100L), eq(8L), anyInt())).thenReturn("download-url");

        assertEquals("download-url", borrowLinkService.getFileAccessUrl("token", 8L, true, "127.0.0.1"));

        assertEquals(1, link.getAccessCount());
        assertEquals(1, link.getDownloadCount());
        verify(accessLogService).logDownload(100L, 8L, "127.0.0.1");
    }

    @Test
    void testGetFileAccessUrl_LimitedPreviewShouldUseAtomicAccessUpdate() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .archiveId(100L)
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(true)
                .accessCount(0)
                .maxAccessCount(1)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
        DigitalFile file = DigitalFile.builder()
                .id(8L)
                .archiveId(100L)
                .build();

        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);
        when(borrowLinkMapper.atomicRecordFileAccess(10L, 1, 0, "127.0.0.1")).thenReturn(1);
        when(digitalFileMapper.selectById(8L)).thenReturn(file);
        when(fileStorageService.getBorrowPreviewUrl(eq(100L), eq(8L), anyInt())).thenReturn("preview-url");

        assertEquals("preview-url", borrowLinkService.getFileAccessUrl("token", 8L, false, "127.0.0.1"));

        assertEquals(1, link.getAccessCount());
        verify(borrowLinkMapper).atomicRecordFileAccess(10L, 1, 0, "127.0.0.1");
        verify(borrowLinkMapper, never()).updateById(link);
    }

    @Test
    void testGetFileAccessUrl_LimitedDownloadShouldRejectWhenConcurrentLimitReached() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .archiveId(100L)
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(true)
                .accessCount(0)
                .downloadCount(0)
                .maxAccessCount(1)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
        DigitalFile file = DigitalFile.builder()
                .id(8L)
                .archiveId(100L)
                .build();

        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);
        when(borrowLinkMapper.atomicRecordFileAccess(10L, 1, 1, "127.0.0.1")).thenReturn(0);
        when(digitalFileMapper.selectById(8L)).thenReturn(file);

        assertThrows(BusinessException.class,
                () -> borrowLinkService.getFileAccessUrl("token", 8L, true, "127.0.0.1"));

        assertEquals(0, link.getAccessCount());
        assertEquals(0, link.getDownloadCount());
        verify(fileStorageService, never()).getBorrowDownloadUrl(anyLong(), anyLong(), anyInt());
        verify(accessLogService, never()).logDownload(anyLong(), anyLong(), anyString());
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
    void testRecordDownload_LegacyEndpoint_ShouldNotDoubleCount() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .archiveId(100L)
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(true)
                .downloadCount(5)
                .expireAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build();
        DigitalFile file = DigitalFile.builder()
                .id(8L)
                .archiveId(100L)
                .build();

        when(borrowLinkMapper.selectByAccessToken("token")).thenReturn(link);
        when(digitalFileMapper.selectById(8L)).thenReturn(file);

        borrowLinkService.recordDownload("token", 8L, "127.0.0.1");

        assertEquals(5, link.getDownloadCount());
        verify(borrowLinkMapper, never()).updateById(any(BorrowLink.class));
        verify(accessLogService, never()).logDownload(anyLong(), anyLong(), anyString());
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

        BorrowLinkResponse link = borrowLinkService.generateLinkForBorrow(1L, 7, true);

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
    void testValidateAndAccess_LimitedLinkShouldRefreshInMemoryAccessState() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .borrowId(1L)
                .archiveId(100L)
                .archiveNo("ARC-001")
                .accessToken("token")
                .status(BorrowLink.STATUS_ACTIVE)
                .allowDownload(false)
                .accessCount(0)
                .maxAccessCount(3)
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
        when(borrowLinkMapper.atomicRecordAccess(10L, 3, "127.0.0.1")).thenReturn(1);
        when(borrowApplicationMapper.selectById(1L)).thenReturn(application);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(digitalFileMapper.selectByArchiveId(100L)).thenReturn(List.of());

        var response = borrowLinkService.validateAndAccess("token", "127.0.0.1");

        assertTrue(Boolean.TRUE.equals(response.getValid()));
        assertEquals(1, link.getAccessCount());
        assertEquals("127.0.0.1", link.getLastAccessIp());
        assertEquals(1, response.getLinkInfo().getAccessCount());
        verify(borrowLinkMapper).atomicRecordAccess(10L, 3, "127.0.0.1");
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

        assertThrows(BusinessException.class, () -> borrowLinkService.applyLink(request, "LAW_FIRM_A"));
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testApplyLink_ReusesExistingExternalLinkForSameRequest() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .build();
        archive.setId(100L);
        BorrowApplication approvedApplication = BorrowApplication.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .applicantName("张三")
                .applicantDept("LAW_FIRM_A")
                .borrowPurpose("阅卷")
                .borrowType(BorrowApplication.TYPE_DOWNLOAD)
                .status(BorrowApplication.STATUS_APPROVED)
                .remarks("[external-borrow] source=LAW_FIRM_A; userId=u1")
                .expectedReturnDate(LocalDate.now().plusDays(7))
                .build();
        approvedApplication.setId(66L);
        BorrowLink existing = BorrowLink.builder()
                .id(88L)
                .borrowId(66L)
                .archiveId(100L)
                .archiveNo("ARC-001")
                .accessToken("existing-token")
                .sourceType(BorrowLink.SOURCE_TYPE_LAW_FIRM)
                .sourceSystem("LAW_FIRM_A")
                .sourceUserId("u1")
                .sourceUserName("张三")
                .borrowPurpose("阅卷")
                .allowDownload(true)
                .status(BorrowLink.STATUS_ACTIVE)
                .expireAt(LocalDateTime.now().plusDays(3))
                .build();
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(List.of(approvedApplication));
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

        BorrowLinkApplyResponse response = borrowLinkService.applyLink(request, "LAW_FIRM_A");

        assertEquals(88L, response.getLinkId());
        assertEquals("/open/borrow/access/existing-token", response.getAccessUrl());
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testApplyLink_ShouldMatchApprovedApplicationWhenOnlyArchiveNoProvided() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .archiveForm(Archive.FORM_ELECTRONIC)
                .hasElectronic(true)
                .build();
        archive.setId(100L);
        BorrowApplication approvedApplication = BorrowApplication.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .applicantName("张三")
                .applicantDept("LAW_FIRM_A")
                .borrowPurpose("阅卷")
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .status(BorrowApplication.STATUS_APPROVED)
                .remarks("[external-borrow] source=LAW_FIRM_A; userId=u1")
                .expectedReturnDate(LocalDate.now().plusDays(7))
                .build();
        approvedApplication.setId(77L);
        when(archiveMapper.selectOne(any())).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(List.of(approvedApplication));
        when(borrowLinkMapper.selectBySourceUser("u1", BorrowLink.SOURCE_TYPE_LAW_FIRM)).thenReturn(List.of());

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .build();

        borrowLinkService.applyLink(request, "LAW_FIRM_A");

        verify(borrowLinkMapper).insert(argThat(link -> approvedApplication.getId().equals(link.getBorrowId())));
        verify(borrowApplicationMapper, never()).insert(any(BorrowApplication.class));
    }

    @Test
    void testApplyLink_ShouldBindExternalSourceCode() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .build();
        archive.setId(100L);
        BorrowApplication approvedApplication = BorrowApplication.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .applicantName("张三")
                .applicantDept("LAW_FIRM_A")
                .borrowPurpose("阅卷")
                .borrowType(BorrowApplication.TYPE_DOWNLOAD)
                .status(BorrowApplication.STATUS_APPROVED)
                .remarks("[external-borrow] source=LAW_FIRM_A; userId=u1")
                .expectedReturnDate(LocalDate.now().plusDays(7))
                .build();
        approvedApplication.setId(66L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(List.of(approvedApplication));
        when(borrowLinkMapper.selectBySourceUser("u1", BorrowLink.SOURCE_TYPE_LAW_FIRM))
                .thenReturn(List.of());

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .allowDownload(true)
                .build();

        borrowLinkService.applyLink(request, "LAW_FIRM_A");

        verify(borrowLinkMapper).insert(argThat(link ->
                "LAW_FIRM_A".equals(link.getSourceSystem()) && approvedApplication.getId().equals(link.getBorrowId())));
    }

    @Test
    void testApplyLink_ShouldDefaultToPreviewOnly() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .archiveForm(Archive.FORM_ELECTRONIC)
                .hasElectronic(true)
                .build();
        archive.setId(100L);
        BorrowApplication approvedApplication = BorrowApplication.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .applicantName("张三")
                .applicantDept("LAW_FIRM_A")
                .borrowPurpose("阅卷")
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .status(BorrowApplication.STATUS_APPROVED)
                .remarks("[external-borrow] source=LAW_FIRM_A; userId=u1")
                .expectedReturnDate(LocalDate.now().plusDays(7))
                .build();
        approvedApplication.setId(77L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(List.of(approvedApplication));
        when(borrowLinkMapper.selectBySourceUser("u1", BorrowLink.SOURCE_TYPE_LAW_FIRM))
                .thenReturn(List.of());

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .build();

        borrowLinkService.applyLink(request, "LAW_FIRM_A");

        verify(borrowLinkMapper).insert(argThat(link ->
                !Boolean.TRUE.equals(link.getAllowDownload()) && approvedApplication.getId().equals(link.getBorrowId())));
    }

    @Test
    void testApplyLink_ShouldCreatePendingBorrowApplicationWhenNoApprovedRequestExists() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .archiveForm(Archive.FORM_ELECTRONIC)
                .hasElectronic(true)
                .build();
        archive.setId(100L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(List.of());

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .build();

        assertThrows(BusinessException.class, () -> borrowLinkService.applyLink(request, "LAW_FIRM_A"));

        verify(borrowApplicationMapper).insert(argThat(application ->
                BorrowApplication.STATUS_PENDING.equals(application.getStatus())
                        && "张三".equals(application.getApplicantName())
                        && BorrowApplication.TYPE_ONLINE.equals(application.getBorrowType())));
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testApplyLink_ShouldRejectWhenMatchingApplicationStillPending() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .archiveForm(Archive.FORM_ELECTRONIC)
                .hasElectronic(true)
                .build();
        archive.setId(100L);
        BorrowApplication pendingApplication = BorrowApplication.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .applicantName("张三")
                .applicantDept("LAW_FIRM_A")
                .borrowPurpose("阅卷")
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .status(BorrowApplication.STATUS_PENDING)
                .remarks("[external-borrow] source=LAW_FIRM_A; userId=u1")
                .expectedReturnDate(LocalDate.now().plusDays(7))
                .build();
        pendingApplication.setId(88L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(List.of(pendingApplication));

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .build();

        assertThrows(BusinessException.class, () -> borrowLinkService.applyLink(request, "LAW_FIRM_A"));

        verify(borrowApplicationMapper, never()).insert(any(BorrowApplication.class));
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testApplyLink_ShouldNotReuseApprovedApplicationFromDifferentExternalUserWithSameName() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .archiveForm(Archive.FORM_ELECTRONIC)
                .hasElectronic(true)
                .build();
        archive.setId(100L);
        BorrowApplication approvedApplication = BorrowApplication.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .applicantName("张三")
                .applicantDept("LAW_FIRM_A")
                .borrowPurpose("阅卷")
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .status(BorrowApplication.STATUS_APPROVED)
                .remarks("[external-borrow] source=LAW_FIRM_A; userId=u-approved")
                .expectedReturnDate(LocalDate.now().plusDays(7))
                .build();
        approvedApplication.setId(88L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(List.of(approvedApplication));

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u-other")
                .userName("张三")
                .purpose("阅卷")
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> borrowLinkService.applyLink(request, "LAW_FIRM_A"));

        assertEquals("1010", ex.getCode());
        verify(borrowApplicationMapper).insert(argThat(application ->
                BorrowApplication.STATUS_PENDING.equals(application.getStatus())
                        && "LAW_FIRM_A".equals(application.getApplicantDept())
                        && "[external-borrow] source=LAW_FIRM_A; userId=u-other".equals(application.getRemarks())));
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testBorrowApplicationResponse_ShouldHideInternalExternalIdentityMarker() {
        BorrowApplication application = BorrowApplication.builder()
                .applicationNo("BR-EXT-001")
                .remarks("[external-borrow] source=LAW_FIRM_A; userId=u1")
                .build();

        var response = com.archivesystem.dto.borrow.BorrowApplicationResponse.from(application);

        assertNull(response.getRemarks());
    }

    @Test
    void testApplyLink_ShouldRejectNonStoredArchive() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_BORROWED)
                .archiveForm(Archive.FORM_ELECTRONIC)
                .hasElectronic(true)
                .build();
        archive.setId(100L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .build();

        assertThrows(BusinessException.class, () -> borrowLinkService.applyLink(request, "LAW_FIRM_A"));
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testApplyLink_ShouldRejectArchiveWithoutElectronicCarrier() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .archiveForm(Archive.FORM_PHYSICAL)
                .hasElectronic(false)
                .build();
        archive.setId(100L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .build();

        assertThrows(BusinessException.class, () -> borrowLinkService.applyLink(request, "LAW_FIRM_A"));
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testApplyLink_ShouldRejectDownloadForConfidentialArchive() {
        Archive archive = Archive.builder()
                .archiveNo("ARC-001")
                .title("测试档案")
                .status(Archive.STATUS_STORED)
                .archiveForm(Archive.FORM_ELECTRONIC)
                .hasElectronic(true)
                .securityLevel(Archive.SECURITY_CONFIDENTIAL)
                .build();
        archive.setId(100L);
        when(archiveMapper.selectById(100L)).thenReturn(archive);

        BorrowLinkApplyRequest request = BorrowLinkApplyRequest.builder()
                .archiveId(100L)
                .archiveNo("ARC-001")
                .userId("u1")
                .userName("张三")
                .purpose("阅卷")
                .allowDownload(true)
                .build();

        assertThrows(BusinessException.class, () -> borrowLinkService.applyLink(request, "LAW_FIRM_A"));
        verify(borrowLinkMapper, never()).insert(any(BorrowLink.class));
    }

    @Test
    void testRevoke_ShouldRejectOtherExternalSource() {
        BorrowLink link = BorrowLink.builder()
                .id(10L)
                .status(BorrowLink.STATUS_ACTIVE)
                .sourceSystem("LAW_FIRM_A")
                .build();
        when(borrowLinkMapper.selectById(10L)).thenReturn(link);

        assertThrows(BusinessException.class, () -> borrowLinkService.revoke(10L, "test", "LAW_FIRM_B"));
        verify(borrowLinkMapper, never()).updateById(any(BorrowLink.class));
    }
}
