package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreateSupplierCommand;
import com.lawfirm.application.admin.dto.SupplierDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.admin.entity.Supplier;
import com.lawfirm.domain.admin.repository.PurchaseRequestRepository;
import com.lawfirm.domain.admin.repository.SupplierRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SupplierAppService 单元测试
 * 测试供应商管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierAppService 供应商服务测试")
class SupplierAppServiceTest {

    private static final Long TEST_SUPPLIER_ID = 100L;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @InjectMocks
    private SupplierAppService supplierAppService;

    @Nested
    @DisplayName("查询供应商测试")
    class QuerySupplierTests {

        @Test
        @DisplayName("应该成功分页查询供应商")
        void listSuppliers_shouldSuccess() {
            // Given
            Supplier supplier = Supplier.builder()
                    .id(TEST_SUPPLIER_ID)
                    .supplierNo("SUP2024001")
                    .name("测试供应商")
                    .supplierType("GOODS")
                    .status("ACTIVE")
                    .rating("A")
                    .build();

            Page<Supplier> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(supplier));
            page.setTotal(1L);

            when(supplierRepository.findPage(any(Page.class), any(), any(), any(), any()))
                    .thenReturn(page);

            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<SupplierDTO> result = supplierAppService.listSuppliers(query, null, null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1L);
        }

        @Test
        @DisplayName("应该成功获取供应商详情")
        void getSupplierById_shouldSuccess() {
            // Given
            Supplier supplier = Supplier.builder()
                    .id(TEST_SUPPLIER_ID)
                    .supplierNo("SUP2024001")
                    .name("测试供应商")
                    .supplierType("GOODS")
                    .status("ACTIVE")
                    .build();

            when(supplierRepository.getById(TEST_SUPPLIER_ID)).thenReturn(supplier);

            // When
            SupplierDTO result = supplierAppService.getSupplierById(TEST_SUPPLIER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSupplierNo()).isEqualTo("SUP2024001");
            assertThat(result.getName()).isEqualTo("测试供应商");
        }

        @Test
        @DisplayName("供应商不存在应该失败")
        void getSupplierById_shouldFail_whenNotFound() {
            // Given
            when(supplierRepository.getById(TEST_SUPPLIER_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> supplierAppService.getSupplierById(TEST_SUPPLIER_ID));
            assertThat(exception.getMessage()).contains("供应商不存在");
        }
    }

    @Nested
    @DisplayName("创建供应商测试")
    class CreateSupplierTests {

        @Test
        @DisplayName("应该成功创建供应商")
        void createSupplier_shouldSuccess() {
            // Given
            CreateSupplierCommand command = new CreateSupplierCommand();
            command.setName("新供应商");
            command.setSupplierType("GOODS");
            command.setContactPerson("张三");
            command.setContactPhone("13800138000");
            command.setContactEmail("test@example.com");
            command.setAddress("北京市朝阳区");
            command.setRating("A");

            when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
                Supplier supplier = invocation.getArgument(0);
                supplier.setId(TEST_SUPPLIER_ID);
                supplier.setSupplierNo("SUP2024001");
                return true;
            });

            // When
            SupplierDTO result = supplierAppService.createSupplier(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("新供应商");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            assertThat(result.getSupplierNo()).isNotNull();
            verify(supplierRepository).save(any(Supplier.class));
        }

        @Test
        @DisplayName("未提供评级应该使用默认值B")
        void createSupplier_shouldUseDefaultRating() {
            // Given
            CreateSupplierCommand command = new CreateSupplierCommand();
            command.setName("新供应商");
            command.setSupplierType("GOODS");
            // 不设置rating

            when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
                Supplier supplier = invocation.getArgument(0);
                supplier.setId(TEST_SUPPLIER_ID);
                supplier.setSupplierNo("SUP2024001");
                return true;
            });

            // When
            SupplierDTO result = supplierAppService.createSupplier(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo("B");
        }
    }

    @Nested
    @DisplayName("更新供应商测试")
    class UpdateSupplierTests {

        @Test
        @DisplayName("应该成功更新供应商")
        void updateSupplier_shouldSuccess() {
            // Given
            Supplier supplier = Supplier.builder()
                    .id(TEST_SUPPLIER_ID)
                    .supplierNo("SUP2024001")
                    .name("原名称")
                    .contactPhone("13800138000")
                    .status("ACTIVE")
                    .build();

            CreateSupplierCommand command = new CreateSupplierCommand();
            command.setName("新名称");
            command.setContactPhone("13900139000");

            when(supplierRepository.getById(TEST_SUPPLIER_ID)).thenReturn(supplier);
            when(supplierRepository.updateById(any(Supplier.class))).thenReturn(true);

            // When
            SupplierDTO result = supplierAppService.updateSupplier(TEST_SUPPLIER_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(supplier.getName()).isEqualTo("新名称");
            assertThat(supplier.getContactPhone()).isEqualTo("13900139000");
            verify(supplierRepository).updateById(supplier);
        }

        @Test
        @DisplayName("只更新非null字段")
        void updateSupplier_shouldOnlyUpdateNonNullFields() {
            // Given
            Supplier supplier = Supplier.builder()
                    .id(TEST_SUPPLIER_ID)
                    .name("原名称")
                    .contactPhone("13800138000")
                    .contactEmail("old@example.com")
                    .build();

            CreateSupplierCommand command = new CreateSupplierCommand();
            command.setName("新名称");
            // 不设置contactPhone和contactEmail

            when(supplierRepository.getById(TEST_SUPPLIER_ID)).thenReturn(supplier);
            when(supplierRepository.updateById(any(Supplier.class))).thenReturn(true);

            // When
            supplierAppService.updateSupplier(TEST_SUPPLIER_ID, command);

            // Then
            assertThat(supplier.getName()).isEqualTo("新名称");
            assertThat(supplier.getContactPhone()).isEqualTo("13800138000"); // 保持不变
            assertThat(supplier.getContactEmail()).isEqualTo("old@example.com"); // 保持不变
        }

        @Test
        @DisplayName("供应商不存在应该失败")
        void updateSupplier_shouldFail_whenNotFound() {
            // Given
            CreateSupplierCommand command = new CreateSupplierCommand();

            when(supplierRepository.getById(TEST_SUPPLIER_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> supplierAppService.updateSupplier(TEST_SUPPLIER_ID, command));
            assertThat(exception.getMessage()).contains("供应商不存在");
        }
    }

    @Nested
    @DisplayName("删除供应商测试")
    class DeleteSupplierTests {

        @Test
        @DisplayName("应该成功删除没有采购记录的供应商")
        void deleteSupplier_shouldSuccess() {
            // Given
            Supplier supplier = Supplier.builder()
                    .id(TEST_SUPPLIER_ID)
                    .supplierNo("SUP2024001")
                    .name("测试供应商")
                    .status("ACTIVE")
                    .build();

            when(supplierRepository.getById(TEST_SUPPLIER_ID)).thenReturn(supplier);
            when(purchaseRequestRepository.countBySupplierId(TEST_SUPPLIER_ID)).thenReturn(0L);
            when(supplierRepository.updateById(any(Supplier.class))).thenReturn(true);

            // When
            supplierAppService.deleteSupplier(TEST_SUPPLIER_ID);

            // Then
            assertThat(supplier.getStatus()).isEqualTo("INACTIVE");
            verify(supplierRepository).updateById(supplier);
        }

        @Test
        @DisplayName("有采购记录的供应商不能删除")
        void deleteSupplier_shouldFail_whenHasPurchaseRecords() {
            // Given
            Supplier supplier = Supplier.builder()
                    .id(TEST_SUPPLIER_ID)
                    .name("测试供应商")
                    .build();

            when(supplierRepository.getById(TEST_SUPPLIER_ID)).thenReturn(supplier);
            when(purchaseRequestRepository.countBySupplierId(TEST_SUPPLIER_ID)).thenReturn(5L);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> supplierAppService.deleteSupplier(TEST_SUPPLIER_ID));
            assertThat(exception.getMessage()).contains("采购记录");
        }
    }

    @Nested
    @DisplayName("供应商统计测试")
    class SupplierStatisticsTests {

        @Test
        @DisplayName("应该成功获取供应商统计")
        void getSupplierStatistics_shouldSuccess() {
            // Given
            when(supplierRepository.countByStatus()).thenReturn(List.of(
                    Map.of("status", "ACTIVE", "count", 80L),
                    Map.of("status", "INACTIVE", "count", 20L)
            ));
            when(supplierRepository.countByRating()).thenReturn(List.of(
                    Map.of("rating", "A", "count", 30L),
                    Map.of("rating", "B", "count", 40L),
                    Map.of("rating", "C", "count", 20L),
                    Map.of("rating", "D", "count", 10L)
            ));

            // When
            Map<String, Object> result = supplierAppService.getStatistics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("byStatus")).isNotNull();
            assertThat(result.get("byRating")).isNotNull();
        }
    }
}
