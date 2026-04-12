package com.archivesystem.service;

import com.archivesystem.dto.backup.BackupSetResponse;
import com.archivesystem.dto.backup.BackupTargetRequest;
import com.archivesystem.dto.backup.BackupTargetResponse;
import com.archivesystem.dto.backup.RestoreExecuteRequest;
import com.archivesystem.entity.BackupTarget;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BackupJobMapper;
import com.archivesystem.repository.BackupTargetMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.repository.RestoreJobMapper;
import com.archivesystem.repository.SysConfigMapper;
import com.archivesystem.security.SecretCryptoService;
import com.archivesystem.service.impl.BackupServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
/**
 * @author junyuzhan
 */

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
    private ArchiveMapper archiveMapper;

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
        assertEquals("FOUNDATION", result.getCurrentPhase());
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
        assertEquals(BackupTarget.VERIFY_PENDING, response.getVerifyStatus());
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
    void testGetBackupSetsFromLocalTarget() throws Exception {
        Path tempDir = Files.createTempDirectory("backup-set-test");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-aaaa1111"));
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
        when(objectMapper.readValue(any(java.io.File.class), eq(Map.class))).thenReturn(Map.of(
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
        doNothing().when(smbStorageService).verifyWritable(target);
        when(smbStorageService.listDirectories(target)).thenReturn(List.of(
                new SmbStorageService.RemoteDirectoryEntry("BK-20260330150000-aaaa1111", LocalDateTime.of(2026, 3, 30, 15, 0))
        ));
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/manifest.json")).thenReturn(true);
        when(smbStorageService.exists(target, "BK-20260330150000-aaaa1111/checksums.txt")).thenReturn(true);
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/manifest.json"))
                .thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(objectMapper.readValue(any(InputStream.class), eq(Map.class))).thenReturn(Map.of(
                "backupNo", "BK-20260330150000-aaaa1111",
                "createdAt", LocalDateTime.of(2026, 3, 30, 15, 0).toString(),
                "databaseMode", "PG_DUMP",
                "fileCount", 2,
                "objectCount", 4,
                "totalBytes", 4096,
                "filesIndex", "files-index.json"
        ));
        String checksum = sha256Bytes("{}".getBytes());
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/checksums.txt"))
                .thenReturn(new ByteArrayInputStream(("manifest.json|" + checksum).getBytes()));
        when(smbStorageService.openInputStream(target, "BK-20260330150000-aaaa1111/manifest.json"))
                .thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(smbStorageService.buildDisplayPath(target, "BK-20260330150000-aaaa1111"))
                .thenReturn("smb://192.168.50.5/archive/BK-20260330150000-aaaa1111");

        List<BackupSetResponse> sets = backupService.getBackupSets(2L);

        assertEquals(1, sets.size());
        assertEquals("READY", sets.get(0).getVerifyStatus());
        assertEquals(4096L, sets.get(0).getTotalBytes());
        assertTrue(sets.get(0).getBackupSetPath().startsWith("smb://"));
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
    void testRunRestoreRejectsWhenMaintenanceDisabled() throws Exception {
        Path tempDir = Files.createTempDirectory("restore-set-test");
        Path setDir = Files.createDirectories(tempDir.resolve("BK-20260330150000-aaaa1111"));
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
        when(objectMapper.readValue(any(java.io.File.class), eq(Map.class))).thenReturn(Map.of(
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
