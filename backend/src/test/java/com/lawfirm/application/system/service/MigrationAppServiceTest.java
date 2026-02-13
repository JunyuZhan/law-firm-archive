package com.lawfirm.application.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.MigrationDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Migration;
import com.lawfirm.domain.system.repository.MigrationRepository;
import com.lawfirm.infrastructure.persistence.mapper.MigrationMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** MigrationAppService 单元测试 测试数据库迁移服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MigrationAppService 数据库迁移服务测试")
class MigrationAppServiceTest {

  private static final Long TEST_MIGRATION_ID = 100L;
  private static final Long TEST_USER_ID = 1L;
  private static final String TEST_VERSION = "V1.0.1";

  @Mock private MigrationRepository migrationRepository;

  @Mock private MigrationMapper migrationMapper;

  @Mock private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  @Mock private SysConfigAppService configAppService;

  @InjectMocks @Spy private MigrationAppService migrationAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;
  private Path tempMigrationDir;

  @BeforeEach
  void setUp() throws IOException {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
    securityUtilsMock
        .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
        .thenReturn(true);
    // Mock two-parameter hasAnyRole for scanMigrationScripts
    securityUtilsMock
        .when(() -> SecurityUtils.hasAnyRole(eq("ADMIN"), eq("SUPER_ADMIN")))
        .thenReturn(true);
    securityUtilsMock
        .when(() -> SecurityUtils.hasAnyRole(eq("SUPER_ADMIN"), eq("ADMIN")))
        .thenReturn(true);
    // Create temporary directory for migration scripts
    tempMigrationDir = Files.createTempDirectory("migration-test-");
    ReflectionTestUtils.setField(migrationAppService, "migrationPath", tempMigrationDir.toString());
  }

  @AfterEach
  void tearDown() throws IOException {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
    // Clean up temporary directory
    if (tempMigrationDir != null && Files.exists(tempMigrationDir)) {
      Files.walk(tempMigrationDir)
          .sorted((a, b) -> b.compareTo(a))
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException e) {
                  // Ignore cleanup errors
                }
              });
    }
  }

  @Nested
  @DisplayName("扫描迁移脚本测试")
  class ScanMigrationTests {

    @Test
    @DisplayName("管理员应该成功扫描迁移脚本")
    void scanMigrationScripts_shouldSuccess_forAdmin() throws IOException {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN"))
          .thenReturn(true);
      when(migrationMapper.selectByVersions(anyList())).thenReturn(List.of());

      // Create a test migration script file
      Path testScript = tempMigrationDir.resolve("V1.0.1-test.sql");
      Files.writeString(testScript, "-- Test migration script\nCREATE TABLE test (id INT);");

      // When
      List<MigrationDTO> result = migrationAppService.scanMigrationScripts();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("非管理员不能扫描迁移脚本")
    void scanMigrationScripts_shouldFail_forNonAdmin() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(eq("ADMIN"), eq("SUPER_ADMIN")))
          .thenReturn(false);

      // When & Then
      assertThrows(BusinessException.class, () -> migrationAppService.scanMigrationScripts());
    }
  }

  @Nested
  @DisplayName("执行迁移测试")
  class ExecuteMigrationTests {

    @Test
    @DisplayName("管理员应该成功执行迁移")
    void executeMigration_shouldSuccess_forAdmin() throws IOException {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(eq("SUPER_ADMIN"), eq("ADMIN")))
          .thenReturn(true);

      // Create a test migration script file
      Path testScript = tempMigrationDir.resolve("V1.0.1-test.sql");
      Files.writeString(testScript, "-- Test migration script\nCREATE TABLE test (id INT);");

      when(migrationMapper.selectByVersions(anyList())).thenReturn(List.of());
      when(configAppService.getConfigValue("sys.maintenance.enabled")).thenReturn("true");
      when(migrationMapper.selectByVersion(TEST_VERSION)).thenReturn(null);
      when(migrationRepository.save(any(Migration.class)))
          .thenAnswer(
              invocation -> {
                Migration m = invocation.getArgument(0);
                m.setId(TEST_MIGRATION_ID);
                return true;
              });
      when(migrationRepository.updateById(any(Migration.class))).thenReturn(true);
      doNothing().when(jdbcTemplate).execute(anyString());

      // When
      MigrationDTO result = migrationAppService.executeMigration(TEST_VERSION, "MIGRATE_V1_0_1");

      // Then
      assertThat(result).isNotNull();
      verify(migrationRepository).save(any(Migration.class));
    }

    @Test
    @DisplayName("确认码错误时应该失败")
    void executeMigration_shouldFail_whenWrongConfirmCode() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("SUPER_ADMIN", "ADMIN"))
          .thenReturn(true);

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> migrationAppService.executeMigration(TEST_VERSION, "WRONG_CODE"));
    }

    @Test
    @DisplayName("未开启维护模式时应该失败")
    void executeMigration_shouldFail_whenMaintenanceNotEnabled() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("SUPER_ADMIN", "ADMIN"))
          .thenReturn(true);
      when(configAppService.getConfigValue("sys.maintenance.enabled")).thenReturn("false");

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> migrationAppService.executeMigration(TEST_VERSION, "MIGRATE_V1_0_1"));
    }

    @Test
    @DisplayName("非管理员不能执行迁移")
    void executeMigration_shouldFail_forNonAdmin() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("SUPER_ADMIN", "ADMIN"))
          .thenReturn(false);

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> migrationAppService.executeMigration(TEST_VERSION, "MIGRATE_V1_0_1"));
    }

    @Test
    @DisplayName("已成功执行的迁移不能重复执行")
    void executeMigration_shouldFail_whenAlreadyExecuted() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("SUPER_ADMIN", "ADMIN"))
          .thenReturn(true);

      Migration existing =
          Migration.builder()
              .id(TEST_MIGRATION_ID)
              .schemaVersion(TEST_VERSION)
              .status(Migration.STATUS_SUCCESS)
              .build();

      when(configAppService.getConfigValue("sys.maintenance.enabled")).thenReturn("true");
      when(migrationMapper.selectByVersion(TEST_VERSION)).thenReturn(existing);

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> migrationAppService.executeMigration(TEST_VERSION, "MIGRATE_V1_0_1"));
    }
  }

  @Nested
  @DisplayName("查询迁移记录测试")
  class QueryMigrationTests {

    @Test
    @DisplayName("管理员应该成功分页查询迁移记录")
    void listMigrations_shouldSuccess_forAdmin() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(eq("ADMIN"), eq("SUPER_ADMIN")))
          .thenReturn(true);

      Migration migration =
          Migration.builder()
              .id(TEST_MIGRATION_ID)
              .schemaVersion(TEST_VERSION)
              .status(Migration.STATUS_SUCCESS)
              .executedAt(LocalDateTime.now())
              .build();

      Page<Migration> page = new Page<>(1, 10);
      page.setRecords(List.of(migration));
      page.setTotal(1L);

      when(migrationMapper.selectMigrationPage(ArgumentMatchers.<Page<Migration>>any()))
          .thenReturn(page);

      // When
      PageResult<MigrationDTO> result = migrationAppService.listMigrations(1, 10);

      // Then
      assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    @DisplayName("非管理员不能查询迁移记录")
    void listMigrations_shouldFail_forNonAdmin() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(eq("ADMIN"), eq("SUPER_ADMIN")))
          .thenReturn(false);

      // When & Then
      assertThrows(BusinessException.class, () -> migrationAppService.listMigrations(1, 10));
    }

    @Test
    @DisplayName("管理员应该成功获取迁移详情")
    void getMigrationById_shouldSuccess_forAdmin() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(eq("ADMIN"), eq("SUPER_ADMIN")))
          .thenReturn(true);

      Migration migration =
          Migration.builder()
              .id(TEST_MIGRATION_ID)
              .schemaVersion(TEST_VERSION)
              .status(Migration.STATUS_SUCCESS)
              .build();

      when(migrationRepository.getByIdOrThrow(eq(TEST_MIGRATION_ID), anyString()))
          .thenReturn(migration);

      // When
      MigrationDTO result = migrationAppService.getMigrationById(TEST_MIGRATION_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getVersion()).isEqualTo(TEST_VERSION);
    }

    @Test
    @DisplayName("非管理员不能获取迁移详情")
    void getMigrationById_shouldFail_forNonAdmin() {
      // Given
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(eq("ADMIN"), eq("SUPER_ADMIN")))
          .thenReturn(false);

      // When & Then
      assertThrows(
          BusinessException.class, () -> migrationAppService.getMigrationById(TEST_MIGRATION_ID));
    }
  }
}
