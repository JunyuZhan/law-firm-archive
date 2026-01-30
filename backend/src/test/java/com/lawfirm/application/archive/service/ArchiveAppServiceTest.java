package com.lawfirm.application.archive.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.archive.command.CreateArchiveCommand;
import com.lawfirm.application.archive.command.StoreArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveDTO;
import com.lawfirm.application.archive.service.ArchiveDataCollectorService.ArchiveDataSnapshot;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.archive.entity.Archive;
import com.lawfirm.domain.archive.entity.ArchiveLocation;
import com.lawfirm.domain.archive.entity.ArchiveOperationLog;
import com.lawfirm.domain.archive.repository.ArchiveLocationRepository;
import com.lawfirm.domain.archive.repository.ArchiveOperationLogRepository;
import com.lawfirm.domain.archive.repository.ArchiveRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.external.document.DossierCoverGenerator;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.ArchiveMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** ArchiveAppService 单元测试 测试归档管理核心业务逻辑 */
@ExtendWith(MockitoExtension.class)
class ArchiveAppServiceTest {

  @Mock private ArchiveRepository archiveRepository;

  @Mock private ArchiveMapper archiveMapper;

  @Mock private ArchiveLocationRepository locationRepository;

  @Mock private ArchiveOperationLogRepository operationLogRepository;

  @Mock private MatterRepository matterRepository;

  @Mock private ArchiveDataCollectorService dataCollectorService;

  @Mock private DossierCoverGenerator coverGenerator;

  @Mock private MinioService minioService;

  @Mock private ApprovalService approvalService;

  @Mock private ApproverService approverService;

  @Mock private MatterAppService matterAppService;

  @InjectMocks private ArchiveAppService archiveAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
    securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(1L);
    securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");

    archiveAppService.setMatterAppService(matterAppService);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  // ==================== 创建档案测试 ====================

  @Nested
  @DisplayName("创建档案测试")
  class CreateArchiveTests {

    @Test
    @DisplayName("成功创建档案")
    void createArchive_Success() throws Exception {
      // Given
      Matter matter =
          Matter.builder()
              .id(1L)
              .matterNo("M2024001")
              .name("合同纠纷案")
              .matterType("LITIGATION")
              .status("CLOSED")
              .actualClosingDate(LocalDate.of(2024, 1, 15))
              .build();

      CreateArchiveCommand command = new CreateArchiveCommand();
      command.setMatterId(1L);
      command.setArchiveName("合同纠纷案档案");
      command.setArchiveType("LITIGATION");
      command.setVolumeCount(2);
      command.setPageCount(150);

      ArchiveDataSnapshot snapshot = new ArchiveDataSnapshot();
      snapshot.setMatterId(1L);
      // 注意：ArchiveDataSnapshot没有setStatistics方法，只有getStatistics()
      // 统计数据是通过其他字段自动计算的

      when(matterRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(matter);
      when(archiveRepository.count(any())).thenReturn(0L);
      when(dataCollectorService.collectMatterData(1L)).thenReturn(snapshot);
      when(dataCollectorService.snapshotToJson(snapshot)).thenReturn("{\"matterId\":1}");
      when(dataCollectorService.jsonToSnapshot(anyString())).thenReturn(snapshot);
      when(archiveRepository.save(any(Archive.class))).thenReturn(true);
      when(coverGenerator.generateCover(any(), anyString(), anyInt(), anyInt(), anyString()))
          .thenReturn(new byte[] {1, 2, 3});
      when(minioService.uploadBytes(any(), anyString(), anyString()))
          .thenReturn("http://minio/cover.pdf");
      when(archiveRepository.updateById(any(Archive.class))).thenReturn(true);
      when(operationLogRepository.save(any(ArchiveOperationLog.class))).thenReturn(true);

      // When
      ArchiveDTO result = archiveAppService.createArchive(command);

      // Then
      assertNotNull(result);
      assertEquals("合同纠纷案档案", result.getArchiveName());
      assertEquals("LITIGATION", result.getArchiveType());
      assertEquals(2, result.getVolumeCount());
      assertEquals(150, result.getPageCount());
      assertEquals("PENDING", result.getStatus());
      verify(archiveRepository).save(any(Archive.class));
      verify(operationLogRepository).save(any(ArchiveOperationLog.class));
    }

    @Test
    @DisplayName("项目状态不正确时创建失败")
    void createArchive_InvalidMatterStatus() {
      // Given
      Matter matter =
          Matter.builder()
              .id(1L)
              .status("ACTIVE") // 非结案状态
              .build();

      CreateArchiveCommand command = new CreateArchiveCommand();
      command.setMatterId(1L);

      when(matterRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(matter);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> archiveAppService.createArchive(command));
      assertTrue(exception.getMessage().contains("已结案"));
    }
  }

  // ==================== 档案入库测试 ====================

  @Nested
  @DisplayName("档案入库测试")
  class StoreArchiveTests {

    @Test
    @DisplayName("成功入库档案")
    void storeArchive_Success() {
      // Given
      Archive archive =
          Archive.builder().id(1L).archiveNo("DA240115A1B2").status("PENDING").build();

      ArchiveLocation location =
          ArchiveLocation.builder()
              .id(1L)
              .locationCode("A-01-001")
              .status("AVAILABLE")
              .totalCapacity(100)
              .usedCapacity(50)
              .build();

      StoreArchiveCommand command = new StoreArchiveCommand();
      command.setArchiveId(1L);
      command.setLocationId(1L);
      command.setBoxNo("BOX-001");

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);
      when(locationRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(location);
      when(archiveRepository.updateById(any(Archive.class))).thenReturn(true);
      when(locationRepository.updateById(any(ArchiveLocation.class))).thenReturn(true);
      when(operationLogRepository.save(any(ArchiveOperationLog.class))).thenReturn(true);

      // When
      archiveAppService.storeArchive(command);

      // Then
      assertEquals("STORED", archive.getStatus());
      assertEquals(1L, archive.getLocationId());
      assertEquals("BOX-001", archive.getBoxNo());
      assertNotNull(archive.getStoredAt());
      assertEquals(51, location.getUsedCapacity());
      verify(operationLogRepository).save(any(ArchiveOperationLog.class));
    }

    @Test
    @DisplayName("库位已满时入库失败")
    void storeArchive_LocationFull() {
      // Given
      Archive archive = Archive.builder().id(1L).status("PENDING").build();

      ArchiveLocation location =
          ArchiveLocation.builder()
              .id(1L)
              .status("AVAILABLE")
              .totalCapacity(100)
              .usedCapacity(100) // 已满
              .build();

      StoreArchiveCommand command = new StoreArchiveCommand();
      command.setArchiveId(1L);
      command.setLocationId(1L);

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);
      when(locationRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(location);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> archiveAppService.storeArchive(command));
      assertEquals("库位已满，请选择其他库位", exception.getMessage());
    }

    @Test
    @DisplayName("档案状态不正确时入库失败")
    void storeArchive_InvalidStatus() {
      // Given
      Archive archive =
          Archive.builder()
              .id(1L)
              .status("STORED") // 已入库
              .build();

      StoreArchiveCommand command = new StoreArchiveCommand();
      command.setArchiveId(1L);

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> archiveAppService.storeArchive(command));
      assertEquals("只有待入库的档案才能入库", exception.getMessage());
    }
  }

  // ==================== 档案迁移测试 ====================

  @Nested
  @DisplayName("档案迁移测试")
  class MigrateArchiveTests {

    @Test
    @DisplayName("成功申请迁移档案")
    void applyMigrate_Success() {
      // Given
      Archive archive = Archive.builder().id(1L).archiveNo("DA240115A1B2").status("STORED").build();

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);
      when(archiveRepository.updateById(any(Archive.class))).thenReturn(true);
      when(operationLogRepository.save(any(ArchiveOperationLog.class))).thenReturn(true);

      // When
      archiveAppService.applyMigrate(1L, "保管期限到期", "外部存储");

      // Then
      assertEquals("PENDING_MIGRATE", archive.getStatus());
      assertEquals("保管期限到期", archive.getMigrateReason());
      assertEquals("外部存储", archive.getMigrateTarget());
      verify(operationLogRepository).save(any(ArchiveOperationLog.class));
    }

    @Test
    @DisplayName("审批通过迁移档案")
    void approveMigrate_Approved() {
      // Given
      Archive archive =
          Archive.builder()
              .id(1L)
              .archiveNo("DA240115A1B2")
              .status("PENDING_MIGRATE")
              .locationId(1L)
              .migrateTarget("外部存储")
              .build();

      ArchiveLocation location = ArchiveLocation.builder().id(1L).usedCapacity(10).build();

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);
      when(locationRepository.findById(1L)).thenReturn(location);
      when(archiveRepository.updateById(any(Archive.class))).thenReturn(true);
      when(locationRepository.updateById(any(ArchiveLocation.class))).thenReturn(true);
      when(operationLogRepository.save(any(ArchiveOperationLog.class))).thenReturn(true);

      // When
      archiveAppService.approveMigrate(1L, true, "审批通过", false);

      // Then
      assertEquals("MIGRATED", archive.getStatus());
      assertNotNull(archive.getMigrateDate());
      assertEquals(9, location.getUsedCapacity()); // 释放库位
      assertFalse(archive.getFilesDeleted());
    }

    @Test
    @DisplayName("审批拒绝迁移档案")
    void approveMigrate_Rejected() {
      // Given
      Archive archive =
          Archive.builder()
              .id(1L)
              .status("PENDING_MIGRATE")
              .migrateReason("测试原因")
              .migrateTarget("测试目标")
              .build();

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);
      when(archiveRepository.updateById(any(Archive.class))).thenReturn(true);
      when(operationLogRepository.save(any(ArchiveOperationLog.class))).thenReturn(true);

      // When
      archiveAppService.approveMigrate(1L, false, "不符合条件", false);

      // Then
      assertEquals("STORED", archive.getStatus());
      assertNull(archive.getMigrateReason());
      assertNull(archive.getMigrateTarget());
    }
  }

  // ==================== 保管期限测试 ====================

  @Nested
  @DisplayName("保管期限测试")
  class RetentionPeriodTests {

    @Test
    @DisplayName("成功设置保管期限")
    void setRetentionPeriod_Success() {
      // Given
      Archive archive =
          Archive.builder()
              .id(1L)
              .archiveNo("DA240115A1B2")
              .status("STORED")
              .caseCloseDate(LocalDate.of(2024, 1, 15))
              .build();

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);
      when(archiveRepository.updateById(any(Archive.class))).thenReturn(true);
      when(operationLogRepository.save(any(ArchiveOperationLog.class))).thenReturn(true);

      // When
      ArchiveDTO result = archiveAppService.setRetentionPeriod(1L, "15_YEARS");

      // Then
      assertEquals("15_YEARS", archive.getRetentionPeriod());
      assertEquals(LocalDate.of(2039, 1, 15), archive.getRetentionExpireDate());
      assertNotNull(result);
      verify(operationLogRepository).save(any(ArchiveOperationLog.class));
    }

    @Test
    @DisplayName("永久保管期限设置")
    void setRetentionPeriod_Permanent() {
      // Given
      Archive archive =
          Archive.builder()
              .id(1L)
              .status("STORED")
              .caseCloseDate(LocalDate.of(2024, 1, 15))
              .build();

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);
      when(archiveRepository.updateById(any(Archive.class))).thenReturn(true);
      when(operationLogRepository.save(any(ArchiveOperationLog.class))).thenReturn(true);

      // When
      archiveAppService.setRetentionPeriod(1L, "PERMANENT");

      // Then
      assertEquals("PERMANENT", archive.getRetentionPeriod());
      assertEquals(LocalDate.of(9999, 12, 31), archive.getRetentionExpireDate());
    }

    @Test
    @DisplayName("档案状态不正确时设置保管期限失败")
    void setRetentionPeriod_InvalidStatus() {
      // Given
      Archive archive = Archive.builder().id(1L).status("PENDING").build();

      when(archiveRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(archive);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> archiveAppService.setRetentionPeriod(1L, "10_YEARS"));
      assertEquals("只有已入库的档案才能设置保管期限", exception.getMessage());
    }
  }
}
