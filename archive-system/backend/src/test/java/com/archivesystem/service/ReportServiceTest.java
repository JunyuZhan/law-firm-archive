package com.archivesystem.service;

import com.archivesystem.dto.archive.ArchiveQueryRequest;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.repository.OperationLogMapper;
import com.archivesystem.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private BorrowApplicationMapper borrowApplicationMapper;

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Archive testArchive;
    private BorrowApplication testApplication;
    private OperationLog testLog;

    @BeforeEach
    void setUp() {
        testArchive = new Archive();
        testArchive.setId(1L);
        testArchive.setArchiveNo("ARC-20260213-0001");
        testArchive.setTitle("测试档案");
        testArchive.setArchiveType("DOCUMENT");
        testArchive.setFondsNo("QZ001");
        testArchive.setCategoryCode("01");
        testArchive.setRetentionPeriod("Y10");
        testArchive.setSecurityLevel("INTERNAL");
        testArchive.setCaseNo("CASE-001");
        testArchive.setLawyerName("张律师");
        testArchive.setClientName("李委托人");
        testArchive.setFileCount(5);
        testArchive.setStatus(Archive.STATUS_STORED);
        testArchive.setReceivedAt(LocalDateTime.now());

        testApplication = new BorrowApplication();
        testApplication.setId(1L);
        testApplication.setApplicationNo("BR-20260213-0001");
        testApplication.setArchiveNo("ARC-20260213-0001");
        testApplication.setArchiveTitle("测试档案");
        testApplication.setApplicantName("申请人");
        testApplication.setApplicantDept("法务部");
        testApplication.setBorrowPurpose("研究");
        testApplication.setApplyTime(LocalDateTime.now());
        testApplication.setStatus(BorrowApplication.STATUS_PENDING);

        testLog = OperationLog.builder()
                .id(1L)
                .objectType("ARCHIVE")
                .objectId("1")
                .operationType(OperationLog.OP_CREATE)
                .operationDesc("创建档案")
                .operatorName("管理员")
                .operatorIp("192.168.1.1")
                .operatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testExportOverviewReport_Success() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalArchives", 100L);
        overview.put("monthlyNew", 10L);
        overview.put("yearlyNew", 50L);
        overview.put("totalFiles", 500L);
        overview.put("totalStorageFormatted", "1.5 GB");
        overview.put("pendingBorrows", 5L);
        overview.put("borrowedCount", 3L);

        List<Map<String, Object>> typeStats = new ArrayList<>();
        Map<String, Object> typeStat = new HashMap<>();
        typeStat.put("archiveType", "DOCUMENT");
        typeStat.put("count", 80L);
        typeStats.add(typeStat);

        List<Map<String, Object>> trendData = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> trend = new HashMap<>();
            trend.put("month", i);
            trend.put("count", 5L);
            trendData.add(trend);
        }

        when(statisticsService.getOverview()).thenReturn(overview);
        when(statisticsService.getByArchiveType()).thenReturn(typeStats);
        when(statisticsService.getMonthlyTrend(anyInt())).thenReturn(trendData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportOverviewReport(2026, outputStream));
        
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportOverviewReport_NoYear() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalArchives", 0L);

        List<Map<String, Object>> typeStats = new ArrayList<>();
        List<Map<String, Object>> trendData = new ArrayList<>();

        when(statisticsService.getOverview()).thenReturn(overview);
        when(statisticsService.getByArchiveType()).thenReturn(typeStats);
        when(statisticsService.getMonthlyTrend(anyInt())).thenReturn(trendData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportOverviewReport(null, outputStream));
    }

    @Test
    void testExportArchiveList_Success() {
        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList(testArchive));

        ArchiveQueryRequest request = new ArchiveQueryRequest();
        request.setPageSize(100);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportArchiveList(request, outputStream));
        
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportArchiveList_WithFilters() {
        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList(testArchive));

        ArchiveQueryRequest request = new ArchiveQueryRequest();
        request.setKeyword("测试");
        request.setArchiveType("DOCUMENT");
        request.setFondsId(1L);
        request.setStatus("STORED");
        request.setArchiveDateStart(LocalDate.of(2026, 1, 1));
        request.setArchiveDateEnd(LocalDate.of(2026, 12, 31));
        request.setPageSize(100);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportArchiveList(request, outputStream));
    }

    @Test
    void testExportArchiveList_LimitPageSize() {
        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList());

        ArchiveQueryRequest request = new ArchiveQueryRequest();
        request.setPageSize(20000); // 超出限制
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportArchiveList(request, outputStream));
    }

    @Test
    void testExportArchiveList_EmptyResult() {
        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList());

        ArchiveQueryRequest request = new ArchiveQueryRequest();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportArchiveList(request, outputStream));
        assertTrue(outputStream.size() > 0); // 仍有表头
    }

    @Test
    void testExportBorrowReport_Success() {
        when(borrowApplicationMapper.selectList(any())).thenReturn(Arrays.asList(testApplication));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> 
            reportService.exportBorrowReport(LocalDate.now().minusDays(30), LocalDate.now(), outputStream));
        
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportBorrowReport_NoDateFilter() {
        when(borrowApplicationMapper.selectList(any())).thenReturn(Arrays.asList(testApplication));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportBorrowReport(null, null, outputStream));
    }

    @Test
    void testExportBorrowReport_WithApprovalInfo() {
        testApplication.setApproverName("审批人");
        testApplication.setApproveTime(LocalDateTime.now());
        testApplication.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testApplication.setActualReturnDate(LocalDate.now());
        testApplication.setStatus(BorrowApplication.STATUS_RETURNED);

        when(borrowApplicationMapper.selectList(any())).thenReturn(Arrays.asList(testApplication));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> 
            reportService.exportBorrowReport(LocalDate.now().minusDays(30), LocalDate.now(), outputStream));
    }

    @Test
    void testExportOperationLogReport_Success() {
        when(operationLogMapper.selectList(any())).thenReturn(Arrays.asList(testLog));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> 
            reportService.exportOperationLogReport(LocalDate.now().minusDays(7), LocalDate.now(), outputStream));
        
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportOperationLogReport_NoDateFilter() {
        when(operationLogMapper.selectList(any())).thenReturn(Arrays.asList(testLog));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportOperationLogReport(null, null, outputStream));
    }

    @Test
    void testExportOperationLogReport_EmptyResult() {
        when(operationLogMapper.selectList(any())).thenReturn(Arrays.asList());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportOperationLogReport(null, null, outputStream));
        assertTrue(outputStream.size() > 0); // 仍有表头
    }

    @Test
    void testExportArchiveList_AllTypes() {
        // 测试所有档案类型的名称转换
        Archive archive1 = createArchiveWithType("DOCUMENT");
        Archive archive2 = createArchiveWithType("SCIENCE");
        Archive archive3 = createArchiveWithType("ACCOUNTING");
        Archive archive4 = createArchiveWithType("PERSONNEL");
        Archive archive5 = createArchiveWithType("SPECIAL");
        Archive archive6 = createArchiveWithType("AUDIOVISUAL");

        when(archiveMapper.selectList(any())).thenReturn(
            Arrays.asList(archive1, archive2, archive3, archive4, archive5, archive6));

        ArchiveQueryRequest request = new ArchiveQueryRequest();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportArchiveList(request, outputStream));
    }

    @Test
    void testExportArchiveList_AllStatuses() {
        // 测试所有状态的名称转换
        Archive archive1 = createArchiveWithStatus("DRAFT");
        Archive archive2 = createArchiveWithStatus("RECEIVED");
        Archive archive3 = createArchiveWithStatus("PROCESSING");
        Archive archive4 = createArchiveWithStatus("STORED");
        Archive archive5 = createArchiveWithStatus("BORROWED");

        when(archiveMapper.selectList(any())).thenReturn(
            Arrays.asList(archive1, archive2, archive3, archive4, archive5));

        ArchiveQueryRequest request = new ArchiveQueryRequest();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> reportService.exportArchiveList(request, outputStream));
    }

    private Archive createArchiveWithType(String type) {
        Archive archive = new Archive();
        archive.setId(System.currentTimeMillis());
        archive.setArchiveNo("ARC-" + System.nanoTime());
        archive.setTitle("测试档案");
        archive.setArchiveType(type);
        archive.setStatus(Archive.STATUS_STORED);
        archive.setReceivedAt(LocalDateTime.now());
        return archive;
    }

    private Archive createArchiveWithStatus(String status) {
        Archive archive = new Archive();
        archive.setId(System.currentTimeMillis());
        archive.setArchiveNo("ARC-" + System.nanoTime());
        archive.setTitle("测试档案");
        archive.setArchiveType("DOCUMENT");
        archive.setStatus(status);
        archive.setReceivedAt(LocalDateTime.now());
        return archive;
    }
}
