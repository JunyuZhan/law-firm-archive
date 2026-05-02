package com.archivesystem.service;

import com.archivesystem.dto.backup.BackupSetResponse;
import com.archivesystem.dto.backup.BackupTargetRequest;
import com.archivesystem.dto.backup.BackupTargetResponse;
import com.archivesystem.dto.backup.RestoreExecuteRequest;
import com.archivesystem.dto.backup.RestoreJobResponse;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.entity.BackupJob;
import com.archivesystem.entity.BackupTarget;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.entity.RestoreJob;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.repository.BackupJobMapper;
import com.archivesystem.repository.BackupTargetMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.repository.RestoreJobMapper;
import com.archivesystem.repository.SysConfigMapper;
import com.archivesystem.security.RuntimeSecretProvider;
import com.archivesystem.security.SecretCryptoService;
import com.archivesystem.service.impl.BackupServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * @author junyuzhan
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BackupTargetMapper backupTargetMapper;

    @Mock
    private BackupJobMapper backupJobMapper;

    @Mock
    private RestoreJobMapper restoreJobMapper;

    @Mock
    private SysConfigMapper sysConfigMapper;

    @Mock
    private DigitalFileMapper digitalFileMapper;

    @Mock
    private MinioService minioService;

    @Mock
    private SecretCryptoService secretCryptoService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ConfigService configService;

    @Mock
    private ArchiveIndexService archiveIndexService;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private SmbStorageService smbStorageService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BackupServiceImpl backupService;

    @Test
    void testGetOverview() {
        when(backupTargetMapper.selectCount(any())).thenReturn(2L, 1L);
        when(backupJobMapper.selectCount(any())).thenReturn(3L, 0L);
        when(restoreJobMapper.selectCount(any())).thenReturn(0L);

        var result = backupService.getOverview();

        assertEquals(2L, result.getEnabledTargetCount());
        assertEquals(1L, result.getVerifiedTargetCount());
        assertEquals(3L, result.getPendingBackupJobs());
        assertEquals("RUNNING", result.getCurrentPhase());
    }

    @Test
    void testGetOverview_ShouldMarkSetupRequiredWhenNoEnabledTargets() {
        when(backupTargetMapper.selectCount(any())).thenReturn(0L, 0L);
        when(backupJobMapper.selectCount(any())).thenReturn(0L, 0L);
        when(restoreJobMapper.selectCount(any())).thenReturn(0L);

        var result = backupService.getOverview();

        assertEquals("SETUP_REQUIRED", result.getCurrentPhase());
    }

    @Test
    void testGetOverview_ShouldMarkVerifyRequiredWhenTargetsNotFullyVerified() {
        when(backupTargetMapper.selectCount(any())).thenReturn(2L, 1L);
        when(backupJobMapper.selectCount(any())).thenReturn(0L, 0L);
        when(restoreJobMapper.selectCount(any())).thenReturn(0L);

        var result = backupService.getOverview();

        assertEquals("VERIFY_REQUIRED", result.getCurrentPhase());
    }

    @Test
    void testGetOverview_ShouldMarkReadyWhenAllTargetsVerifiedAndNoRunningJobs() {
        when(backupTargetMapper.selectCount(any())).thenReturn(2L, 2L);
        when(backupJobMapper.selectCount(any())).thenReturn(0L, 0L);
        when(restoreJobMapper.selectCount(any())).thenReturn(0L);

        var result = backupService.getOverview();

        assertEquals("READY", result.getCurrentPhase());
    }

    @Test
    void testCreateLocalTarget() {
        BackupTargetRequest request = new BackupTargetRequest();
        request.setName("本地备份");
        request.setTargetType(BackupTarget.TYPE_LOCAL);
        request.setEnabled(true);
        request.setLocalPath("/tmp/archive-backups");

        BackupTargetResponse response = backupService.createTarget(request);

        assertEquals("本地备份", response.getName());
        assertEquals(BackupTarget.TYPE_LOCAL, response.getTargetType());
        assertEquals("/tmp/archive-backups", response.getLocalPath());
        assertEquals("/tmp/archive-backups", response.getDisplayAddress());
        assertEquals(BackupTarget.VERIFY_PENDING, response.getVerifyStatus());
    }

    @Test
    void testGetTarget_ShouldBuildSmbDisplayAddressFromConnectionFields() {
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbPort(1445)
                .smbShare("archive")
                .smbSubPath("nightly/full")
                .verifyStatus(BackupTarget.VERIFY_SUCCESS)
                .build();

        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        BackupTargetResponse response = backupService.getTarget(1L);

        assertEquals("smb://192.168.50.5:1445/archive/nightly/full", response.getDisplayAddress());
        assertEquals(1445, response.getSmbPort());
    }

    @Test
    void testGetTarget_ShouldOmitDefaultSmbPortFromDisplayAddress() {
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbPort(445)
                .smbShare("archive")
                .smbSubPath("nightly/full")
                .verifyStatus(BackupTarget.VERIFY_SUCCESS)
                .build();

        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        BackupTargetResponse response = backupService.getTarget(1L);

        assertEquals("\\\\192.168.50.5\\archive\\nightly\\full", response.getDisplayAddress());
    }

    @Test
    void testCreateSmbTarget_ShouldRequireCredentials() {
        BackupTargetRequest request = new BackupTargetRequest();
        request.setName("NAS");
        request.setTargetType(BackupTarget.TYPE_SMB);
        request.setEnabled(true);
        request.setSmbHost("192.168.50.5");
        request.setSmbShare("archive");

        assertThrows(BusinessException.class, () -> backupService.createTarget(request));
    }

    @Test
    void testUpdateSmbTarget_ShouldPreserveExistingConnectionFieldsWhenOmitted() {
        BackupTarget existing = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbPort(445)
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .smbSubPath("nightly")
                .remarks("old")
                .build();
        BackupTargetRequest request = new BackupTargetRequest();
        request.setName("NAS-UPDATED");
        request.setTargetType(BackupTarget.TYPE_SMB);
        request.setEnabled(false);
        request.setRemarks("new");

        when(backupTargetMapper.selectById(1L)).thenReturn(existing);

        BackupTargetResponse response = backupService.updateTarget(1L, request);

        assertEquals("NAS-UPDATED", response.getName());
        assertFalse(response.getEnabled());
        assertEquals("192.168.50.5", existing.getSmbHost());
        assertEquals(445, existing.getSmbPort());
        assertEquals("archive", existing.getSmbShare());
        assertEquals("backup", existing.getSmbUsername());
        assertEquals("encrypted", existing.getSmbPasswordEncrypted());
        assertEquals("nightly", existing.getSmbSubPath());
        assertEquals("new", existing.getRemarks());
    }

    @Test
    void testUpdateTarget_ToLocalShouldClearSmbFields() {
        BackupTarget existing = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbPort(445)
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .smbSubPath("nightly")
                .build();
        BackupTargetRequest request = new BackupTargetRequest();
        request.setName("本地备份");
        request.setTargetType(BackupTarget.TYPE_LOCAL);
        request.setEnabled(true);
        request.setLocalPath("/tmp/archive-backups");

        when(backupTargetMapper.selectById(1L)).thenReturn(existing);

        BackupTargetResponse response = backupService.updateTarget(1L, request);

        assertEquals(BackupTarget.TYPE_LOCAL, response.getTargetType());
        assertEquals("/tmp/archive-backups", existing.getLocalPath());
        assertNull(existing.getSmbHost());
        assertNull(existing.getSmbPort());
        assertNull(existing.getSmbShare());
        assertNull(existing.getSmbUsername());
        assertNull(existing.getSmbPasswordEncrypted());
        assertNull(existing.getSmbSubPath());
    }

    @Test
    void testVerifyLocalTargetSuccess() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-target-test");
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();

        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        BackupTargetResponse response = backupService.verifyTarget(1L);

        assertEquals(BackupTarget.VERIFY_SUCCESS, response.getVerifyStatus());
        assertTrue(response.getVerifyMessage().contains("验证通过"));
    }

    @Test
    void testGetEmptyPages() {
        when(backupJobMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));
        when(restoreJobMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));

        assertNotNull(backupService.getBackupJobs(1, 10));
        assertNotNull(backupService.getRestoreJobs(1, 10));
    }

    @Test
    void testGetJobs_ShouldNormalizeInvalidPagination() {
        when(backupJobMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));
        when(restoreJobMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));

        var backupPage = backupService.getBackupJobs(0, 1000);
        var restorePage = backupService.getRestoreJobs(-1, 0);

        assertEquals(1L, backupPage.getCurrent());
        assertEquals(100L, backupPage.getSize());
        assertEquals(1L, restorePage.getCurrent());
        assertEquals(20L, restorePage.getSize());
    }

    @Test
    void testVerifySmbTargetSuccess() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        doNothing().when(smbStorageService).verifyWritable(target);

        BackupTargetResponse response = backupService.verifyTarget(1L);

        assertEquals(BackupTarget.VERIFY_SUCCESS, response.getVerifyStatus());
        assertTrue(response.getVerifyMessage().contains("共享目录可读写"));
    }

    @Test
    void testVerifySmbTargetFailure_ShouldHideInternalErrorDetails() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        doThrow(new RuntimeException("access denied to \\\\192.168.50.5\\archive"))
                .when(smbStorageService).verifyWritable(target);

        BackupTargetResponse response = backupService.verifyTarget(1L);

        assertEquals(BackupTarget.VERIFY_FAILED, response.getVerifyStatus());
        assertEquals("SMB 目录验证失败，请检查网络、共享配置和权限", response.getVerifyMessage());
        assertFalse(response.getVerifyMessage().contains("192.168.50.5"));
    }

    @Test
    void testUpdateTarget_ShouldRejectWhenBackupJobRunning() {
        BackupTarget existing = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath("/tmp/archive-backups")
                .build();
        BackupTargetRequest request = new BackupTargetRequest();
        request.setName("NAS-UPDATED");
        request.setTargetType(BackupTarget.TYPE_LOCAL);
        request.setEnabled(true);
        request.setLocalPath("/tmp/archive-backups-new");
        when(backupTargetMapper.selectById(1L)).thenReturn(existing);
        when(backupJobMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.updateTarget(1L, request));

        assertTrue(ex.getMessage().contains("已有任务正在执行"));
        verify(backupTargetMapper, never()).updateById(any());
    }

    @Test
    void testDeleteTarget_ShouldRejectWhenBackupJobRunning() {
        BackupTarget existing = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath("/tmp/archive-backups")
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(existing);
        when(backupJobMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.deleteTarget(1L));

        assertTrue(ex.getMessage().contains("已有任务正在执行"));
        verify(backupTargetMapper, never()).deleteById(any());
    }

    @Test
    void testVerifyTarget_ShouldRejectWhenBackupJobRunning() {
        BackupTarget existing = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath("/tmp/archive-backups")
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(existing);
        when(backupJobMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.verifyTarget(1L));

        assertTrue(ex.getMessage().contains("已有任务正在执行"));
        verify(backupTargetMapper, never()).updateById(any());
    }

    @Test
    void testUpdateTarget_ShouldRejectWhenRestoreJobRunningOnSameTarget() {
        BackupTarget existing = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath("/tmp/archive-backups")
                .build();
        BackupTargetRequest request = new BackupTargetRequest();
        request.setName("NAS-UPDATED");
        request.setTargetType(BackupTarget.TYPE_LOCAL);
        request.setEnabled(true);
        request.setLocalPath("/tmp/archive-backups-new");
        when(backupTargetMapper.selectById(1L)).thenReturn(existing);
        when(backupJobMapper.selectCount(any())).thenReturn(0L);
        when(restoreJobMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.updateTarget(1L, request));

        assertTrue(ex.getMessage().contains("正被恢复任务使用"));
        verify(backupTargetMapper, never()).updateById(any());
    }

    @Test
    void testDeleteTarget_ShouldRejectWhenRestoreJobRunningOnSameTarget() {
        BackupTarget existing = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath("/tmp/archive-backups")
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(existing);
        when(backupJobMapper.selectCount(any())).thenReturn(0L);
        when(restoreJobMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.deleteTarget(1L));

        assertTrue(ex.getMessage().contains("正被恢复任务使用"));
        verify(backupTargetMapper, never()).deleteById(any());
    }

    @Test
    void testVerifyTarget_ShouldRejectWhenRestoreJobRunningOnSameTarget() {
        BackupTarget existing = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath("/tmp/archive-backups")
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(existing);
        when(backupJobMapper.selectCount(any())).thenReturn(0L);
        when(restoreJobMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.verifyTarget(1L));

        assertTrue(ex.getMessage().contains("正被恢复任务使用"));
        verify(backupTargetMapper, never()).updateById(any());
    }

    @Test
    void testRunManualBackup_ShouldRejectDisabledTarget() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-disabled-target");
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(false)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runManualBackup(1L));

        assertTrue(ex.getMessage().contains("备份目标已禁用"));
        verify(backupJobMapper, never()).insert(any());
    }

    @Test
    void testRunManualBackup_ShouldRejectWhenSameTargetBackupAlreadyRunning() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-running-target");
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(backupJobMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runManualBackup(1L));

        assertTrue(ex.getMessage().contains("已有任务正在执行"));
        verify(backupJobMapper, never()).insert(any());
    }

    @Test
    void testRunManualBackup_ShouldHideInternalFailureDetails() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-failure-target");
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(backupJobMapper.selectCount(any())).thenReturn(0L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runManualBackup(1L));

        assertTrue(ex.getMessage().contains("执行备份失败，任务编号: BK-"));
        assertFalse(ex.getMessage().contains("NullPointerException"));
        assertFalse(ex.getMessage().contains("Cannot invoke"));
    }

    @Test
    void testRunManualBackup_ShouldCleanupPartialLocalBackupSetOnFailure() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-partial-local");
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(backupJobMapper.selectCount(any())).thenReturn(0L);
        when(sysConfigMapper.selectAllOrdered()).thenReturn(List.of());
        when(digitalFileMapper.selectList(any())).thenThrow(new RuntimeException("simulated file export failure"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runManualBackup(1L));

        assertTrue(ex.getMessage().contains("执行备份失败，任务编号: BK-"));
        try (var paths = Files.list(tempDir)) {
            assertEquals(0L, paths.count());
        }
    }

    @Test
    void testRunManualBackup_ShouldCleanupPartialSmbBackupSetOnUploadFailure() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(backupJobMapper.selectCount(any())).thenReturn(0L);
        when(sysConfigMapper.selectAllOrdered()).thenReturn(List.of());
        when(digitalFileMapper.selectList(any())).thenReturn(List.of());
        doNothing().when(smbStorageService).verifyWritable(target);
        doThrow(new RuntimeException("simulated smb upload failure"))
                .when(smbStorageService).uploadDirectory(eq(target), anyString(), any());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runManualBackup(1L));

        assertTrue(ex.getMessage().contains("执行备份失败，任务编号: BK-"));
        verify(smbStorageService).deleteDirectory(eq(target), argThat(name -> name != null && name.startsWith("BK-")));
    }

    @Test
    void testRunManualBackup_ShouldRemainSuccessfulWhenCleanupOldBackupSetsFails() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-success-cleanup-failure");
        Path missingDir = tempDir.resolve("missing-after-backup");
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        BackupTarget unreadableTarget = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(missingDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target, unreadableTarget);
        when(backupJobMapper.selectCount(any())).thenReturn(0L);
        when(sysConfigMapper.selectAllOrdered()).thenReturn(List.of());
        when(digitalFileMapper.selectList(any())).thenReturn(List.of());

        BackupJob job = backupService.runManualBackup(1L);

        assertEquals(BackupJob.STATUS_SUCCESS, job.getStatus());
        assertNotNull(job.getBackupNo());
        verify(backupJobMapper).updateById(argThat(updatedJob ->
                BackupJob.STATUS_SUCCESS.equals(updatedJob.getStatus())
                        && updatedJob.getBackupNo() != null
        ));
    }

    @Test
    void testGetBackupSetsFromLocalTarget() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-test");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-aaaa1111"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(databaseFile, "-- pg dump");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "database/archive-system.sql|" + sha256(databaseFile) + System.lineSeparator()
                        + "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString(),
                "databaseMode", "PG_DUMP",
                "fileCount", 2,
                "objectCount", 4,
                "totalBytes", 2048,
                "filesIndex", "files-index.json"
        ));

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("BK-20260330150000-aaaa1111", sets.get(0).getBackupNo());
        assertEquals("READY", sets.get(0).getVerifyStatus());
        assertEquals(2048L, sets.get(0).getTotalBytes());
        assertEquals(tempDir.resolve("BK-20260330150000-aaaa1111").toString(), sets.get(0).getDisplayPath());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldIgnoreNonBackupDirectories() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-filter-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-aaaa1111"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(setDir.resolve("checksums.txt"), "manifest.json|" + sha256(manifest));
        Files.createDirectories(tempDir.resolve("documents"));

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString(),
                "databaseMode", "PG_DUMP"
        ));

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("BK-20260330150000-aaaa1111", sets.get(0).getBackupSetName());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeInvalidConfigJson() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-config-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bbbb2222"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(configFile, "{invalid");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("系统配置备份格式无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(sets.get(0).getConfigRestorable()));
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeConfigWithoutConfigKey() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-config-entry-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-zzzz9999"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(configFile, """
                [
                  {
                    "configValue": "archive-system"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("系统配置备份格式无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(sets.get(0).getConfigRestorable()));
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeInvalidBooleanConfigValue() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-config-bool-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bool999"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(configFile, """
                [
                  {
                    "configKey": "system.runtime.demo.enabled",
                    "configType": "BOOLEAN",
                    "configValue": "abc"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("系统配置备份格式无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(sets.get(0).getConfigRestorable()));
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeDuplicateConfigKeys() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-duplicate-config-key-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-dupe999"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(configFile, """
                [
                  {
                    "configKey": "system.site.name",
                    "configType": "STRING",
                    "configValue": "档案系统"
                  },
                  {
                    "configKey": "system.site.name",
                    "configType": "STRING",
                    "configValue": "覆盖值"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("系统配置备份格式无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(sets.get(0).getConfigRestorable()));
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeWhenChecksumsOmitConfigSnapshot() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-missing-config-checksum-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-misscfg"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(configFile, """
                [
                  {
                    "configKey": "system.site.name",
                    "configType": "STRING",
                    "configValue": "档案系统"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("checksums.txt 未覆盖全部恢复文件", sets.get(0).getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(sets.get(0).getConfigRestorable()));
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeInvalidManifestJson() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-pppp4444"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, "{invalid");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 文件格式无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeInvalidManifestCreatedAt() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-created-at-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-qqqq5555"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "not-a-datetime"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeUnexpectedManifestDatabaseFile() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-db-file-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-dbfile77"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseFile": "other.sql"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeManifestConfigFilesMissingSysConfig() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-config-files-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-cfgfile88"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00",
                  "configFiles": ["backup-config.json"]
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeMissingBackupConfigFileWhenManifestDeclaresIt() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-missing-backup-config-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bcfg001"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00",
                  "configFiles": ["sys-config.json", "backup-config.json"]
                }
                """);
        Files.writeString(configFile, "[]");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("备份配置元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeBackupConfigWithMismatchedBackupNo() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-backup-config-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bcfg002"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Path backupConfigFile = configDir.resolve("backup-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00",
                  "configFiles": ["sys-config.json", "backup-config.json"]
                }
                """);
        Files.writeString(configFile, "[]");
        Files.writeString(backupConfigFile, """
                {
                  "backupNo": "BK-20260330150000-other999",
                  "targetType": "LOCAL"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile) + System.lineSeparator()
                        + "config/backup-config.json|" + sha256(backupConfigFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("备份配置元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeBackupConfigWithoutTargetType() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-backup-config-type-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bcfg003"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Path backupConfigFile = configDir.resolve("backup-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00",
                  "configFiles": ["sys-config.json", "backup-config.json"]
                }
                """);
        Files.writeString(configFile, "[]");
        Files.writeString(backupConfigFile, """
                {
                  "backupNo": "BK-20260330150000-bcfg003"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile) + System.lineSeparator()
                        + "config/backup-config.json|" + sha256(backupConfigFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("备份配置元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeBackupConfigWithoutTargetId() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-backup-config-id-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bcfg004"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Path backupConfigFile = configDir.resolve("backup-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00",
                  "configFiles": ["sys-config.json", "backup-config.json"]
                }
                """);
        Files.writeString(configFile, "[]");
        Files.writeString(backupConfigFile, """
                {
                  "backupNo": "BK-20260330150000-bcfg004",
                  "targetType": "LOCAL"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile) + System.lineSeparator()
                        + "config/backup-config.json|" + sha256(backupConfigFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("备份配置元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeBackupConfigWithoutTargetName() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-backup-config-name-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bcfg005"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Path backupConfigFile = configDir.resolve("backup-config.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00",
                  "targetName": "本地目录",
                  "configFiles": ["sys-config.json", "backup-config.json"]
                }
                """);
        Files.writeString(configFile, "[]");
        Files.writeString(backupConfigFile, """
                {
                  "backupNo": "BK-20260330150000-bcfg005",
                  "targetId": 1,
                  "targetType": "LOCAL"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile) + System.lineSeparator()
                        + "config/backup-config.json|" + sha256(backupConfigFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("备份配置元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeNegativeManifestStats() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-stats-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-rrrr6666"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00",
                  "fileCount": -1,
                  "objectCount": 4,
                  "totalBytes": 2048
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeMismatchedManifestBackupNo() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-backup-no-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-ssss7777"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-other9999",
                  "createdAt": "2026-03-30T15:00:00",
                  "fileCount": 2,
                  "objectCount": 4,
                  "totalBytes": 2048,
                  "filesIndex": "files-index.json"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeMismatchedManifestFileCount() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-file-count-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-tttt8888"));
        Path manifest = setDir.resolve("manifest.json");
        Path filesDir = Files.createDirectories(setDir.resolve("files"));
        Path firstObject = Files.createDirectories(filesDir.resolve("archives/2026")).resolve("a.pdf");
        Path secondObject = filesDir.resolve("archives/2026/b.pdf");
        Files.writeString(firstObject, "a");
        Files.writeString(secondObject, "b");
        Path filesIndex = filesDir.resolve("files-index.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-tttt8888",
                  "createdAt": "2026-03-30T15:00:00",
                  "fileCount": 3,
                  "objectCount": 2,
                  "totalBytes": 2048,
                  "filesIndex": "files-index.json"
                }
                """);
        Files.writeString(filesIndex, """
                [
                  {
                    "id": 1,
                    "fileName": "a.pdf",
                    "storagePath": "archives/2026/a.pdf"
                  },
                  {
                    "id": 2,
                    "fileName": "b.pdf",
                    "storagePath": "archives/2026/b.pdf"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "files/files-index.json|" + sha256(filesIndex) + System.lineSeparator()
                        + "files/archives/2026/a.pdf|" + sha256(firstObject) + System.lineSeparator()
                        + "files/archives/2026/b.pdf|" + sha256(secondObject)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeMismatchedManifestObjectCount() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-object-count-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-uuuu9999"));
        Path manifest = setDir.resolve("manifest.json");
        Path filesDir = Files.createDirectories(setDir.resolve("files"));
        Path storageObject = Files.createDirectories(filesDir.resolve("archives/2026")).resolve("original.pdf");
        Path previewObject = Files.createDirectories(filesDir.resolve("previews/2026")).resolve("preview.jpg");
        Files.writeString(storageObject, "original");
        Files.writeString(previewObject, "preview");
        Path filesIndex = filesDir.resolve("files-index.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-uuuu9999",
                  "createdAt": "2026-03-30T15:00:00",
                  "fileCount": 1,
                  "objectCount": 1,
                  "totalBytes": 2048,
                  "filesIndex": "files-index.json"
                }
                """);
        Files.writeString(filesIndex, """
                [
                  {
                    "id": 1,
                    "fileName": "original.pdf",
                    "storagePath": " archives/2026/original.pdf ",
                    "previewPath": "previews/2026/preview.jpg"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "files/files-index.json|" + sha256(filesIndex) + System.lineSeparator()
                        + "files/archives/2026/original.pdf|" + sha256(storageObject) + System.lineSeparator()
                        + "files/previews/2026/preview.jpg|" + sha256(previewObject)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeMismatchedManifestTotalBytes() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-total-bytes-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-vvvv0000"));
        Path manifest = setDir.resolve("manifest.json");
        Path filesDir = Files.createDirectories(setDir.resolve("files"));
        Path objectFile = Files.createDirectories(filesDir.resolve("archives/2026")).resolve("payload.bin");
        Files.writeString(objectFile, "12345");
        Path filesIndex = filesDir.resolve("files-index.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-vvvv0000",
                  "createdAt": "2026-03-30T15:00:00",
                  "fileCount": 1,
                  "objectCount": 1,
                  "totalBytes": 999,
                  "filesIndex": "files-index.json"
                }
                """);
        Files.writeString(filesIndex, """
                [
                  {
                    "id": 1,
                    "fileName": "payload.bin",
                    "storagePath": "archives/2026/payload.bin"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "files/files-index.json|" + sha256(filesIndex) + System.lineSeparator()
                        + "files/archives/2026/payload.bin|" + sha256(objectFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeMismatchedManifestScope() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-scope-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-xxxx1111"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(databaseFile, "-- placeholder database dump");
        Files.writeString(configFile, "[]");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-xxxx1111",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "scope": ["DATABASE"]
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeInvalidManifestChecksumAlgorithm() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-manifest-checksum-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-zzzz3333"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-zzzz3333",
                  "createdAt": "2026-03-30T15:00:00",
                  "checksumAlgorithm": "MD5"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradePlaceholderDatabaseWithoutWarning() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-database-warning-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bbbb4444"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- placeholder database dump");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-bbbb4444",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER"
                }
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testReadLocalBackupSetSafely_ShouldDowngradeUnreadableEntry() {
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath("/tmp/archive-backups")
                .build();
        Path backupSetPath = mock(Path.class);
        Path fileName = mock(Path.class);
        when(backupSetPath.getFileName()).thenReturn(fileName);
        when(fileName.toString()).thenReturn("BK-20260330150000-bad2222");
        when(backupSetPath.toString()).thenReturn("/tmp/archive-backups/BK-20260330150000-bad2222");
        when(backupSetPath.resolve("manifest.json")).thenThrow(new RuntimeException("access denied"));

        BackupSetResponse response = ReflectionTestUtils.invokeMethod(
                backupService,
                "readLocalBackupSetSafely",
                target,
                backupSetPath
        );

        assertNotNull(response);
        assertEquals("BK-20260330150000-bad2222", response.getBackupSetName());
        assertEquals("INCOMPLETE", response.getVerifyStatus());
        assertEquals("读取备份集元数据失败，请检查备份文件完整性和访问权限", response.getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(response.getDatabaseRestorable()));
        assertFalse(Boolean.TRUE.equals(response.getFilesRestorable()));
        assertFalse(Boolean.TRUE.equals(response.getConfigRestorable()));
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldNotCreateMissingDirectoryForRestoreBrowse() throws Exception {
        Path baseDir = Files.createTempDirectory("backup-set-missing-local");
        Path missingDir = baseDir.resolve("missing-backups");

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(missingDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.getBackupSets(1L));

        assertEquals("无法读取备份目录: 目标目录不存在", ex.getMessage());
        assertFalse(Files.exists(missingDir));
    }

    @Test
    void testGetBackupSets_ShouldRejectDisabledTargetWhenSpecified() {
        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(false)
                .localPath("/tmp/archive-backups")
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.getBackupSets(1L));

        assertTrue(ex.getMessage().contains("备份目标已禁用"));
    }

    @Test
    void testGetBackupSetsWithoutTargetId_ShouldSkipUnreadableTargetsAndKeepHealthyResults() throws Exception {
        Path localDir = Files.createTempDirectory("backup-set-aggregate-local");
        Path setDir = Files.createDirectories(localDir.resolve("BK-20260330150000-aaaa1111"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(setDir.resolve("checksums.txt"), "manifest.json|" + sha256(manifest));

        BackupTarget localTarget = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(localDir.toString())
                .updatedAt(LocalDateTime.of(2026, 3, 30, 16, 0))
                .build();
        BackupTarget smbTarget = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .updatedAt(LocalDateTime.of(2026, 3, 30, 15, 0))
                .build();
        when(backupTargetMapper.selectList(any())).thenReturn(List.of(localTarget, smbTarget));
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString(),
                "databaseMode", "PG_DUMP"
        ));
        when(smbStorageService.listDirectories(smbTarget))
                .thenThrow(new RuntimeException("access denied to \\\\192.168.50.5\\archive"));

        List<BackupSetResponse> sets = backupService.getBackupSets(null);

        assertEquals(1, sets.size());
        assertEquals("BK-20260330150000-aaaa1111", sets.get(0).getBackupSetName());
        assertEquals("本地目录", sets.get(0).getTargetName());
    }

    @Test
    void testGetBackupSetsFromSmbTarget() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-aaaa1111", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/manifest.json")).thenReturn(true);
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/checksums.txt")).thenReturn(true);
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/database/archive-system.sql")).thenReturn(true);
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/manifest.json"))
                .thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(objectMapper.readValue(any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0).toString(),
                "databaseMode", "PG_DUMP",
                "fileCount", 2,
                "objectCount", 4,
                "totalBytes", 4096,
                "filesIndex", "files-index.json"
        ));
        byte[] databaseBytes = "-- pg dump".getBytes();
        String checksum = sha256Bytes("{}".getBytes());
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream((
                        "database/archive-system.sql|" + sha256Bytes(databaseBytes) + System.lineSeparator()
                                + "manifest.json|" + checksum
                ).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/database/archive-system.sql"))
                .thenReturn(new ByteArrayInputStream(databaseBytes));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-aaaa1111"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-aaaa1111");

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("READY", sets.get(0).getVerifyStatus());
        assertEquals(4096L, sets.get(0).getTotalBytes());
        assertTrue(sets.get(0).getBackupSetPath().startsWith("smb://"));
        assertEquals("smb://192.168.50.5/archive/BK-20260330150000-aaaa1111", sets.get(0).getDisplayPath());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeMissingDatabaseModeWhenDatabaseFileExists() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-invalid-database-mode-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-mmmm2222"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(databaseFile, "-- pg dump");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "database/archive-system.sql|" + sha256(databaseFile) + System.lineSeparator()
                        + "manifest.json|" + sha256(manifest)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("数据库备份元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(sets.get(0).getDatabaseRestorable()));
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldIgnoreNonBackupDirectories() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-aaaa1111", LocalDateTime.of(2026, 3, 30, 15, 0)),
                new SmbStorageService.RemoteDirectoryEntry("documents", LocalDateTime.of(2026, 3, 29, 12, 0))
        ));
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/manifest.json")).thenReturn(true);
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/checksums.txt")).thenReturn(true);
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/manifest.json"))
                .thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(objectMapper.readValue(any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0).toString()
        ));
        String checksum = sha256Bytes("{}".getBytes());
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream(("manifest.json|" + checksum).getBytes()));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-aaaa1111"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-aaaa1111");

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("BK-20260330150000-aaaa1111", sets.get(0).getBackupSetName());
        verify(smbStorageService, never()).exists(target, "documents/manifest.json");
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldKeepHealthyEntriesWhenOneBackupSetIsUnreadable() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-good111", LocalDateTime.of(2026, 3, 30, 15, 0)),
                new SmbStorageService.RemoteDirectoryEntry("BK-20260331150000-bad2222", LocalDateTime.of(2026, 3, 31, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-good111/manifest.json", "BK-20260330150000-good111/checksums.txt" -> true;
                case "BK-20260331150000-bad2222/manifest.json" -> throw new RuntimeException("access denied");
                default -> false;
            };
        });
        when(smbStorageService.openInputStream(target, "BK-20260330150000-good111/manifest.json"))
                .thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(objectMapper.readValue(any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-good111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0).toString()
        ));
        String checksum = sha256Bytes("{}".getBytes());
        when(smbStorageService.openInputStream(target, "BK-20260330150000-good111/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream(("manifest.json|" + checksum).getBytes()));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-good111"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-good111");
        when(smbStorageService.buildDisplayPath(target, "BK-20260331150000-bad2222"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260331150000-bad2222");

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(2, sets.size());
        assertEquals("BK-20260331150000-bad2222", sets.get(0).getBackupSetName());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("读取备份集元数据失败，请检查备份文件完整性和访问权限", sets.get(0).getVerifyMessage());
        assertEquals("BK-20260330150000-good111", sets.get(1).getBackupSetName());
        assertEquals("READY", sets.get(1).getVerifyStatus());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradeInvalidFilesIndexJson() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-cccc3333", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-cccc3333/manifest.json",
                        "BK-20260330150000-cccc3333/checksums.txt",
                        "BK-20260330150000-cccc3333/files/files-index.json" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-cccc3333",
                  "createdAt": "2026-03-30T15:00:00",
                  "fileCount": 2,
                  "objectCount": 4,
                  "totalBytes": 2048,
                  "filesIndex": "files-index.json"
                }
                """.getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-cccc3333/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        String manifestChecksum = sha256Bytes(manifestBytes);
        byte[] invalidIndexBytes = "{invalid".getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-cccc3333/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream((
                        "manifest.json|" + manifestChecksum + System.lineSeparator()
                                + "files/files-index.json|" + sha256Bytes(invalidIndexBytes)
                ).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-cccc3333/files/files-index.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(invalidIndexBytes));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-cccc3333"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-cccc3333");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("文件索引格式无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(sets.get(0).getFilesRestorable()));
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradeMismatchedManifestBackupNo() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-tttt8888", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-tttt8888/manifest.json",
                        "BK-20260330150000-tttt8888/checksums.txt" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-other9999",
                  "createdAt": "2026-03-30T15:00:00",
                  "fileCount": 2,
                  "objectCount": 4,
                  "totalBytes": 2048,
                  "filesIndex": "files-index.json"
                }
                """.getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-tttt8888/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-tttt8888/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream(("manifest.json|" + sha256Bytes(manifestBytes)).getBytes()));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-tttt8888"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-tttt8888");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradeMismatchedManifestTotalBytes() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-www1111", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-www1111/manifest.json",
                        "BK-20260330150000-www1111/checksums.txt",
                        "BK-20260330150000-www1111/files/files-index.json",
                        "BK-20260330150000-www1111/files/archives/2026/payload.bin" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-www1111",
                  "createdAt": "2026-03-30T15:00:00",
                  "fileCount": 1,
                  "objectCount": 1,
                  "totalBytes": 999,
                  "filesIndex": "files-index.json"
                }
                """.getBytes();
        byte[] indexBytes = """
                [
                  {
                    "id": 1,
                    "fileName": "payload.bin",
                    "storagePath": "archives/2026/payload.bin"
                  }
                ]
                """.getBytes();
        byte[] objectBytes = "12345".getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-www1111/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-www1111/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream((
                        "manifest.json|" + sha256Bytes(manifestBytes) + System.lineSeparator()
                                + "files/files-index.json|" + sha256Bytes(indexBytes) + System.lineSeparator()
                                + "files/archives/2026/payload.bin|" + sha256Bytes(objectBytes)
                ).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-www1111/files/files-index.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(indexBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-www1111/files/archives/2026/payload.bin"))
                .thenAnswer(invocation -> new ByteArrayInputStream(objectBytes));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-www1111"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-www1111");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradeInvalidManifestScopeValue() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-yyyy2222", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-yyyy2222/manifest.json",
                        "BK-20260330150000-yyyy2222/checksums.txt",
                        "BK-20260330150000-yyyy2222/database/archive-system.sql" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-yyyy2222",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "scope": ["DATABASE", "SECRETS"]
                }
                """.getBytes();
        byte[] databaseBytes = "-- placeholder database dump".getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-yyyy2222/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-yyyy2222/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream((
                        "manifest.json|" + sha256Bytes(manifestBytes) + System.lineSeparator()
                                + "database/archive-system.sql|" + sha256Bytes(databaseBytes)
                ).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-yyyy2222/database/archive-system.sql"))
                .thenAnswer(invocation -> new ByteArrayInputStream(databaseBytes));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-yyyy2222"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-yyyy2222");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradeInvalidManifestChecksumAlgorithm() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-aaaa4444", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-aaaa4444/manifest.json",
                        "BK-20260330150000-aaaa4444/checksums.txt" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-aaaa4444",
                  "createdAt": "2026-03-30T15:00:00",
                  "checksumAlgorithm": "MD5"
                }
                """.getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa4444/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa4444/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream(("manifest.json|" + sha256Bytes(manifestBytes)).getBytes()));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-aaaa4444"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-aaaa4444");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradePgDumpManifestWithWarning() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-cccc5555", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-cccc5555/manifest.json",
                        "BK-20260330150000-cccc5555/checksums.txt",
                        "BK-20260330150000-cccc5555/database/archive-system.sql" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-cccc5555",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PG_DUMP",
                  "databaseWarning": "pg_dump 执行失败，已写入占位文件"
                }
                """.getBytes();
        byte[] databaseBytes = "-- pg dump".getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-cccc5555/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-cccc5555/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream((
                        "manifest.json|" + sha256Bytes(manifestBytes) + System.lineSeparator()
                                + "database/archive-system.sql|" + sha256Bytes(databaseBytes)
                ).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-cccc5555/database/archive-system.sql"))
                .thenAnswer(invocation -> new ByteArrayInputStream(databaseBytes));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-cccc5555"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-cccc5555");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("manifest 元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradeBackupConfigWithMismatchedTargetName() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-dddd6666", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-dddd6666/manifest.json",
                        "BK-20260330150000-dddd6666/checksums.txt",
                        "BK-20260330150000-dddd6666/config/sys-config.json",
                        "BK-20260330150000-dddd6666/config/backup-config.json" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-dddd6666",
                  "createdAt": "2026-03-30T15:00:00",
                  "targetName": "NAS-A",
                  "configFiles": ["sys-config.json", "backup-config.json"]
                }
                """.getBytes();
        byte[] configBytes = "[]".getBytes();
        byte[] backupConfigBytes = """
                {
                  "backupNo": "BK-20260330150000-dddd6666",
                  "targetType": "SMB",
                  "targetName": "NAS-B"
                }
                """.getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-dddd6666/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-dddd6666/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream((
                        "manifest.json|" + sha256Bytes(manifestBytes) + System.lineSeparator()
                                + "config/sys-config.json|" + sha256Bytes(configBytes) + System.lineSeparator()
                                + "config/backup-config.json|" + sha256Bytes(backupConfigBytes)
                ).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-dddd6666/config/sys-config.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(configBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-dddd6666/config/backup-config.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(backupConfigBytes));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-dddd6666"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-dddd6666");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("备份配置元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradeBackupConfigWithInvalidTargetId() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-eeee7777", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-eeee7777/manifest.json",
                        "BK-20260330150000-eeee7777/checksums.txt",
                        "BK-20260330150000-eeee7777/config/sys-config.json",
                        "BK-20260330150000-eeee7777/config/backup-config.json" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-eeee7777",
                  "createdAt": "2026-03-30T15:00:00",
                  "targetName": "NAS",
                  "configFiles": ["sys-config.json", "backup-config.json"]
                }
                """.getBytes();
        byte[] configBytes = "[]".getBytes();
        byte[] backupConfigBytes = """
                {
                  "backupNo": "BK-20260330150000-eeee7777",
                  "targetId": 0,
                  "targetType": "SMB",
                  "targetName": "NAS"
                }
                """.getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-eeee7777/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-eeee7777/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream((
                        "manifest.json|" + sha256Bytes(manifestBytes) + System.lineSeparator()
                                + "config/sys-config.json|" + sha256Bytes(configBytes) + System.lineSeparator()
                                + "config/backup-config.json|" + sha256Bytes(backupConfigBytes)
                ).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-eeee7777/config/sys-config.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(configBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-eeee7777/config/backup-config.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(backupConfigBytes));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-eeee7777"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-eeee7777");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("备份配置元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldDowngradeBackupConfigWithoutTargetName() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-ffff8888", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(eq(target), anyString())).thenAnswer(invocation -> {
            String relativePath = invocation.getArgument(1, String.class);
            return switch (relativePath) {
                case "BK-20260330150000-ffff8888/manifest.json",
                        "BK-20260330150000-ffff8888/checksums.txt",
                        "BK-20260330150000-ffff8888/config/sys-config.json",
                        "BK-20260330150000-ffff8888/config/backup-config.json" -> true;
                default -> false;
            };
        });
        byte[] manifestBytes = """
                {
                  "backupNo": "BK-20260330150000-ffff8888",
                  "createdAt": "2026-03-30T15:00:00",
                  "targetName": "NAS",
                  "configFiles": ["sys-config.json", "backup-config.json"]
                }
                """.getBytes();
        byte[] configBytes = "[]".getBytes();
        byte[] backupConfigBytes = """
                {
                  "backupNo": "BK-20260330150000-ffff8888",
                  "targetId": 2,
                  "targetType": "SMB"
                }
                """.getBytes();
        when(smbStorageService.openInputStream(target, "BK-20260330150000-ffff8888/manifest.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(manifestBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-ffff8888/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream((
                        "manifest.json|" + sha256Bytes(manifestBytes) + System.lineSeparator()
                                + "config/sys-config.json|" + sha256Bytes(configBytes) + System.lineSeparator()
                                + "config/backup-config.json|" + sha256Bytes(backupConfigBytes)
                ).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-ffff8888/config/sys-config.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(configBytes));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-ffff8888/config/backup-config.json"))
                .thenAnswer(invocation -> new ByteArrayInputStream(backupConfigBytes));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-ffff8888"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-ffff8888");
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("备份配置元数据无效，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromLocalTarget_ShouldDowngradeFilesIndexReferencingMissingObject() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-missing-object-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-dddd4444"));
        Path manifest = setDir.resolve("manifest.json");
        Path filesDir = Files.createDirectories(setDir.resolve("files"));
        Path filesIndex = filesDir.resolve("files-index.json");
        Files.writeString(manifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(filesIndex, """
                [
                  {
                    "id": 1,
                    "archiveId": 10,
                    "fileName": "missing.pdf",
                    "storagePath": "archives/2026/missing.pdf"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "files/files-index.json|" + sha256(filesIndex)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("文件索引引用了缺失的电子文件对象，请检查备份完整性", sets.get(0).getVerifyMessage());
        assertFalse(Boolean.TRUE.equals(sets.get(0).getFilesRestorable()));
    }

    @Test
    void testCleanupOldBackupSets_ShouldPreferKeepingReadyBackupSetOverNewerIncompleteSet() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-retention-local");
        Path readySetDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-ready111"));
        Path readyManifest = readySetDir.resolve("manifest.json");
        Files.writeString(readyManifest, """
                {
                  "createdAt": "2026-03-30T15:00:00"
                }
                """);
        Files.writeString(readySetDir.resolve("checksums.txt"), "manifest.json|" + sha256(readyManifest));

        Path incompleteSetDir = Files.createDirectories(tempDir.resolve("BK-20260331150000-bad2222"));
        Path incompleteManifest = incompleteSetDir.resolve("manifest.json");
        Files.writeString(incompleteManifest, "{}");
        Files.writeString(incompleteSetDir.resolve("checksums.txt"), "manifest.json|deadbeef");

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(configService.getIntValue("system.backup.keep.count", 7)).thenReturn(1);

        ReflectionTestUtils.invokeMethod(backupService, "cleanupOldBackupSets", target);

        assertTrue(Files.exists(readySetDir));
        assertFalse(Files.exists(incompleteSetDir));
    }

    @Test
    void testGetBackupSetsFromSmbTarget_ShouldAllowReadOnlyRestoreBrowse() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-aaaa1111", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/manifest.json")).thenReturn(true);
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/checksums.txt")).thenReturn(true);
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/manifest.json"))
                .thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(objectMapper.readValue(any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0).toString()
        ));
        String checksum = sha256Bytes("{}".getBytes());
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream(("manifest.json|" + checksum).getBytes()));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-aaaa1111"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-aaaa1111");

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("READY", sets.get(0).getVerifyStatus());
        verify(smbStorageService, never()).verifyWritable(target);
    }

    @Test
    void testGetBackupSetsFromSmbTargetFailure_ShouldHideInternalErrorDetails() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target))
                .thenThrow(new RuntimeException("access denied to \\\\192.168.50.5\\archive"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.getBackupSets(2L));

        assertEquals("读取 SMB 备份目录失败，请检查网络、共享配置和访问权限", ex.getMessage());
        assertFalse(ex.getMessage().contains("192.168.50.5"));
    }

    @Test
    void testGetBackupSetsFromLocalTarget_WithTraversalChecksumPath_ShouldFailVerification() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-traversal-local");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-aaaa1111"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, "{}");
        Files.writeString(setDir.resolve("checksums.txt"), "../manifest.json|" + sha256(manifest));

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString(),
                "databaseMode", "PG_DUMP"
        ));

        List<BackupSetResponse> sets = backupService.getBackupSets(1L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("校验备份集失败，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetBackupSetsFromSmbTarget_WithTraversalChecksumPath_ShouldFailVerification() throws Exception {
        BackupTarget target = BackupTarget.builder()
                .id(2L)
                .name("NAS")
                .targetType(BackupTarget.TYPE_SMB)
                .enabled(true)
                .smbHost("192.168.50.5")
                .smbShare("archive")
                .smbUsername("backup")
                .smbPasswordEncrypted("encrypted")
                .build();
        when(backupTargetMapper.selectById(2L)).thenReturn(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-aaaa1111", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/manifest.json")).thenReturn(true);
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/checksums.txt")).thenReturn(true);
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/manifest.json"))
                .thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(objectMapper.readValue(any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0).toString()
        ));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/checksums.txt"))
                .thenAnswer(invocation -> new ByteArrayInputStream("../manifest.json|deadbeef".getBytes()));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-aaaa1111"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-aaaa1111");

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("INCOMPLETE", sets.get(0).getVerifyStatus());
        assertEquals("校验备份集失败，请检查备份文件完整性", sets.get(0).getVerifyMessage());
    }

    @Test
    void testGetMaintenanceStatus() {
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(true);

        var result = backupService.getRestoreMaintenanceStatus();

        assertTrue(result.getEnabled());
        assertTrue(result.getRestoreRequiresMaintenance());
    }

    @Test
    void testSetRestoreMaintenanceMode_ShouldRejectDisableWhenRestoreRunning() {
        when(restoreJobMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.setRestoreMaintenanceMode(false));

        assertTrue(ex.getMessage().contains("不能退出维护模式"));
        verify(configService, never()).saveConfig(
                eq("system.runtime.maintenance.enabled"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void testRunRestoreRejectsWhenMaintenanceDisabled() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-test");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-aaaa1111"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Files.writeString(manifest, "{}");
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- restore payload");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString(),
                "databaseMode", "PG_DUMP"
        ));
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(true);
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(false);

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-aaaa1111")
                .restoreDatabase(true)
                .restoreFiles(false)
                .restoreConfig(false)
                .confirmationText("RESTORE")
                .build();

        assertThrows(RuntimeException.class, () -> backupService.runRestore(request));
    }

    @Test
    void testRunRestoreRejectsWhenAnotherRestoreIsRunning() throws Exception {
        when(restoreJobMapper.selectCount(any())).thenReturn(1L);

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-aaaa1111")
                .restoreDatabase(true)
                .restoreFiles(false)
                .restoreConfig(false)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));
        assertTrue(ex.getMessage().contains("已有恢复任务正在执行"));
        verify(backupTargetMapper, never()).selectById(anyLong());
    }

    @Test
    void testRunRestoreRejectsWhenFileRestoreRequestedButBackupSetHasNoFilesIndex() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-no-files-index");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-dddd4444"));
        Path manifest = setDir.resolve("manifest.json");
        Files.writeString(manifest, "{}");
        Files.writeString(setDir.resolve("checksums.txt"), "manifest.json|" + sha256(manifest));

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-dddd4444",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString()
        ));

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-dddd4444")
                .restoreDatabase(false)
                .restoreFiles(true)
                .restoreConfig(false)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));
        assertTrue(ex.getMessage().contains("未包含可恢复的电子文件索引"));
        verify(restoreJobMapper, never()).insert(any());
    }

    @Test
    void testRunRestoreRejectsWhenConfigRestoreRequestedButBackupSetHasNoConfigFile() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-no-config-file");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-eeee5555"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path filesDir = Files.createDirectories(setDir.resolve("files"));
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-eeee5555",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "databaseWarning": "未识别 PostgreSQL 连接信息，已生成占位数据库文件"
                }
                """);
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- placeholder database dump");
        Path filesIndex = filesDir.resolve("files-index.json");
        Files.writeString(filesIndex, "[]");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile) + System.lineSeparator()
                        + "files/files-index.json|" + sha256(filesIndex)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-eeee5555")
                .restoreDatabase(false)
                .restoreFiles(false)
                .restoreConfig(true)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));
        assertTrue(ex.getMessage().contains("未包含可恢复的系统配置"));
        verify(restoreJobMapper, never()).insert(any());
    }

    @Test
    void testRunRestoreRejectsWhenConfigRestoreRequestedButConfigEntriesAreInvalid() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-invalid-config-entry");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-yyyy8888"));
        Path manifest = setDir.resolve("manifest.json");
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-yyyy8888",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "databaseWarning": "未识别 PostgreSQL 连接信息，已生成占位数据库文件"
                }
                """);
        Files.writeString(configFile, """
                [
                  {
                    "configValue": "archive-system"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ReflectionTestUtils.setField(backupService, "objectMapper", new ObjectMapper().findAndRegisterModules());
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-yyyy8888")
                .restoreDatabase(false)
                .restoreFiles(false)
                .restoreConfig(true)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));

        assertEquals("备份集结构不完整，不能执行恢复", ex.getMessage());
        verify(restoreJobMapper, never()).insert(any());
    }

    @Test
    void testRunRestoreRejectsWhenAnyBusinessTableHasData() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-non-empty");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-bbbb2222"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Files.writeString(manifest, "{}");
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- restore payload");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-bbbb2222",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString(),
                "databaseMode", "PG_DUMP"
        ));
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(true);
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("sys_config", "arc_archive"));
        when(jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM \"arc_archive\" LIMIT 1)", Boolean.class)).thenReturn(true);

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-bbbb2222")
                .restoreDatabase(true)
                .restoreFiles(false)
                .restoreConfig(false)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));
        assertTrue(ex.getMessage().contains("arc_archive"));
    }

    @Test
    void testRunRestoreAllowsPreservedOperationalTables() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-preserved-only");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-cccc3333"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Files.writeString(manifest, "{}");
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- data only dump");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-cccc3333",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString(),
                "databaseMode", "PG_DUMP"
        ));
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(true);
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("sys_config", "arc_backup_target"));

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-cccc3333")
                .restoreDatabase(true)
                .restoreFiles(false)
                .restoreConfig(false)
                .rebuildIndex(false)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));
        assertTrue(ex.getMessage().contains("执行恢复失败，任务编号: RS-"));
        assertFalse(ex.getMessage().contains("未识别 PostgreSQL 连接信息"));
        verify(restoreJobMapper).updateById(argThat(job ->
                RestoreJob.STATUS_FAILED.equals(job.getStatus())
                        && "SKIPPED".equals(job.getRebuildIndexStatus())
                        && Boolean.FALSE.equals(job.getRestoredDatabase())
        ));
        verify(operationLogService).log(
                eq(OperationLog.OBJ_SYSTEM),
                anyString(),
                eq(null),
                eq(OperationLog.OP_UPDATE),
                eq("系统恢复任务失败"),
                argThat(detail -> "恢复执行失败，请联系系统管理员查看系统日志".equals(detail.get("errorMessage"))
                        && "BK-20260330150000-cccc3333".equals(detail.get("backupSetName")))
        );
    }

    @Test
    void testRunRestoreFailureBeforeIndex_ShouldMarkRebuildNotStartedWhenRequested() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-index-pending");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-ffff6666"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Files.writeString(manifest, "{}");
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- data only dump");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(objectMapper.readValue(any(java.io.File.class), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-ffff6666",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0, 0).toString(),
                "databaseMode", "PG_DUMP"
        ));
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(true);
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("sys_config", "arc_backup_target"));

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-ffff6666")
                .restoreDatabase(true)
                .restoreFiles(false)
                .restoreConfig(false)
                .rebuildIndex(true)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));

        assertTrue(ex.getMessage().contains("执行恢复失败，任务编号: RS-"));
        verify(restoreJobMapper).updateById(argThat(job ->
                RestoreJob.STATUS_FAILED.equals(job.getStatus())
                        && "NOT_STARTED".equals(job.getRebuildIndexStatus())
                        && Boolean.FALSE.equals(job.getRestoredDatabase())
        ));
    }

    @Test
    void testRunRestorePartialFailure_ShouldPreserveCompletedStepsInFailureReport() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-partial-failure");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-gggg7777"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path filesDir = Files.createDirectories(setDir.resolve("files"));
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-gggg7777",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "databaseWarning": "未识别 PostgreSQL 连接信息，已生成占位数据库文件"
                }
                """);
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- placeholder database dump");
        Path filesIndex = filesDir.resolve("files-index.json");
        Files.writeString(filesIndex, "[]");
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(configFile, """
                [
                  {
                    "configKey": "system.site.name",
                    "configType": "STRING",
                    "configValue": "档案系统"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile) + System.lineSeparator()
                        + "files/files-index.json|" + sha256(filesIndex) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ObjectMapper realObjectMapper = new ObjectMapper().findAndRegisterModules();
        org.springframework.test.util.ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(true);
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);
        doThrow(new RuntimeException("simulated config write failure"))
                .when(configService).saveConfig(
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-gggg7777")
                .restoreDatabase(false)
                .restoreFiles(true)
                .restoreConfig(true)
                .rebuildIndex(false)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));

        assertTrue(ex.getMessage().contains("执行恢复失败，任务编号: RS-"));
        verify(restoreJobMapper).updateById(argThat(job ->
                RestoreJob.STATUS_FAILED.equals(job.getStatus())
                        && Boolean.FALSE.equals(job.getRestoredDatabase())
                        && Boolean.TRUE.equals(job.getRestoredFiles())
                        && Boolean.FALSE.equals(job.getRestoredConfig())
                        && job.getRestoreReport() != null
                        && job.getRestoreReport().contains("\"step\":\"FILES\"")
                        && job.getRestoreReport().contains("\"status\":\"SUCCESS\"")
                        && job.getRestoreReport().contains("\"step\":\"CONFIG\"")
                        && job.getRestoreReport().contains("\"status\":\"FAILED\"")
                        && job.getRestoreReport().contains("\"restoredFiles\":true")
                        && job.getRestoreReport().contains("\"restoredConfig\":false")
        ));
    }

    @Test
    void testRunRestoreShouldRejectIncompleteBackupSetWhenFilesIndexReferencesMissingObjectBackup() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-missing-object");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-iiii9999"));
        Path manifest = setDir.resolve("manifest.json");
        Path filesDir = Files.createDirectories(setDir.resolve("files"));
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-iiii9999",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "databaseWarning": "未识别 PostgreSQL 连接信息，已生成占位数据库文件"
                }
                """);
        Path filesIndex = filesDir.resolve("files-index.json");
        Files.writeString(filesIndex, """
                [
                  {
                    "id": 1,
                    "archiveId": 10,
                    "fileName": "missing.pdf",
                    "storagePath": "archives/2026/missing.pdf"
                  }
                ]
                """);
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "files/files-index.json|" + sha256(filesIndex)
        );

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        ObjectMapper realObjectMapper = new ObjectMapper().findAndRegisterModules();
        ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);
        when(backupTargetMapper.selectById(1L)).thenReturn(target);

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-iiii9999")
                .restoreDatabase(false)
                .restoreFiles(true)
                .restoreConfig(false)
                .rebuildIndex(false)
                .confirmationText("RESTORE")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> backupService.runRestore(request));

        assertEquals("备份集结构不完整，不能执行恢复", ex.getMessage());
        verify(restoreJobMapper, never()).insert(any());
        verify(restoreJobMapper, never()).updateById(any());
    }

    @Test
    void testRunRestoreSuccess_ShouldRemainSuccessfulWhenExitMaintenanceFails() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-maintenance-exit-failure");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-hhhh8888"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-hhhh8888",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "databaseWarning": "未识别 PostgreSQL 连接信息，已生成占位数据库文件"
                }
                """);
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- placeholder database dump");
        Files.writeString(configFile, "[]");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        ObjectMapper realObjectMapper = new ObjectMapper().findAndRegisterModules();
        ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(true);
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);
        when(restoreJobMapper.selectCount(any())).thenReturn(0L, 0L);
        doThrow(new RuntimeException("simulated maintenance exit failure"))
                .when(configService).saveConfig(
                        eq("system.runtime.maintenance.enabled"),
                        eq("false"),
                        eq(SysConfig.GROUP_SYSTEM),
                        eq("系统维护模式开关"),
                        eq(SysConfig.TYPE_BOOLEAN),
                        eq(true),
                        eq(74)
                );

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-hhhh8888")
                .restoreDatabase(false)
                .restoreFiles(false)
                .restoreConfig(true)
                .rebuildIndex(false)
                .exitMaintenanceAfterSuccess(true)
                .confirmationText("RESTORE")
                .build();

        RestoreJob job = backupService.runRestore(request);

        assertEquals(RestoreJob.STATUS_SUCCESS, job.getStatus());
        assertTrue(Boolean.TRUE.equals(job.getRestoredConfig()));
        assertEquals("SKIPPED", job.getRebuildIndexStatus());
        assertEquals("恢复任务已完成，但退出维护模式失败，请手动处理", RestoreJobResponse.from(job).getStatusMessage());
        verify(restoreJobMapper, never()).updateById(argThat(updatedJob ->
                RestoreJob.STATUS_FAILED.equals(updatedJob.getStatus())
        ));
        verify(restoreJobMapper, atLeastOnce()).updateById(argThat(updatedJob ->
                RestoreJob.STATUS_SUCCESS.equals(updatedJob.getStatus())
                        && updatedJob.getRestoreReport() != null
                        && updatedJob.getRestoreReport().contains("\"restoredConfig\":true")
                        && updatedJob.getRestoreReport().contains("\"restoredDatabase\":false")
                        && updatedJob.getRestoreReport().contains("\"restoredFiles\":false")
                        && updatedJob.getRestoreReport().contains("\"rebuildIndexStatus\":\"SKIPPED\"")
                        && updatedJob.getRestoreReport().contains("\"step\":\"MAINTENANCE\"")
                        && updatedJob.getRestoreReport().contains("\"status\":\"FAILED\"")
                        && updatedJob.getRestoreReport().contains("恢复已完成，但退出维护模式失败，请手动处理")
        ));
    }

    @Test
    void testRunRestoreSuccess_ShouldPersistCommonReportFieldsWhenSkipExitMaintenance() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-success-no-exit-maintenance");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-jjjj0000"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-jjjj0000",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "databaseWarning": "未识别 PostgreSQL 连接信息，已生成占位数据库文件"
                }
                """);
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- placeholder database dump");
        Files.writeString(configFile, "[]");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        ObjectMapper realObjectMapper = new ObjectMapper().findAndRegisterModules();
        ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(true);
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);
        when(restoreJobMapper.selectCount(any())).thenReturn(0L, 0L);

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-jjjj0000")
                .restoreDatabase(false)
                .restoreFiles(false)
                .restoreConfig(true)
                .rebuildIndex(false)
                .exitMaintenanceAfterSuccess(false)
                .confirmationText("RESTORE")
                .build();

        RestoreJob job = backupService.runRestore(request);

        assertEquals(RestoreJob.STATUS_SUCCESS, job.getStatus());
        verify(restoreJobMapper, atLeastOnce()).updateById(argThat(updatedJob ->
                RestoreJob.STATUS_SUCCESS.equals(updatedJob.getStatus())
                        && updatedJob.getRestoreReport() != null
                        && updatedJob.getRestoreReport().contains("\"restoredConfig\":true")
                        && updatedJob.getRestoreReport().contains("\"restoredDatabase\":false")
                        && updatedJob.getRestoreReport().contains("\"restoredFiles\":false")
                        && updatedJob.getRestoreReport().contains("\"rebuildIndexStatus\":\"SKIPPED\"")
                        && updatedJob.getRestoreReport().contains("\"finishedAt\"")
        ));
        verify(configService, never()).saveConfig(
                eq("system.runtime.maintenance.enabled"),
                eq("false"),
                eq(SysConfig.GROUP_SYSTEM),
                eq("系统维护模式开关"),
                eq(SysConfig.TYPE_BOOLEAN),
                eq(true),
                eq(74)
        );
    }

    @Test
    void testRunRestoreSuccess_ShouldNotExitMaintenanceWhenSystemIsNotInMaintenanceMode() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-success-maintenance-already-off");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-kkkk1111"));
        Path manifest = setDir.resolve("manifest.json");
        Path databaseDir = Files.createDirectories(setDir.resolve("database"));
        Path configDir = Files.createDirectories(setDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(manifest, """
                {
                  "backupNo": "BK-20260330150000-kkkk1111",
                  "createdAt": "2026-03-30T15:00:00",
                  "databaseMode": "PLACEHOLDER",
                  "databaseWarning": "未识别 PostgreSQL 连接信息，已生成占位数据库文件"
                }
                """);
        Path databaseFile = databaseDir.resolve("archive-system.sql");
        Files.writeString(databaseFile, "-- placeholder database dump");
        Files.writeString(configFile, "[]");
        Files.writeString(
                setDir.resolve("checksums.txt"),
                "manifest.json|" + sha256(manifest) + System.lineSeparator()
                        + "database/archive-system.sql|" + sha256(databaseFile) + System.lineSeparator()
                        + "config/sys-config.json|" + sha256(configFile)
        );

        ObjectMapper realObjectMapper = new ObjectMapper().findAndRegisterModules();
        ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .enabled(true)
                .localPath(tempDir.toString())
                .build();
        when(backupTargetMapper.selectById(1L)).thenReturn(target);
        when(configService.getBooleanValue("system.restore.maintenance.mode", true)).thenReturn(false);
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(false);
        when(restoreJobMapper.selectCount(any())).thenReturn(0L, 0L);

        RestoreExecuteRequest request = RestoreExecuteRequest.builder()
                .targetId(1L)
                .backupSetName("BK-20260330150000-kkkk1111")
                .restoreDatabase(false)
                .restoreFiles(false)
                .restoreConfig(true)
                .rebuildIndex(false)
                .exitMaintenanceAfterSuccess(true)
                .confirmationText("RESTORE")
                .build();

        RestoreJob job = backupService.runRestore(request);

        assertEquals(RestoreJob.STATUS_SUCCESS, job.getStatus());
        verify(configService, never()).saveConfig(
                eq("system.runtime.maintenance.enabled"),
                eq("false"),
                eq(SysConfig.GROUP_SYSTEM),
                eq("系统维护模式开关"),
                eq(SysConfig.TYPE_BOOLEAN),
                eq(true),
                eq(74)
        );
        verify(restoreJobMapper, atLeastOnce()).updateById(argThat(updatedJob ->
                RestoreJob.STATUS_SUCCESS.equals(updatedJob.getStatus())
                        && updatedJob.getRestoreReport() != null
                        && !updatedJob.getRestoreReport().contains("\"step\":\"MAINTENANCE\"")
        ));
    }

    @Test
    void testRestoreConfigs_ShouldSkipProtectedRuntimeSecrets() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-config-test");
        Path configDir = Files.createDirectories(tempDir.resolve("config"));
        Path configFile = configDir.resolve("sys-config.json");
        Files.writeString(configFile, "[]");

        SysConfig publicConfig = SysConfig.builder()
                .configKey("system.site.name")
                .configValue("档案系统")
                .configGroup("SITE")
                .configType(SysConfig.TYPE_STRING)
                .editable(true)
                .sortOrder(1)
                .build();
        SysConfig protectedConfig = SysConfig.builder()
                .configKey(RuntimeSecretProvider.KEY_JWT_SECRET)
                .configValue("secret-from-backup")
                .configGroup(SysConfig.GROUP_SYSTEM)
                .configType(SysConfig.TYPE_STRING)
                .editable(false)
                .sortOrder(90)
                .build();

        when(objectMapper.readValue(
                eq(configFile.toFile()),
                any(com.fasterxml.jackson.databind.JavaType.class)))
                .thenReturn(List.of(publicConfig, protectedConfig));
        when(objectMapper.getTypeFactory()).thenReturn(new ObjectMapper().getTypeFactory());

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(backupService, "restoreConfigs", tempDir);

        verify(configService).saveConfig(
                eq("system.site.name"),
                eq("档案系统"),
                eq("SITE"),
                any(),
                eq(SysConfig.TYPE_STRING),
                eq(true),
                eq(1)
        );
        verify(configService, never()).saveConfig(
                eq(RuntimeSecretProvider.KEY_JWT_SECRET),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void testExportSystemConfigs_ShouldExcludeProtectedRuntimeSecrets() throws Exception {
        Path tempDir = Files.createTempDirectory("export-config-test");
        ObjectMapper realObjectMapper = new ObjectMapper();
        org.springframework.test.util.ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);

        SysConfig publicConfig = SysConfig.builder()
                .configKey("system.site.name")
                .configValue("档案系统")
                .configGroup("SITE")
                .configType(SysConfig.TYPE_STRING)
                .editable(true)
                .sortOrder(1)
                .build();
        SysConfig protectedConfig = SysConfig.builder()
                .configKey(RuntimeSecretProvider.KEY_JWT_SECRET)
                .configValue("secret-from-runtime")
                .configGroup(SysConfig.GROUP_SYSTEM)
                .configType(SysConfig.TYPE_STRING)
                .editable(false)
                .sortOrder(90)
                .build();
        when(sysConfigMapper.selectAllOrdered()).thenReturn(List.of(publicConfig, protectedConfig));

        Path output = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                backupService, "exportSystemConfigs", tempDir);

        assertNotNull(output);
        List<?> exported = realObjectMapper.readValue(output.toFile(), List.class);
        assertEquals(1, exported.size());
        assertEquals("system.site.name", ((Map<?, ?>) exported.get(0)).get("configKey"));
    }

    @Test
    void testExportBackupConfig_ShouldExcludeDatasourceUrl() throws Exception {
        Path tempDir = Files.createTempDirectory("export-backup-config");
        ObjectMapper realObjectMapper = new ObjectMapper();
        org.springframework.test.util.ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(
                backupService, "datasourceUrl", "jdbc:postgresql://db.internal:5432/archive?sslmode=disable");

        BackupTarget target = BackupTarget.builder()
                .id(1L)
                .name("本地目录")
                .targetType(BackupTarget.TYPE_LOCAL)
                .build();
        com.archivesystem.entity.BackupJob job = com.archivesystem.entity.BackupJob.builder()
                .backupNo("BK-001")
                .build();

        Path output = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                backupService, "exportBackupConfig", tempDir, target, job);

        assertNotNull(output);
        Map<?, ?> exported = realObjectMapper.readValue(output.toFile(), Map.class);
        assertEquals("BK-001", exported.get("backupNo"));
        assertFalse(exported.containsKey("datasourceUrl"));
        assertFalse(exported.containsKey("minioBucket"));
    }

    @Test
    void testExportDigitalFiles_ShouldFailWhenAnyObjectPathIsIllegal() throws Exception {
        Path filesDir = Files.createTempDirectory("export-digital-files");
        ObjectMapper realObjectMapper = new ObjectMapper();
        org.springframework.test.util.ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);

        DigitalFile safeFile = DigitalFile.builder()
                .id(1L)
                .archiveId(10L)
                .fileName("safe.pdf")
                .storagePath("archives/2026/safe.pdf")
                .deleted(false)
                .build();
        DigitalFile traversalFile = DigitalFile.builder()
                .id(2L)
                .archiveId(10L)
                .fileName("evil.pdf")
                .storagePath("../evil.txt")
                .deleted(false)
                .build();
        when(digitalFileMapper.selectList(any())).thenReturn(List.of(safeFile, traversalFile));
        doAnswer(invocation -> {
            Path target = invocation.getArgument(1);
            Files.createDirectories(target.getParent());
            Files.writeString(target, "backup-data");
            return null;
        }).when(minioService).downloadToFile(anyString(), any(Path.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(backupService, "exportDigitalFiles", filesDir));

        assertTrue(ex.getMessage().contains("部分电子文件备份失败"));
        verify(minioService).downloadToFile(eq("archives/2026/safe.pdf"), any(Path.class));
        verify(minioService, never()).downloadToFile(eq("../evil.txt"), any(Path.class));
        assertTrue(Files.exists(filesDir.resolve("archives/2026/safe.pdf")));
        assertFalse(Files.exists(filesDir.getParent().resolve("evil.txt")));
        assertFalse(Files.exists(filesDir.resolve("files-index.json")));
    }

    @Test
    void testExportDigitalFiles_ShouldNormalizeIndexedObjectPaths() throws Exception {
        Path filesDir = Files.createTempDirectory("export-digital-files-normalized");
        ObjectMapper realObjectMapper = new ObjectMapper();
        org.springframework.test.util.ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);

        DigitalFile file = DigitalFile.builder()
                .id(1L)
                .archiveId(10L)
                .fileName("normalized.pdf")
                .storagePath(" archives/2026/original.pdf ")
                .convertedPath(" archives/2026/converted.pdf ")
                .previewPath("  ")
                .thumbnailPath(null)
                .deleted(false)
                .build();
        when(digitalFileMapper.selectList(any())).thenReturn(List.of(file));
        doAnswer(invocation -> {
            Path target = invocation.getArgument(1);
            Files.createDirectories(target.getParent());
            Files.writeString(target, "backup-data");
            return null;
        }).when(minioService).downloadToFile(anyString(), any(Path.class));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(backupService, "exportDigitalFiles", filesDir);

        verify(minioService).downloadToFile(eq("archives/2026/original.pdf"), any(Path.class));
        verify(minioService).downloadToFile(eq("archives/2026/converted.pdf"), any(Path.class));
        List<?> exported = realObjectMapper.readValue(filesDir.resolve("files-index.json").toFile(), List.class);
        assertEquals(1, exported.size());
        Map<?, ?> item = (Map<?, ?>) exported.get(0);
        assertEquals("archives/2026/original.pdf", item.get("storagePath"));
        assertEquals("archives/2026/converted.pdf", item.get("convertedPath"));
        assertNull(item.get("previewPath"));
        assertNull(item.get("thumbnailPath"));
    }

    @Test
    void testBuildFailureReport_ShouldHideInternalErrorDetails() throws Exception {
        ObjectMapper realObjectMapper = new ObjectMapper();
        org.springframework.test.util.ReflectionTestUtils.setField(backupService, "objectMapper", realObjectMapper);

        String report = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                backupService,
                "buildFailureReport",
                RestoreJob.builder()
                        .restoreNo("RS-001")
                        .backupSetName("BK-001")
                        .startedAt(LocalDateTime.of(2026, 4, 29, 15, 0))
                        .rebuildIndexStatus("FAILED")
                        .build(),
                new java.util.LinkedHashMap<String, Object>()
        );

        assertNotNull(report);
        assertFalse(report.contains("/tmp/restore-output.log"));
        assertTrue(report.contains("恢复执行失败，请联系系统管理员查看系统日志"));
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Files.readAllBytes(path));
        return toHex(digest.digest());
    }

    private String sha256Bytes(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(content);
        return toHex(digest.digest());
    }

    private String toHex(byte[] digestBytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : digestBytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
