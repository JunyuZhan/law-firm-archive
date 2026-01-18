package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreateAssetInventoryCommand;
import com.lawfirm.application.admin.dto.AssetInventoryDTO;
import com.lawfirm.application.admin.dto.AssetInventoryDetailDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.Asset;
import com.lawfirm.domain.admin.entity.AssetInventory;
import com.lawfirm.domain.admin.entity.AssetInventoryDetail;
import com.lawfirm.domain.admin.repository.AssetInventoryRepository;
import com.lawfirm.domain.admin.repository.AssetRepository;
import com.lawfirm.infrastructure.persistence.mapper.AssetInventoryDetailMapper;
import com.lawfirm.infrastructure.persistence.mapper.AssetInventoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AssetInventoryAppService 单元测试
 * 测试资产盘点服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetInventoryAppService 资产盘点服务测试")
class AssetInventoryAppServiceTest {

    private static final Long TEST_INVENTORY_ID = 100L;
    private static final Long TEST_DETAIL_ID = 200L;
    private static final Long TEST_ASSET_ID = 1L;
    private static final Long TEST_USER_ID = 10L;
    private static final Long ADMIN_USER_ID = 999L;

    @Mock
    private AssetInventoryRepository inventoryRepository;

    @Mock
    private AssetInventoryMapper inventoryMapper;

    @Mock
    private AssetInventoryDetailMapper detailMapper;

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetInventoryAppService assetInventoryAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(() -> SecurityUtils.hasRole(anyString())).thenReturn(false);
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("创建盘点测试")
    class CreateInventoryTests {

        @Test
        @DisplayName("应该成功创建全盘")
        void createInventory_shouldSuccess_forFullType() {
            // Given
            CreateAssetInventoryCommand command = new CreateAssetInventoryCommand();
            command.setInventoryDate(LocalDate.now());
            command.setInventoryType(AssetInventory.TYPE_FULL);
            command.setDepartmentId(1L);
            command.setLocation("主仓库");

            Asset asset1 = Asset.builder()
                    .id(TEST_ASSET_ID)
                    .assetNo("A001")
                    .name("电脑")
                    .status("IN_USE")
                    .location("主仓库")
                    .currentUserId(1L)
                    .build();

            Asset asset2 = Asset.builder()
                    .id(2L)
                    .assetNo("A002")
                    .name("打印机")
                    .status("IN_USE")
                    .build();

            when(assetRepository.list()).thenReturn(List.of(asset1, asset2));
            when(inventoryRepository.save(any(AssetInventory.class))).thenAnswer(invocation -> {
                AssetInventory inventory = invocation.getArgument(0);
                inventory.setId(TEST_INVENTORY_ID);
                return true;
            });
            when(detailMapper.insert(any(AssetInventoryDetail.class))).thenReturn(1);
            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenAnswer(invocation -> {
                        AssetInventory inv = createTestInventory(TEST_INVENTORY_ID);
                        inv.setInventoryType(AssetInventory.TYPE_FULL);
                        inv.setTotalCount(2);
                        return inv;
                    });
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID)).thenReturn(Collections.emptyList());

            // When
            AssetInventoryDTO result = assetInventoryAppService.createInventory(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getInventoryType()).isEqualTo(AssetInventory.TYPE_FULL);
            assertThat(result.getTotalCount()).isEqualTo(2);
            verify(inventoryRepository).save(any(AssetInventory.class));
        }

        @Test
        @DisplayName("应该成功创建抽盘")
        void createInventory_shouldSuccess_forPartialType() {
            // Given
            List<Long> assetIds = List.of(TEST_ASSET_ID, 2L);

            CreateAssetInventoryCommand command = new CreateAssetInventoryCommand();
            command.setInventoryDate(LocalDate.now());
            command.setInventoryType(AssetInventory.TYPE_PARTIAL);
            command.setAssetIds(assetIds);

            Asset asset1 = Asset.builder()
                    .id(TEST_ASSET_ID)
                    .assetNo("A001")
                    .name("电脑")
                    .status("IN_USE")
                    .build();

            Asset asset2 = Asset.builder()
                    .id(2L)
                    .assetNo("A002")
                    .name("打印机")
                    .status("IN_USE")
                    .build();

            when(assetRepository.listByIds(assetIds)).thenReturn(List.of(asset1, asset2));
            when(inventoryRepository.save(any(AssetInventory.class))).thenAnswer(invocation -> {
                AssetInventory inventory = invocation.getArgument(0);
                inventory.setId(TEST_INVENTORY_ID);
                return true;
            });
            when(detailMapper.insert(any(AssetInventoryDetail.class))).thenReturn(1);
            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenAnswer(invocation -> {
                        AssetInventory inv = createTestInventory(TEST_INVENTORY_ID);
                        inv.setInventoryType(AssetInventory.TYPE_PARTIAL);
                        inv.setTotalCount(2);
                        return inv;
                    });
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID)).thenReturn(Collections.emptyList());

            // When
            AssetInventoryDTO result = assetInventoryAppService.createInventory(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getInventoryType()).isEqualTo(AssetInventory.TYPE_PARTIAL);
        }

        @Test
        @DisplayName("抽盘时未指定资产ID应该抛出异常")
        void createInventory_shouldFail_whenPartialWithoutAssetIds() {
            // Given
            CreateAssetInventoryCommand command = new CreateAssetInventoryCommand();
            command.setInventoryDate(LocalDate.now());
            command.setInventoryType(AssetInventory.TYPE_PARTIAL);
            command.setAssetIds(null); // 未指定资产ID

            // When & Then
            assertThrows(BusinessException.class, () -> assetInventoryAppService.createInventory(command));
        }

        @Test
        @DisplayName("没有资产时应该抛出异常")
        void createInventory_shouldFail_whenNoAssets() {
            // Given
            CreateAssetInventoryCommand command = new CreateAssetInventoryCommand();
            command.setInventoryDate(LocalDate.now());
            command.setInventoryType(AssetInventory.TYPE_FULL);

            when(assetRepository.list()).thenReturn(Collections.emptyList());

            // When & Then
            assertThrows(BusinessException.class, () -> assetInventoryAppService.createInventory(command));
        }
    }

    @Nested
    @DisplayName("更新盘点明细测试")
    class UpdateDetailTests {

        @Test
        @DisplayName("应该成功更新盘点明细")
        void updateInventoryDetail_shouldSuccess() {
            // Given
            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(TEST_DETAIL_ID)
                    .inventoryId(TEST_INVENTORY_ID)
                    .assetId(TEST_ASSET_ID)
                    .expectedStatus("IN_USE")
                    .expectedLocation("主仓库")
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();

            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .status(AssetInventory.STATUS_IN_PROGRESS)
                    .build();

            when(detailMapper.selectById(TEST_DETAIL_ID)).thenReturn(detail);
            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString())).thenReturn(inventory);
            when(detailMapper.updateById(any(AssetInventoryDetail.class))).thenReturn(1);

            // When
            assetInventoryAppService.updateInventoryDetail(
                    TEST_DETAIL_ID, "IN_USE", "主仓库", 1L, null);

            // Then
            assertThat(detail.getActualStatus()).isEqualTo("IN_USE");
            assertThat(detail.getActualLocation()).isEqualTo("主仓库");
            verify(detailMapper).updateById(detail);
        }

        @Test
        @DisplayName("盘点已完成时不能修改明细")
        void updateInventoryDetail_shouldFail_whenInventoryCompleted() {
            // Given
            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(TEST_DETAIL_ID)
                    .inventoryId(TEST_INVENTORY_ID)
                    .build();

            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .status(AssetInventory.STATUS_COMPLETED) // 已完成
                    .build();

            when(detailMapper.selectById(TEST_DETAIL_ID)).thenReturn(detail);
            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString())).thenReturn(inventory);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> assetInventoryAppService.updateInventoryDetail(
                            TEST_DETAIL_ID, "IN_USE", "主仓库", 1L, null));
        }

        @Test
        @DisplayName("应该正确检测状态不符")
        void updateInventoryDetail_shouldDetectStatusDiscrepancy() {
            // Given
            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(TEST_DETAIL_ID)
                    .inventoryId(TEST_INVENTORY_ID)
                    .expectedStatus("IN_USE")
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();

            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .status(AssetInventory.STATUS_IN_PROGRESS)
                    .build();

            when(detailMapper.selectById(TEST_DETAIL_ID)).thenReturn(detail);
            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString())).thenReturn(inventory);
            when(detailMapper.updateById(any(AssetInventoryDetail.class))).thenReturn(1);

            // When
            assetInventoryAppService.updateInventoryDetail(
                    TEST_DETAIL_ID, "DAMAGED", "主仓库", 1L, null);

            // Then
            assertThat(detail.getDiscrepancyType()).isEqualTo(AssetInventoryDetail.DISCREPANCY_STATUS);
        }

        @Test
        @DisplayName("应该正确检测位置不符")
        void updateInventoryDetail_shouldDetectLocationDiscrepancy() {
            // Given
            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(TEST_DETAIL_ID)
                    .inventoryId(TEST_INVENTORY_ID)
                    .expectedStatus("IN_USE")
                    .expectedLocation("主仓库")
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();

            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .status(AssetInventory.STATUS_IN_PROGRESS)
                    .build();

            when(detailMapper.selectById(TEST_DETAIL_ID)).thenReturn(detail);
            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString())).thenReturn(inventory);
            when(detailMapper.updateById(any(AssetInventoryDetail.class))).thenReturn(1);

            // When
            assetInventoryAppService.updateInventoryDetail(
                    TEST_DETAIL_ID, "IN_USE", "分仓库", 1L, null);

            // Then
            assertThat(detail.getDiscrepancyType()).isEqualTo(AssetInventoryDetail.DISCREPANCY_LOCATION);
        }
    }

    @Nested
    @DisplayName("完成盘点测试")
    class CompleteInventoryTests {

        @Test
        @DisplayName("创建人应该成功完成盘点")
        void completeInventory_shouldSuccess_forCreator() {
            // Given
            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .inventoryNo("INV20240101001")
                    .status(AssetInventory.STATUS_IN_PROGRESS)
                    .createdBy(TEST_USER_ID) // 创建人是当前用户
                    .totalCount(10)
                    .build();

            AssetInventoryDetail detail1 = AssetInventoryDetail.builder()
                    .id(1L)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();

            AssetInventoryDetail detail2 = AssetInventoryDetail.builder()
                    .id(2L)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();

            when(inventoryRepository.getByIdOrThrow(TEST_INVENTORY_ID, "盘点不存在"))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID))
                    .thenReturn(List.of(detail1, detail2));
            when(inventoryRepository.updateById(any(AssetInventory.class))).thenReturn(true);

            // When
            AssetInventoryDTO result = assetInventoryAppService.completeInventory(TEST_INVENTORY_ID);

            // Then
            assertThat(inventory.getStatus()).isEqualTo(AssetInventory.STATUS_COMPLETED);
            assertThat(inventory.getActualCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("管理员可以完成他人的盘点")
        void completeInventory_shouldSuccess_forAdmin() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasRole(anyString())).thenReturn(true);

            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .status(AssetInventory.STATUS_IN_PROGRESS)
                    .createdBy(ADMIN_USER_ID) // 创建人是其他用户
                    .totalCount(1)
                    .build();

            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(1L)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();

            when(inventoryRepository.getByIdOrThrow(TEST_INVENTORY_ID, "盘点不存在"))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID))
                    .thenReturn(List.of(detail));
            when(inventoryRepository.updateById(any(AssetInventory.class))).thenReturn(true);

            // When
            assetInventoryAppService.completeInventory(TEST_INVENTORY_ID);

            // Then
            assertThat(inventory.getStatus()).isEqualTo(AssetInventory.STATUS_COMPLETED);
        }

        @Test
        @DisplayName("非管理员非创建人不能完成盘点")
        void completeInventory_shouldFail_forNonCreatorNonAdmin() {
            // Given
            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .status(AssetInventory.STATUS_IN_PROGRESS)
                    .createdBy(ADMIN_USER_ID) // 创建人是其他用户
                    .build();

            when(inventoryRepository.getByIdOrThrow(TEST_INVENTORY_ID, "盘点不存在"))
                    .thenReturn(inventory);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> assetInventoryAppService.completeInventory(TEST_INVENTORY_ID));
        }

        @Test
        @DisplayName("盘点已完成后不能重复完成")
        void completeInventory_shouldFail_whenAlreadyCompleted() {
            // Given
            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .status(AssetInventory.STATUS_COMPLETED) // 已完成
                    .createdBy(TEST_USER_ID)
                    .build();

            when(inventoryRepository.getByIdOrThrow(TEST_INVENTORY_ID, "盘点不存在"))
                    .thenReturn(inventory);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> assetInventoryAppService.completeInventory(TEST_INVENTORY_ID));
        }

        @Test
        @DisplayName("应该正确统计盘盈盘亏")
        void completeInventory_shouldCountDiscrepancies() {
            // Given
            AssetInventory inventory = AssetInventory.builder()
                    .id(TEST_INVENTORY_ID)
                    .status(AssetInventory.STATUS_IN_PROGRESS)
                    .createdBy(TEST_USER_ID)
                    .totalCount(5)
                    .build();

            AssetInventoryDetail detail1 = AssetInventoryDetail.builder()
                    .id(1L)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();

            AssetInventoryDetail detail2 = AssetInventoryDetail.builder()
                    .id(2L)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_SURPLUS)
                    .build();

            AssetInventoryDetail detail3 = AssetInventoryDetail.builder()
                    .id(3L)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_SHORTAGE)
                    .build();

            when(inventoryRepository.getByIdOrThrow(TEST_INVENTORY_ID, "盘点不存在"))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID))
                    .thenReturn(List.of(detail1, detail2, detail3));
            when(inventoryRepository.updateById(any(AssetInventory.class))).thenReturn(true);

            // When
            assetInventoryAppService.completeInventory(TEST_INVENTORY_ID);

            // Then
            assertThat(inventory.getSurplusCount()).isEqualTo(1);
            assertThat(inventory.getShortageCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询盘点测试")
    class QueryInventoryTests {

        @Test
        @DisplayName("应该成功获取盘点详情")
        void getInventoryById_shouldSuccess() {
            // Given
            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);

            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(TEST_DETAIL_ID)
                    .inventoryId(TEST_INVENTORY_ID)
                    .assetId(TEST_ASSET_ID)
                    .build();

            Asset asset = Asset.builder()
                    .id(TEST_ASSET_ID)
                    .assetNo("A001")
                    .name("电脑")
                    .build();

            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID))
                    .thenReturn(List.of(detail));
            when(assetRepository.listByIds(anyList())).thenReturn(List.of(asset));

            // When
            AssetInventoryDTO result = assetInventoryAppService.getInventoryById(TEST_INVENTORY_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_INVENTORY_ID);
            assertThat(result.getDetails()).hasSize(1);
        }

        @Test
        @DisplayName("应该成功分页查询盘点列表")
        void listInventories_shouldSuccess() {
            // Given
            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);

            Page<AssetInventory> page = new Page<>(1, 10);
            page.setRecords(List.of(inventory));
            page.setTotal(1L);

            when(inventoryMapper.selectPage(any(Page.class), any())).thenReturn(page);

            // When
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);
            PageResult<AssetInventoryDTO> result = assetInventoryAppService.listInventories(query, null);

            // Then
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        @DisplayName("应该按状态过滤盘点")
        void listInventories_shouldFilterByStatus() {
            // Given
            when(inventoryMapper.selectPage(any(Page.class), any()))
                    .thenReturn(new Page<>(1, 10));

            // When
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);
            assetInventoryAppService.listInventories(query, AssetInventory.STATUS_IN_PROGRESS);

            // Then
            verify(inventoryMapper).selectPage(any(Page.class), any());
        }

        @Test
        @DisplayName("应该成功查询进行中的盘点")
        void getInProgressInventories_shouldSuccess() {
            // Given
            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);
            inventory.setStatus(AssetInventory.STATUS_IN_PROGRESS);

            when(inventoryMapper.selectInProgress()).thenReturn(List.of(inventory));

            // When
            List<AssetInventoryDTO> result = assetInventoryAppService.getInProgressInventories();

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("类型和状态映射测试")
    class TypeAndStatusMappingTests {

        @Test
        @DisplayName("应该正确映射全盘类型")
        void type_shouldMapFull() {
            // Given
            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);
            inventory.setInventoryType(AssetInventory.TYPE_FULL);

            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID)).thenReturn(Collections.emptyList());

            // When
            AssetInventoryDTO result = assetInventoryAppService.getInventoryById(TEST_INVENTORY_ID);

            // Then
            assertThat(result.getInventoryTypeName()).isEqualTo("全盘");
        }

        @Test
        @DisplayName("应该正确映射抽盘类型")
        void type_shouldMapPartial() {
            // Given
            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);
            inventory.setInventoryType(AssetInventory.TYPE_PARTIAL);

            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID)).thenReturn(Collections.emptyList());

            // When
            AssetInventoryDTO result = assetInventoryAppService.getInventoryById(TEST_INVENTORY_ID);

            // Then
            assertThat(result.getInventoryTypeName()).isEqualTo("抽盘");
        }

        @Test
        @DisplayName("应该正确映射进行中状态")
        void status_shouldMapInProgress() {
            // Given
            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);
            inventory.setStatus(AssetInventory.STATUS_IN_PROGRESS);

            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID)).thenReturn(Collections.emptyList());

            // When
            AssetInventoryDTO result = assetInventoryAppService.getInventoryById(TEST_INVENTORY_ID);

            // Then
            assertThat(result.getStatusName()).isEqualTo("进行中");
        }

        @Test
        @DisplayName("应该正确映射已完成状态")
        void status_shouldMapCompleted() {
            // Given
            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);
            inventory.setStatus(AssetInventory.STATUS_COMPLETED);

            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID)).thenReturn(Collections.emptyList());

            // When
            AssetInventoryDTO result = assetInventoryAppService.getInventoryById(TEST_INVENTORY_ID);

            // Then
            assertThat(result.getStatusName()).isEqualTo("已完成");
        }

        @Test
        @DisplayName("应该正确映射正常差异类型")
        void discrepancyType_shouldMapNormal() {
            // Given
            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(TEST_DETAIL_ID)
                    .inventoryId(TEST_INVENTORY_ID)
                    .assetId(TEST_ASSET_ID)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();

            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);

            Asset asset = Asset.builder()
                    .id(TEST_ASSET_ID)
                    .assetNo("A001")
                    .name("电脑")
                    .build();

            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID))
                    .thenReturn(List.of(detail));
            when(assetRepository.listByIds(anyList())).thenReturn(List.of(asset));

            // When
            AssetInventoryDTO result = assetInventoryAppService.getInventoryById(TEST_INVENTORY_ID);

            // Then
            assertThat(result.getDetails().get(0).getDiscrepancyTypeName()).isEqualTo("正常");
        }

        @Test
        @DisplayName("应该正确映射盘盈差异类型")
        void discrepancyType_shouldMapSurplus() {
            // Given
            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(TEST_DETAIL_ID)
                    .inventoryId(TEST_INVENTORY_ID)
                    .assetId(TEST_ASSET_ID)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_SURPLUS)
                    .build();

            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);

            Asset asset = Asset.builder()
                    .id(TEST_ASSET_ID)
                    .assetNo("A001")
                    .name("电脑")
                    .build();

            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID))
                    .thenReturn(List.of(detail));
            when(assetRepository.listByIds(anyList())).thenReturn(List.of(asset));

            // When
            AssetInventoryDTO result = assetInventoryAppService.getInventoryById(TEST_INVENTORY_ID);

            // Then
            assertThat(result.getDetails().get(0).getDiscrepancyTypeName()).isEqualTo("盘盈");
        }

        @Test
        @DisplayName("应该正确映射盘亏差异类型")
        void discrepancyType_shouldMapShortage() {
            // Given
            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .id(TEST_DETAIL_ID)
                    .inventoryId(TEST_INVENTORY_ID)
                    .assetId(TEST_ASSET_ID)
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_SHORTAGE)
                    .build();

            AssetInventory inventory = createTestInventory(TEST_INVENTORY_ID);

            Asset asset = Asset.builder()
                    .id(TEST_ASSET_ID)
                    .assetNo("A001")
                    .name("电脑")
                    .build();

            when(inventoryRepository.getByIdOrThrow(eq(TEST_INVENTORY_ID), anyString()))
                    .thenReturn(inventory);
            when(detailMapper.selectByInventoryId(TEST_INVENTORY_ID))
                    .thenReturn(List.of(detail));
            when(assetRepository.listByIds(anyList())).thenReturn(List.of(asset));

            // When
            AssetInventoryDTO result = assetInventoryAppService.getInventoryById(TEST_INVENTORY_ID);

            // Then
            assertThat(result.getDetails().get(0).getDiscrepancyTypeName()).isEqualTo("盘亏");
        }
    }

    // 辅助方法
    private AssetInventory createTestInventory(Long id) {
        return AssetInventory.builder()
                .id(id)
                .inventoryNo("INV20240101001")
                .inventoryDate(LocalDate.now())
                .inventoryType(AssetInventory.TYPE_FULL)
                .departmentId(1L)
                .location("主仓库")
                .status(AssetInventory.STATUS_IN_PROGRESS)
                .totalCount(10)
                .actualCount(0)
                .surplusCount(0)
                .shortageCount(0)
                .createdBy(TEST_USER_ID)
                .build();
    }
}
