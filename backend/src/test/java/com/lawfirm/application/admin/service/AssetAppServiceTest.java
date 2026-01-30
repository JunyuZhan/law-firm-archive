package com.lawfirm.application.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.AssetReceiveCommand;
import com.lawfirm.application.admin.command.CreateAssetCommand;
import com.lawfirm.application.admin.dto.AssetDTO;
import com.lawfirm.application.admin.dto.AssetRecordDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.Asset;
import com.lawfirm.domain.admin.entity.AssetRecord;
import com.lawfirm.domain.admin.repository.AssetRecordRepository;
import com.lawfirm.domain.admin.repository.AssetRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** AssetAppService 单元测试 测试资产管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetAppService 资产服务测试")
class AssetAppServiceTest {

  private static final Long TEST_ASSET_ID = 100L;
  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_DEPT_ID = 10L;
  private static final Long TEST_RECORD_ID = 200L;

  @Mock private AssetRepository assetRepository;

  @Mock private AssetRecordRepository assetRecordRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private AssetAppService assetAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("查询资产测试")
  class QueryAssetTests {

    @Test
    @DisplayName("应该成功分页查询资产")
    void listAssets_shouldSuccess() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .assetNo("AST2024001")
              .name("笔记本电脑")
              .category("IT设备")
              .status("IDLE")
              .build();

      Page<Asset> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(asset));
      page.setTotal(1L);

      @SuppressWarnings("unchecked")
      Page<Asset> pageParam = any(Page.class);
      when(assetRepository.findPage(pageParam, any(), any(), any(), any(), any())).thenReturn(page);
      lenient().when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

      PageQuery query = new PageQuery();
      query.setPageNum(1);
      query.setPageSize(10);

      // When
      PageResult<AssetDTO> result = assetAppService.listAssets(query, null, null, null, null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("应该成功获取资产详情")
    void getAssetById_shouldSuccess() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .assetNo("AST2024001")
              .name("笔记本电脑")
              .category("IT设备")
              .status("IDLE")
              .build();

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);

      // When
      AssetDTO result = assetAppService.getAssetById(TEST_ASSET_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getAssetNo()).isEqualTo("AST2024001");
      assertThat(result.getName()).isEqualTo("笔记本电脑");
    }

    @Test
    @DisplayName("资产不存在应该失败")
    void getAssetById_shouldFail_whenNotFound() {
      // Given
      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> assetAppService.getAssetById(TEST_ASSET_ID));
      assertThat(exception.getMessage()).contains("资产不存在");
    }
  }

  @Nested
  @DisplayName("创建资产测试")
  class CreateAssetTests {

    @Test
    @DisplayName("应该成功创建资产")
    void createAsset_shouldSuccess() {
      // Given
      CreateAssetCommand command = new CreateAssetCommand();
      command.setName("新笔记本电脑");
      command.setCategory("IT设备");
      command.setBrand("联想");
      command.setModel("ThinkPad X1");
      command.setPurchaseDate(LocalDate.now());
      command.setPurchasePrice(BigDecimal.valueOf(8000));
      command.setLocation("3楼办公室");
      command.setDepartmentId(TEST_DEPT_ID);

      when(assetRepository.save(any(Asset.class)))
          .thenAnswer(
              invocation -> {
                Asset asset = invocation.getArgument(0);
                asset.setId(TEST_ASSET_ID);
                asset.setAssetNo("AST2024001");
                return true;
              });

      // When
      AssetDTO result = assetAppService.createAsset(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getName()).isEqualTo("新笔记本电脑");
      assertThat(result.getStatus()).isEqualTo("IDLE");
      assertThat(result.getAssetNo()).isNotNull();
      verify(assetRepository).save(any(Asset.class));
    }
  }

  @Nested
  @DisplayName("更新资产测试")
  class UpdateAssetTests {

    @Test
    @DisplayName("应该成功更新资产")
    void updateAsset_shouldSuccess() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .assetNo("AST2024001")
              .name("原名称")
              .status("IDLE")
              .build();

      CreateAssetCommand command = new CreateAssetCommand();
      command.setName("新名称");
      command.setCategory("IT设备");
      command.setLocation("新位置");

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);
      when(assetRepository.updateById(any(Asset.class))).thenReturn(true);

      // When
      AssetDTO result = assetAppService.updateAsset(TEST_ASSET_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(asset.getName()).isEqualTo("新名称");
      assertThat(asset.getLocation()).isEqualTo("新位置");
      verify(assetRepository).updateById(asset);
    }

    @Test
    @DisplayName("资产不存在应该失败")
    void updateAsset_shouldFail_whenNotFound() {
      // Given
      CreateAssetCommand command = new CreateAssetCommand();

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> assetAppService.updateAsset(TEST_ASSET_ID, command));
      assertThat(exception.getMessage()).contains("资产不存在");
    }
  }

  @Nested
  @DisplayName("资产领用测试")
  class ReceiveAssetTests {

    @Test
    @DisplayName("应该成功领用资产")
    void receiveAsset_shouldSuccess() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .assetNo("AST2024001")
              .name("笔记本电脑")
              .status("IDLE")
              .build();

      AssetReceiveCommand command = new AssetReceiveCommand();
      command.setAssetId(TEST_ASSET_ID);
      command.setUserId(TEST_USER_ID);
      command.setReason("工作需要");
      command.setRemarks("备注");

      command.setAssetId(TEST_ASSET_ID);
      command.setUserId(TEST_USER_ID);

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);
      securityUtilsMock.when(() -> SecurityUtils.hasRole(anyString())).thenReturn(false);
      when(assetRepository.updateById(any(Asset.class))).thenReturn(true);
      when(assetRecordRepository.save(any(AssetRecord.class)))
          .thenAnswer(
              invocation -> {
                AssetRecord record = invocation.getArgument(0);
                record.setId(TEST_RECORD_ID);
                return true;
              });

      // When
      assetAppService.receiveAsset(command);

      // Then
      assertThat(asset.getStatus()).isEqualTo("IN_USE");
      assertThat(asset.getCurrentUserId()).isEqualTo(TEST_USER_ID);
      verify(assetRepository).updateById(asset);
      verify(assetRecordRepository).save(any(AssetRecord.class));
    }

    @Test
    @DisplayName("已使用的资产不能重复领用")
    void receiveAsset_shouldFail_whenInUse() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .status("IN_USE") // 已使用
              .currentUserId(999L)
              .build();

      AssetReceiveCommand command = new AssetReceiveCommand();

      command.setAssetId(TEST_ASSET_ID);

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> assetAppService.receiveAsset(command));
      assertThat(exception.getMessage()).contains("不可领用");
    }
  }

  @Nested
  @DisplayName("资产归还测试")
  class ReturnAssetTests {

    @Test
    @DisplayName("应该成功归还资产")
    void returnAsset_shouldSuccess() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .assetNo("AST2024001")
              .name("笔记本电脑")
              .status("IN_USE")
              .currentUserId(TEST_USER_ID)
              .build();

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);
      securityUtilsMock.when(() -> SecurityUtils.hasRole(anyString())).thenReturn(false);
      when(assetRepository.updateById(any(Asset.class))).thenReturn(true);
      when(assetRecordRepository.save(any(AssetRecord.class)))
          .thenAnswer(
              invocation -> {
                AssetRecord record = invocation.getArgument(0);
                record.setId(TEST_RECORD_ID);
                return true;
              });

      // When
      assetAppService.returnAsset(TEST_ASSET_ID, "归还备注");

      // Then
      assertThat(asset.getStatus()).isEqualTo("IDLE");
      assertThat(asset.getCurrentUserId()).isNull();
      verify(assetRepository).updateById(asset);
      verify(assetRecordRepository).save(any(AssetRecord.class));
    }

    @Test
    @DisplayName("未使用的资产不能归还")
    void returnAsset_shouldFail_whenNotInUse() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .status("IDLE") // 未使用
              .build();

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> assetAppService.returnAsset(TEST_ASSET_ID, "备注"));
      assertThat(exception.getMessage()).contains("不在使用中");
    }
  }

  @Nested
  @DisplayName("资产记录测试")
  class AssetRecordTests {

    @Test
    @DisplayName("应该成功获取资产记录列表")
    void getAssetRecords_shouldSuccess() {
      // Given
      AssetRecord record =
          AssetRecord.builder()
              .id(TEST_RECORD_ID)
              .assetId(TEST_ASSET_ID)
              .recordType("RECEIVE")
              .operatorId(TEST_USER_ID)
              .build();

      Asset asset = Asset.builder().id(TEST_ASSET_ID).assetNo("AST2024001").name("笔记本电脑").build();

      when(assetRecordRepository.findByAssetId(TEST_ASSET_ID))
          .thenReturn(Collections.singletonList(record));
      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);

      // When
      List<AssetRecordDTO> result = assetAppService.getAssetRecords(TEST_ASSET_ID);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getRecordType()).isEqualTo("RECEIVE");
    }
  }

  @Nested
  @DisplayName("删除资产测试")
  class DeleteAssetTests {

    @Test
    @DisplayName("应该成功删除未使用的资产")
    void deleteAsset_shouldSuccess() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .status("IDLE") // 未使用
              .build();

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);
      when(assetRecordRepository.countByAssetId(TEST_ASSET_ID)).thenReturn(0L);
      when(assetRepository.removeById(TEST_ASSET_ID)).thenReturn(true);

      // When
      assetAppService.deleteAsset(TEST_ASSET_ID);

      // Then
      verify(assetRepository).removeById(TEST_ASSET_ID);
    }

    @Test
    @DisplayName("已使用的资产不能删除")
    void deleteAsset_shouldFail_whenInUse() {
      // Given
      Asset asset =
          Asset.builder()
              .id(TEST_ASSET_ID)
              .status("IN_USE") // 已使用
              .build();

      when(assetRepository.getById(TEST_ASSET_ID)).thenReturn(asset);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> assetAppService.deleteAsset(TEST_ASSET_ID));
      assertThat(exception.getMessage()).contains("使用中");
    }
  }
}
