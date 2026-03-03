package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.config.MetricsConfig;
import com.archivesystem.dto.archive.*;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.Category;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.entity.Fonds;
import com.archivesystem.entity.RetentionPeriod;
import com.archivesystem.repository.*;
import com.archivesystem.service.impl.ArchiveServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private DigitalFileMapper digitalFileMapper;

    @Mock
    private FondsMapper fondsMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private RetentionPeriodMapper retentionPeriodMapper;

    @Mock
    private PushRecordMapper pushRecordMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private MetricsConfig metricsConfig;

    @Mock
    private ConfigService configService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ArchiveServiceImpl archiveService;

    private Archive testArchive;
    private Fonds testFonds;

    @BeforeEach
    void setUp() {
        testFonds = new Fonds();
        testFonds.setId(1L);
        testFonds.setFondsNo("QZ001");
        testFonds.setFondsName("测试全宗");

        testArchive = new Archive();
        testArchive.setId(1L);
        testArchive.setArchiveNo("ARC-20260213-0001");
        testArchive.setFondsId(1L);
        testArchive.setFondsNo("QZ001");
        testArchive.setArchiveType("DOCUMENT");
        testArchive.setTitle("测试档案");
        testArchive.setStatus(Archive.STATUS_RECEIVED);
        testArchive.setReceivedAt(LocalDateTime.now());
        testArchive.setHasElectronic(true);
        testArchive.setFileCount(0);
        testArchive.setTotalFileSize(0L);
        testArchive.setDeleted(false);
    }

    @Test
    void testReceive_NewArchive_Success() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("123");
        request.setTitle("新档案");
        request.setArchiveType("DOCUMENT");
        request.setAsync(false);

        when(archiveMapper.selectBySourceId("LAW_FIRM", "123")).thenReturn(null);
        when(fondsMapper.selectByFondsNo("QZ001")).thenReturn(testFonds);
        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("ARC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        ArchiveReceiveResponse response = archiveService.receive(request);

        assertNotNull(response);
        assertEquals(Archive.STATUS_RECEIVED, response.getStatus());
        verify(archiveMapper).insert(any(Archive.class));
    }

    @Test
    void testReceive_ExistingArchive_ReturnsExisting() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("123");

        when(archiveMapper.selectBySourceId("LAW_FIRM", "123")).thenReturn(testArchive);

        ArchiveReceiveResponse response = archiveService.receive(request);

        assertNotNull(response);
        assertEquals(testArchive.getArchiveNo(), response.getArchiveNo());
        assertEquals("档案已存在", response.getMessage());
        verify(archiveMapper, never()).insert(any());
    }

    @Test
    void testGetById_Success() {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Collections.emptyList());

        ArchiveDTO result = archiveService.getById(1L);

        assertNotNull(result);
        assertEquals("测试档案", result.getTitle());
        assertEquals("ARC-20260213-0001", result.getArchiveNo());
    }

    @Test
    void testGetById_NotFound() {
        when(archiveMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> archiveService.getById(999L));
    }

    @Test
    void testGetByArchiveNo_Success() {
        when(archiveMapper.selectByArchiveNo("ARC-20260213-0001")).thenReturn(testArchive);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Collections.emptyList());

        ArchiveDTO result = archiveService.getByArchiveNo("ARC-20260213-0001");

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetByArchiveNo_NotFound() {
        when(archiveMapper.selectByArchiveNo("NOT-EXISTS")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> archiveService.getByArchiveNo("NOT-EXISTS"));
    }

    @Test
    void testQuery_WithKeyword() {
        ArchiveQueryRequest request = new ArchiveQueryRequest();
        request.setKeyword("测试");
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Archive> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testArchive));
        page.setTotal(1);

        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<ArchiveDTO> result = archiveService.query(request);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void testQuery_WithFilters() {
        ArchiveQueryRequest request = new ArchiveQueryRequest();
        request.setArchiveType("DOCUMENT");
        request.setStatus("RECEIVED");
        request.setFondsId(1L);
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Archive> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testArchive));
        page.setTotal(1);

        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<ArchiveDTO> result = archiveService.query(request);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void testQuery_WithDateRange() {
        ArchiveQueryRequest request = new ArchiveQueryRequest();
        request.setArchiveDateStart(LocalDate.of(2026, 1, 1));
        request.setArchiveDateEnd(LocalDate.of(2026, 12, 31));
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Archive> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<ArchiveDTO> result = archiveService.query(request);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    void testDelete_Success() {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> archiveService.delete(1L));

        verify(archiveMapper).deleteById(1L);
    }

    @Test
    void testDelete_NotFound() {
        when(archiveMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> archiveService.delete(999L));
    }

    @Test
    void testUpdateStatus_Success() {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);

        assertDoesNotThrow(() -> archiveService.updateStatus(1L, Archive.STATUS_STORED));

        verify(archiveMapper).updateById(any(Archive.class));
    }

    @Test
    void testUpdateStatus_NotFound() {
        when(archiveMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> archiveService.updateStatus(999L, Archive.STATUS_STORED));
    }

    @Test
    void testGenerateArchiveNo() {
        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("DOC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        String archiveNo = archiveService.generateArchiveNo("DOCUMENT");

        assertNotNull(archiveNo);
        assertTrue(archiveNo.startsWith("DOC-"));
        assertTrue(archiveNo.contains("-0005"));
    }

    @Test
    void testGenerateArchiveNo_RedisUnavailable() {
        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("DOC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis不可用"));

        String archiveNo = archiveService.generateArchiveNo("DOCUMENT");

        assertNotNull(archiveNo);
        assertTrue(archiveNo.startsWith("DOC-"));
    }

    @Test
    void testCreate_Success() {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("新创建的档案");
        request.setArchiveType("DOCUMENT");
        request.setFondsId(1L);

        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("ARC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);
        when(archiveMapper.insert(any(Archive.class))).thenAnswer(invocation -> {
            Archive archive = invocation.getArgument(0);
            archive.setId(2L);
            return 1;
        });
        
        Archive newArchive = new Archive();
        newArchive.setId(2L);
        newArchive.setTitle("新创建的档案");
        newArchive.setArchiveType("DOCUMENT");
        newArchive.setFondsId(1L);
        newArchive.setFondsNo("QZ001");
        newArchive.setStatus(Archive.STATUS_RECEIVED);
        newArchive.setHasElectronic(false);
        newArchive.setDeleted(false);
        when(archiveMapper.selectById(2L)).thenReturn(newArchive);

        ArchiveDTO result = archiveService.create(request);

        assertNotNull(result);
        assertEquals("新创建的档案", result.getTitle());
    }

    @Test
    void testUpdate_Success() {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("更新后的档案");
        request.setArchiveType("DOCUMENT");

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Collections.emptyList());

        ArchiveDTO result = archiveService.update(1L, request);

        assertNotNull(result);
        verify(archiveMapper).updateById(any(Archive.class));
    }

    @Test
    void testUpdate_NotFound() {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("更新后的档案");

        when(archiveMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> archiveService.update(999L, request));
    }

    @Test
    void testReceive_WithFiles_Async() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("456");
        request.setTitle("带文件的档案");
        request.setArchiveType("DOCUMENT");
        request.setAsync(true);

        ArchiveReceiveRequest.FileInfo fileInfo = new ArchiveReceiveRequest.FileInfo();
        fileInfo.setFileName("test.pdf");
        fileInfo.setDownloadUrl("http://example.com/test.pdf");
        request.setFiles(Arrays.asList(fileInfo));

        when(archiveMapper.selectBySourceId("LAW_FIRM", "456")).thenReturn(null);
        when(fondsMapper.selectByFondsNo("QZ001")).thenReturn(testFonds);
        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("ARC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(2L);
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        ArchiveReceiveResponse response = archiveService.receive(request);

        assertNotNull(response);
        assertEquals(Archive.STATUS_PROCESSING, response.getStatus());
        assertEquals("接收成功，文件正在异步处理中", response.getMessage());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void testReceive_WithRetentionPeriod() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("789");
        request.setTitle("带保管期限的档案");
        request.setArchiveType("DOCUMENT");
        request.setRetentionPeriod("Y10");
        request.setAsync(false);

        RetentionPeriod retention = new RetentionPeriod();
        retention.setPeriodCode("Y10");
        retention.setPeriodYears(10);

        when(archiveMapper.selectBySourceId("LAW_FIRM", "789")).thenReturn(null);
        when(fondsMapper.selectByFondsNo("QZ001")).thenReturn(testFonds);
        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("ARC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(3L);
        when(retentionPeriodMapper.selectByPeriodCode("Y10")).thenReturn(retention);
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        ArchiveReceiveResponse response = archiveService.receive(request);

        assertNotNull(response);
        verify(archiveMapper).insert(argThat(archive -> 
            archive.getRetentionExpireDate() != null && 
            archive.getRetentionExpireDate().equals(LocalDate.now().plusYears(10))
        ));
    }

    @Test
    void testCreate_WithCategoryAndRetention() {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("带分类的档案");
        request.setArchiveType("DOCUMENT");
        request.setFondsId(1L);
        request.setCategoryId(1L);
        request.setRetentionPeriod("Y10");
        request.setArchiveDate(LocalDate.of(2026, 1, 1));

        Category testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setCategoryCode("CAT001");

        RetentionPeriod retention = new RetentionPeriod();
        retention.setPeriodCode("Y10");
        retention.setPeriodYears(10);

        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("ARC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(retentionPeriodMapper.selectByPeriodCode("Y10")).thenReturn(retention);
        when(archiveMapper.insert(any(Archive.class))).thenAnswer(invocation -> {
            Archive archive = invocation.getArgument(0);
            archive.setId(3L);
            return 1;
        });
        
        Archive newArchive = new Archive();
        newArchive.setId(3L);
        newArchive.setTitle("带分类的档案");
        newArchive.setArchiveType("DOCUMENT");
        newArchive.setFondsId(1L);
        newArchive.setFondsNo("QZ001");
        newArchive.setCategoryId(1L);
        newArchive.setCategoryCode("CAT001");
        newArchive.setStatus(Archive.STATUS_RECEIVED);
        newArchive.setHasElectronic(false);
        newArchive.setDeleted(false);
        when(archiveMapper.selectById(3L)).thenReturn(newArchive);

        ArchiveDTO result = archiveService.create(request);

        assertNotNull(result);
        verify(categoryMapper).selectById(1L);
    }

    @Test
    void testReceive_SyncWithFiles() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("sync001");
        request.setTitle("同步处理档案");
        request.setArchiveType("DOCUMENT");
        request.setAsync(false);

        ArchiveReceiveRequest.FileInfo fileInfo = new ArchiveReceiveRequest.FileInfo();
        fileInfo.setFileName("test.pdf");
        fileInfo.setDownloadUrl("http://example.com/test.pdf");
        request.setFiles(Arrays.asList(fileInfo));

        DigitalFile downloadedFile = new DigitalFile();
        downloadedFile.setId(1L);
        downloadedFile.setFileSize(1024L);

        when(archiveMapper.selectBySourceId("LAW_FIRM", "sync001")).thenReturn(null);
        when(fondsMapper.selectByFondsNo("QZ001")).thenReturn(testFonds);
        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("ARC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(4L);
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(any(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(downloadedFile);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);

        ArchiveReceiveResponse response = archiveService.receive(request);

        assertNotNull(response);
        assertEquals(Archive.STATUS_RECEIVED, response.getStatus());
        assertEquals(1, response.getFileCount());
        verify(fileStorageService).downloadAndStore(any(), eq("http://example.com/test.pdf"), 
                eq("test.pdf"), any(), eq(1));
    }

    @Test
    void testReceive_SyncWithFiles_DownloadFails() {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("sync002");
        request.setTitle("下载失败的档案");
        request.setArchiveType("DOCUMENT");
        request.setAsync(false);

        ArchiveReceiveRequest.FileInfo fileInfo = new ArchiveReceiveRequest.FileInfo();
        fileInfo.setFileName("fail.pdf");
        fileInfo.setDownloadUrl("http://example.com/fail.pdf");
        request.setFiles(Arrays.asList(fileInfo));

        when(archiveMapper.selectBySourceId("LAW_FIRM", "sync002")).thenReturn(null);
        when(fondsMapper.selectByFondsNo("QZ001")).thenReturn(testFonds);
        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("ARC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(any(), anyString(), anyString(), any(), anyInt()))
                .thenThrow(new RuntimeException("下载失败"));
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);

        ArchiveReceiveResponse response = archiveService.receive(request);

        assertNotNull(response);
        assertEquals(0, response.getFileCount()); // 下载失败，文件数为0
    }

    @Test
    void testQuery_WithMoreFilters() {
        ArchiveQueryRequest request = new ArchiveQueryRequest();
        request.setArchiveNo("ARC-20260213-0001");
        request.setCategoryId(1L);
        request.setRetentionPeriod("PERMANENT");
        request.setSecurityLevel("INTERNAL");
        request.setSourceType("LAW_FIRM");
        request.setCaseNo("CASE001");
        request.setCreatedAtStart(LocalDate.of(2026, 1, 1));
        request.setCreatedAtEnd(LocalDate.of(2026, 12, 31));
        request.setSortOrder("asc");
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Archive> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testArchive));
        page.setTotal(1);

        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<ArchiveDTO> result = archiveService.query(request);

        assertNotNull(result);
    }

    @Test
    void testCreate_WithFileIds() {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("带文件的档案");
        request.setArchiveType("DOCUMENT");
        request.setFondsId(1L);
        request.setFileIds(Arrays.asList(1L, 2L));

        DigitalFile file1 = new DigitalFile();
        file1.setId(1L);
        file1.setArchiveId(null);
        file1.setFileSize(1024L);

        DigitalFile file2 = new DigitalFile();
        file2.setId(2L);
        file2.setArchiveId(null);
        file2.setFileSize(2048L);

        when(configService.getArchiveNoPrefix("DOCUMENT")).thenReturn("ARC");
        when(configService.getValue("archive.no.date.format", "yyyyMMdd")).thenReturn("yyyyMMdd");
        when(configService.getIntValue("archive.no.seq.digits", 4)).thenReturn(4);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(fondsMapper.selectById(1L)).thenReturn(testFonds);
        when(archiveMapper.insert(any(Archive.class))).thenAnswer(invocation -> {
            Archive archive = invocation.getArgument(0);
            archive.setId(4L);
            return 1;
        });
        when(digitalFileMapper.selectById(1L)).thenReturn(file1);
        when(digitalFileMapper.selectById(2L)).thenReturn(file2);
        when(digitalFileMapper.updateById(any(DigitalFile.class))).thenReturn(1);
        
        Archive newArchive = new Archive();
        newArchive.setId(4L);
        newArchive.setTitle("带文件的档案");
        newArchive.setArchiveType("DOCUMENT");
        newArchive.setStatus(Archive.STATUS_RECEIVED);
        newArchive.setHasElectronic(true);
        newArchive.setFileCount(2);
        newArchive.setTotalFileSize(3072L);
        newArchive.setDeleted(false);
        when(archiveMapper.selectById(4L)).thenReturn(newArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);

        ArchiveDTO result = archiveService.create(request);

        assertNotNull(result);
        verify(digitalFileMapper, times(2)).updateById(any(DigitalFile.class));
    }

    @Test
    void testGetById_WithFiles() {
        testArchive.setHasElectronic(true);
        
        DigitalFile file = new DigitalFile();
        file.setId(1L);
        file.setArchiveId(1L);
        file.setFileName("test.pdf");
        file.setFileSize(1024L);
        
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Arrays.asList(file));

        ArchiveDTO result = archiveService.getById(1L);

        assertNotNull(result);
        assertNotNull(result.getFiles());
        assertEquals(1, result.getFiles().size());
    }
}
