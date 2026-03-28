package com.archivesystem.service;

import com.archivesystem.TestLambdaCacheInitializer;
import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.AppraisalRecord;
import com.archivesystem.entity.Archive;
import com.archivesystem.repository.AppraisalRecordMapper;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.impl.AppraisalServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AppraisalService测试类.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AppraisalServiceTest {

    @BeforeAll
    static void initLambdaCache() {
        TestLambdaCacheInitializer.initTableInfo(AppraisalRecord.class, Archive.class);
    }

    @Mock
    private AppraisalRecordMapper appraisalMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @InjectMocks
    private AppraisalServiceImpl appraisalService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        securityUtilsMock.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Test
    void testCreate_Success() {
        // Given
        Long archiveId = 1L;
        Archive archive = new Archive();
        archive.setId(archiveId);
        when(archiveMapper.selectById(archiveId)).thenReturn(archive);
        when(appraisalMapper.insert(any())).thenReturn(1);

        // When
        AppraisalRecord result = appraisalService.create(archiveId, 
                AppraisalRecord.TYPE_VALUE, "原值", "新值", "鉴定原因");

        // Then
        assertNotNull(result);
        assertEquals(archiveId, result.getArchiveId());
        assertEquals(AppraisalRecord.TYPE_VALUE, result.getAppraisalType());
        assertEquals(AppraisalRecord.STATUS_PENDING, result.getStatus());
        verify(appraisalMapper).insert(any());
    }

    @Test
    void testCreate_ArchiveNotFound_ShouldThrowException() {
        // Given
        Long archiveId = 999L;
        when(archiveMapper.selectById(archiveId)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class, () -> 
                appraisalService.create(archiveId, AppraisalRecord.TYPE_VALUE, "原值", "新值", "原因"));
    }

    @Test
    void testCreate_InvalidType_ShouldThrowException() {
        // Given
        Long archiveId = 1L;
        Archive archive = new Archive();
        when(archiveMapper.selectById(archiveId)).thenReturn(archive);

        // When & Then
        assertThrows(BusinessException.class, () -> 
                appraisalService.create(archiveId, "INVALID_TYPE", "原值", "新值", "原因"));
    }

    @Test
    void testGetById_Success() {
        // Given
        Long id = 1L;
        AppraisalRecord record = new AppraisalRecord();
        record.setId(id);
        when(appraisalMapper.selectById(id)).thenReturn(record);

        // When
        AppraisalRecord result = appraisalService.getById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void testGetById_NotFound_ShouldThrowException() {
        // Given
        Long id = 999L;
        when(appraisalMapper.selectById(id)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class, () -> appraisalService.getById(id));
    }

    @Test
    void testGetList_WithFilters() {
        // Given
        List<AppraisalRecord> records = Arrays.asList(new AppraisalRecord(), new AppraisalRecord());
        Page<AppraisalRecord> page = new Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(2);
        when(appraisalMapper.selectPage(any(), any())).thenReturn(page);

        // When
        PageResult<AppraisalRecord> result = appraisalService.getList(
                AppraisalRecord.TYPE_VALUE, AppraisalRecord.STATUS_PENDING, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    void testGetList_WithoutFilters() {
        // Given
        List<AppraisalRecord> records = Arrays.asList(new AppraisalRecord());
        Page<AppraisalRecord> page = new Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(1);
        when(appraisalMapper.selectPage(any(), any())).thenReturn(page);

        // When
        PageResult<AppraisalRecord> result = appraisalService.getList(null, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testGetPendingList() {
        // Given
        List<AppraisalRecord> records = Arrays.asList(new AppraisalRecord());
        Page<AppraisalRecord> page = new Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(1);
        when(appraisalMapper.selectPage(any(), any())).thenReturn(page);

        // When
        PageResult<AppraisalRecord> result = appraisalService.getPendingList(1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testGetByArchiveId() {
        // Given
        Long archiveId = 1L;
        List<AppraisalRecord> records = Arrays.asList(new AppraisalRecord());
        when(appraisalMapper.selectByArchiveId(archiveId)).thenReturn(records);

        // When
        List<AppraisalRecord> result = appraisalService.getByArchiveId(archiveId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testApprove_Success() {
        // Given
        Long id = 1L;
        AppraisalRecord record = new AppraisalRecord();
        record.setId(id);
        record.setArchiveId(1L);
        record.setStatus(AppraisalRecord.STATUS_PENDING);
        record.setAppraisalType(AppraisalRecord.TYPE_VALUE);
        when(appraisalMapper.selectById(id)).thenReturn(record);
        when(appraisalMapper.update(isNull(), any())).thenReturn(1);

        Archive archive = new Archive();
        archive.setId(1L);
        when(archiveMapper.selectById(1L)).thenReturn(archive);
        when(archiveMapper.updateById(any())).thenReturn(1);

        // When
        appraisalService.approve(id, "同意");

        // Then
        verify(appraisalMapper).update(isNull(), any());
    }

    @Test
    void testApprove_NotPending_ShouldThrowException() {
        // Given
        Long id = 1L;
        AppraisalRecord record = new AppraisalRecord();
        record.setId(id);
        record.setStatus(AppraisalRecord.STATUS_APPROVED);
        when(appraisalMapper.selectById(id)).thenReturn(record);

        // When & Then
        assertThrows(BusinessException.class, () -> appraisalService.approve(id, "同意"));
    }

    @Test
    void testReject_Success() {
        // Given
        Long id = 1L;
        AppraisalRecord record = new AppraisalRecord();
        record.setId(id);
        record.setStatus(AppraisalRecord.STATUS_PENDING);
        when(appraisalMapper.selectById(id)).thenReturn(record);
        when(appraisalMapper.update(isNull(), any())).thenReturn(1);

        // When
        appraisalService.reject(id, "拒绝");

        // Then
        verify(appraisalMapper).update(isNull(), any());
    }

    @Test
    void testReject_NotPending_ShouldThrowException() {
        // Given
        Long id = 1L;
        AppraisalRecord record = new AppraisalRecord();
        record.setId(id);
        record.setStatus(AppraisalRecord.STATUS_REJECTED);
        when(appraisalMapper.selectById(id)).thenReturn(record);

        // When & Then
        assertThrows(BusinessException.class, () -> appraisalService.reject(id, "拒绝"));
    }

    @Test
    void testApprove_SecurityAppraisal_ShouldUpdateArchive() {
        // Given
        Long id = 1L;
        AppraisalRecord record = new AppraisalRecord();
        record.setId(id);
        record.setArchiveId(1L);
        record.setStatus(AppraisalRecord.STATUS_PENDING);
        record.setAppraisalType(AppraisalRecord.TYPE_SECURITY);
        record.setNewValue("SECRET");
        
        AppraisalRecord approvedRecord = new AppraisalRecord();
        approvedRecord.setId(id);
        approvedRecord.setArchiveId(1L);
        approvedRecord.setStatus(AppraisalRecord.STATUS_APPROVED);
        approvedRecord.setAppraisalType(AppraisalRecord.TYPE_SECURITY);
        approvedRecord.setNewValue("SECRET");
        
        when(appraisalMapper.selectById(id)).thenReturn(record).thenReturn(approvedRecord);
        when(appraisalMapper.update(isNull(), any())).thenReturn(1);
        
        Archive archive = new Archive();
        archive.setId(1L);
        when(archiveMapper.selectById(1L)).thenReturn(archive);
        when(archiveMapper.updateById(any())).thenReturn(1);

        // When
        appraisalService.approve(id, "同意");

        // Then
        verify(archiveMapper).updateById(argThat(a -> "SECRET".equals(a.getSecurityLevel())));
    }

    @Test
    void testApprove_RetentionAppraisal_ShouldUpdateArchive() {
        // Given
        Long id = 1L;
        AppraisalRecord record = new AppraisalRecord();
        record.setId(id);
        record.setArchiveId(1L);
        record.setStatus(AppraisalRecord.STATUS_PENDING);
        record.setAppraisalType(AppraisalRecord.TYPE_RETENTION);
        record.setNewValue("Y30");
        
        AppraisalRecord approvedRecord = new AppraisalRecord();
        approvedRecord.setId(id);
        approvedRecord.setArchiveId(1L);
        approvedRecord.setStatus(AppraisalRecord.STATUS_APPROVED);
        approvedRecord.setAppraisalType(AppraisalRecord.TYPE_RETENTION);
        approvedRecord.setNewValue("Y30");
        
        when(appraisalMapper.selectById(id)).thenReturn(record).thenReturn(approvedRecord);
        when(appraisalMapper.update(isNull(), any())).thenReturn(1);
        
        Archive archive = new Archive();
        archive.setId(1L);
        archive.setArchiveDate(LocalDate.now());
        when(archiveMapper.selectById(1L)).thenReturn(archive);
        when(archiveMapper.updateById(any())).thenReturn(1);

        // When
        appraisalService.approve(id, "同意");

        // Then
        verify(archiveMapper).updateById(argThat(a -> 
                "Y30".equals(a.getRetentionPeriod()) && a.getRetentionExpireDate() != null));
    }

    @Test
    void testApprove_OpenAppraisal_ShouldUpdateArchive() {
        // Given
        Long id = 1L;
        AppraisalRecord record = new AppraisalRecord();
        record.setId(id);
        record.setArchiveId(1L);
        record.setStatus(AppraisalRecord.STATUS_PENDING);
        record.setAppraisalType(AppraisalRecord.TYPE_OPEN);
        record.setNewValue("OPEN");
        
        AppraisalRecord approvedRecord = new AppraisalRecord();
        approvedRecord.setId(id);
        approvedRecord.setArchiveId(1L);
        approvedRecord.setStatus(AppraisalRecord.STATUS_APPROVED);
        approvedRecord.setAppraisalType(AppraisalRecord.TYPE_OPEN);
        approvedRecord.setNewValue("OPEN");
        
        when(appraisalMapper.selectById(id)).thenReturn(record).thenReturn(approvedRecord);
        when(appraisalMapper.update(isNull(), any())).thenReturn(1);
        
        Archive archive = new Archive();
        archive.setId(1L);
        when(archiveMapper.selectById(1L)).thenReturn(archive);
        when(archiveMapper.updateById(any())).thenReturn(1);

        // When
        appraisalService.approve(id, "同意");

        // Then
        verify(archiveMapper).updateById(argThat(a -> 
                Archive.SECURITY_PUBLIC.equals(a.getSecurityLevel())));
    }

    @Test
    void testCreate_AllValidTypes() {
        // Given
        Long archiveId = 1L;
        Archive archive = new Archive();
        archive.setId(archiveId);
        when(archiveMapper.selectById(archiveId)).thenReturn(archive);
        when(appraisalMapper.insert(any())).thenReturn(1);

        String[] validTypes = {
                AppraisalRecord.TYPE_VALUE,
                AppraisalRecord.TYPE_SECURITY,
                AppraisalRecord.TYPE_OPEN,
                AppraisalRecord.TYPE_RETENTION
        };

        // When & Then
        for (String type : validTypes) {
            assertDoesNotThrow(() -> 
                    appraisalService.create(archiveId, type, "原值", "新值", "原因"));
        }
    }
}
