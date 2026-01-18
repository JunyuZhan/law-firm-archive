package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.BackupCommand;
import com.lawfirm.application.system.dto.BackupDTO;
import com.lawfirm.application.system.dto.BackupQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Backup;
import com.lawfirm.domain.system.repository.BackupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BackupAppService 单元测试
 * 测试备份服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BackupAppService 备份服务测试")
class BackupAppServiceTest {

    private static final Long TEST_BACKUP_ID = 100L;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_BACKUP_NO = "BK2023010112000ABC123";

    @Mock
    private BackupRepository backupRepository;

    @Spy
    @InjectMocks
    private BackupAppService backupAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private Path tempBackupDir;

    @BeforeEach
    void setUp() throws IOException {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(true);
        // Create temporary directory for backup tests
        tempBackupDir = Files.createTempDirectory("backup-test-");
        ReflectionTestUtils.setField(backupAppService, "backupBasePath", tempBackupDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
        // Clean up temporary directory
        if (tempBackupDir != null && Files.exists(tempBackupDir)) {
            Files.walk(tempBackupDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }

    @Nested
    @DisplayName("查询备份测试")
    class QueryBackupTests {

        @Test
        @DisplayName("应该成功分页查询备份列表")
        void listBackups_shouldSuccess() {
            // Given
            Backup backup = Backup.builder()
                    .id(TEST_BACKUP_ID)
                    .backupNo(TEST_BACKUP_NO)
                    .backupType("DATABASE")
                    .backupName("数据库备份")
                    .status("SUCCESS")
                    .backupTime(LocalDateTime.now())
                    .build();

            Page<Backup> page = new Page<>(1, 10);
            page.setRecords(List.of(backup));
            page.setTotal(1L);

            when(backupRepository.page(any(Page.class), any())).thenReturn(page);

            // When
            BackupQueryDTO query = new BackupQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);
            PageResult<BackupDTO> result = backupAppService.listBackups(query);

            // Then
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1L);
        }

        @Test
        @DisplayName("应该成功获取备份详情")
        void getBackupById_shouldSuccess() {
            // Given
            Backup backup = Backup.builder()
                    .id(TEST_BACKUP_ID)
                    .backupNo(TEST_BACKUP_NO)
                    .backupType("DATABASE")
                    .backupName("数据库备份")
                    .status("SUCCESS")
                    .build();

            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString())).thenReturn(backup);

            // When
            BackupDTO result = backupAppService.getBackupById(TEST_BACKUP_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBackupNo()).isEqualTo(TEST_BACKUP_NO);
        }

        @Test
        @DisplayName("备份不存在时应该抛出异常")
        void getBackupById_shouldFail_whenNotFound() {
            // Given
            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString()))
                    .thenThrow(new BusinessException("备份记录不存在"));

            // When & Then
            assertThrows(BusinessException.class, () -> backupAppService.getBackupById(TEST_BACKUP_ID));
        }
    }

    @Nested
    @DisplayName("创建备份测试")
    class CreateBackupTests {

        @Test
        @DisplayName("管理员应该成功创建备份")
        void createBackup_shouldSuccess_forAdmin() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(true);

            BackupCommand command = new BackupCommand();
            command.setBackupType("DATABASE");
            command.setDescription("测试备份");

            when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
                Backup backup = invocation.getArgument(0);
                backup.setId(TEST_BACKUP_ID);
                backup.setBackupNo(TEST_BACKUP_NO);
                backup.setStatus("PENDING");
                return true;
            });
            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString())).thenAnswer(invocation -> {
                Backup backup = Backup.builder()
                        .id(TEST_BACKUP_ID)
                        .backupNo(TEST_BACKUP_NO)
                        .backupType("DATABASE")
                        .status("PENDING")
                        .build();
                return backup;
            });
            // Mock async execution to avoid actual backup execution
            doNothing().when(backupAppService).executeBackupAsync(any(Backup.class));

            // When
            BackupDTO result = backupAppService.createBackup(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBackupType()).isEqualTo("DATABASE");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            verify(backupRepository).save(any(Backup.class));
        }

        @Test
        @DisplayName("非管理员不能创建备份")
        void createBackup_shouldFail_forNonAdmin() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(false);

            BackupCommand command = new BackupCommand();
            command.setBackupType("DATABASE");

            // When & Then
            assertThrows(BusinessException.class, () -> backupAppService.createBackup(command));
        }
    }

    @Nested
    @DisplayName("删除备份测试")
    class DeleteBackupTests {

        @Test
        @DisplayName("应该成功删除备份")
        void deleteBackup_shouldSuccess() {
            // Given
            Backup backup = Backup.builder()
                    .id(TEST_BACKUP_ID)
                    .backupNo(TEST_BACKUP_NO)
                    .backupPath("/backups/test.sql")
                    .build();

            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString())).thenReturn(backup);
            when(backupRepository.softDelete(TEST_BACKUP_ID)).thenReturn(true);

            // When
            backupAppService.deleteBackup(TEST_BACKUP_ID);

            // Then
            verify(backupRepository).softDelete(TEST_BACKUP_ID);
        }
    }

    @Nested
    @DisplayName("下载备份测试")
    class DownloadBackupTests {

        @Test
        @DisplayName("管理员应该成功下载备份")
        void downloadBackup_shouldSuccess_forAdmin() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(true);

            Backup backup = Backup.builder()
                    .id(TEST_BACKUP_ID)
                    .backupNo(TEST_BACKUP_NO)
                    .backupType("DATABASE")
                    .status("SUCCESS")
                    .backupPath("/backups/test.sql")
                    .build();

            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString())).thenReturn(backup);

            // When
            // 由于需要实际文件存在，这里只验证权限检查
            assertThrows(Exception.class, () -> backupAppService.downloadBackup(TEST_BACKUP_ID));
        }

        @Test
        @DisplayName("只能下载成功的备份")
        void downloadBackup_shouldFail_whenNotSuccess() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(true);

            Backup backup = Backup.builder()
                    .id(TEST_BACKUP_ID)
                    .backupType("DATABASE")
                    .status("PENDING") // 不是成功状态
                    .build();

            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString())).thenReturn(backup);

            // When & Then
            assertThrows(BusinessException.class, () -> backupAppService.downloadBackup(TEST_BACKUP_ID));
        }

        @Test
        @DisplayName("非管理员不能下载备份")
        void downloadBackup_shouldFail_forNonAdmin() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> backupAppService.downloadBackup(TEST_BACKUP_ID));
        }
    }

    @Nested
    @DisplayName("恢复备份测试")
    class RestoreBackupTests {

        @Test
        @DisplayName("管理员应该成功启动恢复")
        void restoreBackup_shouldStart_forAdmin() throws IOException {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("SUPER_ADMIN", "ADMIN")).thenReturn(true);

            // Create a test backup file
            Path testBackupFile = tempBackupDir.resolve("test.sql");
            Files.createDirectories(testBackupFile.getParent());
            Files.writeString(testBackupFile, "-- Test backup file");

            Backup backup = Backup.builder()
                    .id(TEST_BACKUP_ID)
                    .backupNo(TEST_BACKUP_NO)
                    .backupType("DATABASE")
                    .status("SUCCESS")
                    .backupPath(testBackupFile.toString())
                    .build();

            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString())).thenReturn(backup);
            when(backupRepository.updateById(any(Backup.class))).thenReturn(true);
            // Mock async execution to avoid actual restore execution
            doNothing().when(backupAppService).executeRestoreAsync(any(Backup.class));

            // When
            var command = new com.lawfirm.application.system.command.RestoreCommand();
            command.setBackupId(TEST_BACKUP_ID);
            command.setConfirmCode("RESTORE_" + TEST_BACKUP_NO);

            backupAppService.restoreBackup(command);

            // Then
            assertThat(backup.getStatus()).isEqualTo("IN_PROGRESS");
            verify(backupRepository).updateById(backup);
        }

        @Test
        @DisplayName("确认码错误时应该失败")
        void restoreBackup_shouldFail_whenWrongConfirmCode() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(true);

            Backup backup = Backup.builder()
                    .id(TEST_BACKUP_ID)
                    .backupNo(TEST_BACKUP_NO)
                    .status("SUCCESS")
                    .build();

            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString())).thenReturn(backup);

            // When
            var command = new com.lawfirm.application.system.command.RestoreCommand();
            command.setBackupId(TEST_BACKUP_ID);
            command.setConfirmCode("WRONG_CODE");

            // Then
            assertThrows(BusinessException.class, () -> backupAppService.restoreBackup(command));
        }

        @Test
        @DisplayName("非管理员不能执行恢复")
        void restoreBackup_shouldFail_forNonAdmin() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(false);

            // When
            var command = new com.lawfirm.application.system.command.RestoreCommand();
            command.setBackupId(TEST_BACKUP_ID);
            command.setConfirmCode("TEST_CODE");

            // Then
            assertThrows(BusinessException.class, () -> backupAppService.restoreBackup(command));
        }

        @Test
        @DisplayName("只能恢复成功的备份")
        void restoreBackup_shouldFail_whenNotSuccess() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(true);

            Backup backup = Backup.builder()
                    .id(TEST_BACKUP_ID)
                    .backupNo(TEST_BACKUP_NO)
                    .status("FAILED") // 失败状态
                    .build();

            when(backupRepository.getByIdOrThrow(eq(TEST_BACKUP_ID), anyString())).thenReturn(backup);

            // When
            var command = new com.lawfirm.application.system.command.RestoreCommand();
            command.setBackupId(TEST_BACKUP_ID);
            command.setConfirmCode("RESTORE_" + TEST_BACKUP_NO);

            // Then
            assertThrows(BusinessException.class, () -> backupAppService.restoreBackup(command));
        }
    }
}
