package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DestructionRecord;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.entity.RetentionPeriod;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.DestructionRecordMapper;
import com.archivesystem.repository.OperationLogMapper;
import com.archivesystem.repository.RetentionPeriodMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.impl.RetentionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetentionServiceTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private RetentionPeriodMapper retentionPeriodMapper;

    @Mock
    private OperationLogMapper operationLogMapper;

    @Mock
    private DestructionRecordMapper destructionRecordMapper;

    @InjectMocks
    private RetentionServiceImpl retentionService;

    private Archive testArchive;
    private RetentionPeriod testRetention;

    @BeforeEach
    void setUp() {
        testArchive = new Archive();
        testArchive.setId(1L);
        testArchive.setArchiveNo("ARC-20260213-0001");
        testArchive.setTitle("测试档案");
        testArchive.setStatus(Archive.STATUS_STORED);
        testArchive.setRetentionPeriod("Y10");
        testArchive.setRetentionExpireDate(LocalDate.now().plusYears(10));
        testArchive.setArchiveDate(LocalDate.now());
        testArchive.setDeleted(false);

        testRetention = new RetentionPeriod();
        testRetention.setId(1L);
        testRetention.setPeriodCode("Y10");
        testRetention.setPeriodName("10年");
        testRetention.setPeriodYears(10);
    }

    @Test
    void testFindExpiringArchives() {
        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList(testArchive));

        List<Archive> result = retentionService.findExpiringArchives(30);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindExpiringArchives_Empty() {
        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList());

        List<Archive> result = retentionService.findExpiringArchives(30);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindExpiredArchives() {
        testArchive.setRetentionExpireDate(LocalDate.now().minusDays(30));
        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList(testArchive));

        List<Archive> result = retentionService.findExpiredArchives();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindExpiredArchives_Empty() {
        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList());

        List<Archive> result = retentionService.findExpiredArchives();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExtendRetention_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("管理员");

            RetentionPeriod newRetention = new RetentionPeriod();
            newRetention.setPeriodCode("Y30");
            newRetention.setPeriodYears(30);

            when(archiveMapper.selectById(1L)).thenReturn(testArchive);
            when(retentionPeriodMapper.selectByPeriodCode("Y30")).thenReturn(newRetention);
            when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
            when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                retentionService.extendRetention(1L, "Y30", "延长保管"));

            verify(archiveMapper).updateById(argThat(archive -> 
                "Y30".equals(archive.getRetentionPeriod())));
            verify(operationLogMapper).insert(any(OperationLog.class));
        }
    }

    @Test
    void testExtendRetention_ArchiveNotFound() {
        when(archiveMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> 
            retentionService.extendRetention(999L, "Y30", "延长保管"));
    }

    @Test
    void testExtendRetention_InvalidRetentionPeriod() {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(retentionPeriodMapper.selectByPeriodCode("INVALID")).thenReturn(null);

        assertThrows(BusinessException.class, () -> 
            retentionService.extendRetention(1L, "INVALID", "延长保管"));
    }

    @Test
    void testExtendRetention_ToPermanent() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("管理员");

            RetentionPeriod permanent = new RetentionPeriod();
            permanent.setPeriodCode("PERMANENT");
            permanent.setPeriodYears(null); // 永久

            when(archiveMapper.selectById(1L)).thenReturn(testArchive);
            when(retentionPeriodMapper.selectByPeriodCode("PERMANENT")).thenReturn(permanent);
            when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
            when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                retentionService.extendRetention(1L, "PERMANENT", "改为永久保管"));

            verify(archiveMapper).updateById(argThat(archive -> 
                "PERMANENT".equals(archive.getRetentionPeriod()) &&
                archive.getRetentionExpireDate() == null));
        }
    }

    @Test
    void testApplyForDestruction_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("管理员");

            when(archiveMapper.selectById(1L)).thenReturn(testArchive);
            when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
            when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                retentionService.applyForDestruction(1L, "已过保管期限"));

            verify(archiveMapper).updateById(argThat(archive -> 
                Archive.STATUS_APPRAISAL.equals(archive.getStatus())));
        }
    }

    @Test
    void testApplyForDestruction_ArchiveNotFound() {
        when(archiveMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> 
            retentionService.applyForDestruction(999L, "已过保管期限"));
    }

    @Test
    void testApplyForDestruction_AlreadyDestroyed() {
        testArchive.setStatus(Archive.STATUS_DESTROYED);
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);

        assertThrows(BusinessException.class, () -> 
            retentionService.applyForDestruction(1L, "已过保管期限"));
    }

    @Test
    void testExecuteDestruction_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("管理员");

            testArchive.setStatus(Archive.STATUS_APPRAISAL);
            
            DestructionRecord record = new DestructionRecord();
            record.setId(2L);
            record.setArchiveId(1L);
            record.setStatus(DestructionRecord.STATUS_APPROVED);
            
            when(archiveMapper.selectById(1L)).thenReturn(testArchive);
            when(destructionRecordMapper.selectOne(any())).thenReturn(record);
            when(destructionRecordMapper.updateById(any())).thenReturn(1);
            when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
            when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                retentionService.executeDestruction(1L, 2L, "审批通过，执行销毁"));

            verify(archiveMapper).updateById(argThat(archive -> 
                Archive.STATUS_DESTROYED.equals(archive.getStatus())));
        }
    }

    @Test
    void testExecuteDestruction_ArchiveNotFound() {
        when(archiveMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> 
            retentionService.executeDestruction(999L, 2L, "审批通过"));
    }

    @Test
    void testExecuteDestruction_NotInAppraisalStatus() {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);

        assertThrows(BusinessException.class, () -> 
            retentionService.executeDestruction(1L, 2L, "审批通过"));
    }
}
