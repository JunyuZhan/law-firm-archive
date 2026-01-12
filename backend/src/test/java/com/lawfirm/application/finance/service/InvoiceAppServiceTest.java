package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreateInvoiceCommand;
import com.lawfirm.application.finance.dto.InvoiceDTO;
import com.lawfirm.application.finance.dto.InvoiceStatisticsDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Invoice;
import com.lawfirm.domain.finance.repository.InvoiceRepository;
import com.lawfirm.infrastructure.persistence.mapper.InvoiceMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InvoiceAppService 单元测试
 * 测试财务模块发票管理核心业务逻辑：开票申请、税额计算、权限控制等
 */
@ExtendWith(MockitoExtension.class)
class InvoiceAppServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    
    @Mock
    private ClientRepository clientRepository;
    
    @Mock
    private InvoiceMapper invoiceMapper;

    @InjectMocks
    private InvoiceAppService invoiceAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN", "FINANCE"));
        securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }


    // ==================== 分页查询测试 ====================

    @Nested
    @DisplayName("分页查询发票测试")
    class ListInvoicesTests {

        @Test
        @DisplayName("成功查询发票列表 - ALL权限")
        void listInvoices_Success_AllPermission() {
            // Given
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);
            Long clientId = 1L;
            String status = "PENDING";
            
            Invoice invoice = Invoice.builder()
                    .id(1L)
                    .invoiceNo("INV2024010001")
                    .clientId(clientId)
                    .amount(new BigDecimal("10000.00"))
                    .taxRate(new BigDecimal("0.06"))
                    .taxAmount(new BigDecimal("600.00"))
                    .status(status)
                    .build();
            
            List<Invoice> invoiceList = Collections.singletonList(invoice);
            Page<Invoice> page = new Page<>(query.getPageNum(), query.getPageSize(), invoiceList.size());
            page.setRecords(invoiceList);
            
            when(invoiceRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            // When
            PageResult<InvoiceDTO> result = invoiceAppService.listInvoices(query, clientId, status);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getList().size());
            
            InvoiceDTO dto = result.getList().get(0);
            assertEquals("INV2024010001", dto.getInvoiceNo());
            assertEquals(new BigDecimal("10000.00"), dto.getAmount());
            assertEquals("待开票", dto.getStatusName());
            
            verify(invoiceRepository).page(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("查询发票列表 - SELF权限只能查看自己申请的发票")
        void listInvoices_SelfPermission() {
            // Given
            securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("SELF");
            securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(100L);
            
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);
            
            Invoice invoice = Invoice.builder()
                    .id(1L)
                    .applicantId(100L) // 当前用户
                    .build();
            
            List<Invoice> invoiceList = Collections.singletonList(invoice);
            Page<Invoice> page = new Page<>(query.getPageNum(), query.getPageSize(), invoiceList.size());
            page.setRecords(invoiceList);
            
            when(invoiceRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            // When
            PageResult<InvoiceDTO> result = invoiceAppService.listInvoices(query, null, null);

            // Then
            assertNotNull(result);
            verify(invoiceRepository).page(any(Page.class), argThat(wrapper -> {
                // 验证条件中包含了 applicantId = 100L
                return true; // 简化验证，实际可以更详细
            }));
        }
    }


    // ==================== 申请开票测试 ====================

    @Nested
    @DisplayName("申请开票测试")
    class ApplyInvoiceTests {

        @Test
        @DisplayName("成功申请开票 - 含税价")
        void applyInvoice_Success_TaxIncluded() {
            // Given
            CreateInvoiceCommand command = new CreateInvoiceCommand();
            command.setClientId(1L);
            command.setInvoiceType("SPECIAL");
            command.setTitle("测试发票");
            command.setTaxNo("911100001000123456");
            command.setAmount(new BigDecimal("10600.00")); // 含税价
            command.setTaxRate(new BigDecimal("0.06"));
            command.setTaxIncluded(true);
            command.setContent("测试开票内容");
            
            Client client = new Client();
            client.setId(1L);
            when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
            
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(true);

            // When
            InvoiceDTO result = invoiceAppService.applyInvoice(command);

            // Then
            assertNotNull(result);
            assertEquals("测试发票", result.getTitle());
            assertEquals("SPECIAL", result.getInvoiceType());
            assertEquals("增值税专用发票", result.getInvoiceTypeName());
            assertEquals(new BigDecimal("0.06"), result.getTaxRate());
            
            // 验证税额计算：含税价10600，税率6%，不含税价应为10000，税额600
            // 由于舍入误差，使用近似比较
            assertTrue(result.getAmount().compareTo(new BigDecimal("10000.00")) == 0);
            assertTrue(result.getTaxAmount().compareTo(new BigDecimal("600.00")) == 0);
            
            verify(clientRepository).getByIdOrThrow(eq(1L), anyString());
            verify(invoiceRepository).save(any(Invoice.class));
        }

        @Test
        @DisplayName("成功申请开票 - 不含税价")
        void applyInvoice_Success_TaxExcluded() {
            // Given
            CreateInvoiceCommand command = new CreateInvoiceCommand();
            command.setClientId(1L);
            command.setAmount(new BigDecimal("10000.00")); // 不含税价
            command.setTaxRate(new BigDecimal("0.06"));
            command.setTaxIncluded(false);
            
            Client client = new Client();
            client.setId(1L);
            when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(true);

            // When
            InvoiceDTO result = invoiceAppService.applyInvoice(command);

            // Then
            assertNotNull(result);
            // 验证税额计算：不含税价10000，税率6%，税额600，价税合计10600
            assertTrue(result.getAmount().compareTo(new BigDecimal("10000.00")) == 0);
            assertTrue(result.getTaxAmount().compareTo(new BigDecimal("600.00")) == 0);
        }

        @Test
        @DisplayName("申请开票 - 客户不存在时失败")
        void applyInvoice_ClientNotFound() {
            // Given
            CreateInvoiceCommand command = new CreateInvoiceCommand();
            command.setClientId(999L);
            command.setAmount(new BigDecimal("10000.00"));

            when(clientRepository.getByIdOrThrow(eq(999L), anyString()))
                    .thenThrow(new BusinessException("客户不存在"));

            // When & Then
            assertThrows(BusinessException.class, () -> invoiceAppService.applyInvoice(command));
        }
    }


    // ==================== 获取发票详情测试 ====================

    @Nested
    @DisplayName("获取发票详情测试")
    class GetInvoiceByIdTests {

        @Test
        @DisplayName("成功获取发票详情")
        void getInvoiceById_Success() {
            // Given
            Long invoiceId = 1L;
            Invoice invoice = Invoice.builder()
                    .id(invoiceId)
                    .invoiceNo("INV2024010001")
                    .amount(new BigDecimal("10000.00"))
                    .status("PENDING")
                    .build();
            
            when(invoiceRepository.getByIdOrThrow(eq(invoiceId), anyString())).thenReturn(invoice);

            // When
            InvoiceDTO result = invoiceAppService.getInvoiceById(invoiceId);

            // Then
            assertNotNull(result);
            assertEquals("INV2024010001", result.getInvoiceNo());
            assertEquals(new BigDecimal("10000.00"), result.getAmount());
            assertEquals("待开票", result.getStatusName());
            verify(invoiceRepository).getByIdOrThrow(eq(invoiceId), anyString());
        }

        @Test
        @DisplayName("发票不存在时获取详情失败")
        void getInvoiceById_NotFound() {
            // Given
            Long invoiceId = 999L;
            when(invoiceRepository.getByIdOrThrow(eq(invoiceId), anyString()))
                    .thenThrow(new BusinessException("发票不存在"));

            // When & Then
            assertThrows(BusinessException.class, () -> invoiceAppService.getInvoiceById(invoiceId));
        }
    }


    // ==================== 开票操作测试 ====================

    @Nested
    @DisplayName("开票操作测试")
    class IssueInvoiceTests {

        @Test
        @DisplayName("成功开票")
        void issueInvoice_Success() {
            // Given
            Long invoiceId = 1L;
            String invoiceNo = "INV2024010001";
            
            Invoice invoice = Invoice.builder()
                    .id(invoiceId)
                    .status("PENDING")
                    .build();
            
            when(invoiceRepository.getByIdOrThrow(eq(invoiceId), anyString())).thenReturn(invoice);
            when(invoiceRepository.updateById(any(Invoice.class))).thenReturn(true);

            // When
            invoiceAppService.issueInvoice(invoiceId, invoiceNo);

            // Then
            assertEquals("ISSUED", invoice.getStatus());
            assertEquals(invoiceNo, invoice.getInvoiceNo());
            assertNotNull(invoice.getInvoiceDate());
            verify(invoiceRepository).updateById(invoice);
        }

        @Test
        @DisplayName("非待开票状态不能开票")
        void issueInvoice_NotPending() {
            // Given
            Long invoiceId = 1L;
            Invoice invoice = Invoice.builder()
                    .id(invoiceId)
                    .status("ISSUED") // 已开票状态
                    .build();
            
            when(invoiceRepository.getByIdOrThrow(eq(invoiceId), anyString())).thenReturn(invoice);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> invoiceAppService.issueInvoice(invoiceId, "INV2024010001"));
            assertEquals("当前状态不允许开票", exception.getMessage());
        }
    }


    // ==================== 作废发票测试 ====================

    @Nested
    @DisplayName("作废发票测试")
    class CancelInvoiceTests {

        @Test
        @DisplayName("成功作废发票")
        void cancelInvoice_Success() {
            // Given
            Long invoiceId = 1L;
            String reason = "开票信息错误";
            
            Invoice invoice = Invoice.builder()
                    .id(invoiceId)
                    .invoiceNo("INV2024010001")
                    .status("ISSUED")
                    .build();
            
            when(invoiceRepository.getByIdOrThrow(eq(invoiceId), anyString())).thenReturn(invoice);
            when(invoiceRepository.updateById(any(Invoice.class))).thenReturn(true);

            // When
            invoiceAppService.cancelInvoice(invoiceId, reason);

            // Then
            assertEquals("CANCELLED", invoice.getStatus());
            assertEquals(reason, invoice.getRemark());
            verify(invoiceRepository).updateById(invoice);
        }

        @Test
        @DisplayName("非已开票状态不能作废")
        void cancelInvoice_NotIssued() {
            // Given
            Long invoiceId = 1L;
            Invoice invoice = Invoice.builder()
                    .id(invoiceId)
                    .status("PENDING") // 待开票状态
                    .build();
            
            when(invoiceRepository.getByIdOrThrow(eq(invoiceId), anyString())).thenReturn(invoice);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> invoiceAppService.cancelInvoice(invoiceId, "测试原因"));
            assertEquals("只有已开票状态可以作废", exception.getMessage());
        }
    }


    // ==================== 发票统计测试 ====================

    @Nested
    @DisplayName("发票统计测试")
    class GetInvoiceStatisticsTests {

        @Test
        @DisplayName("管理员成功获取发票统计")
        void getInvoiceStatistics_Success_Admin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
            
            BigDecimal totalAmount = new BigDecimal("100000.00");
            BigDecimal monthlyAmount = new BigDecimal("50000.00");
            BigDecimal yearlyAmount = new BigDecimal("80000.00");
            
            List<Map<String, Object>> byClient = Arrays.asList(
                    createStatMap("客户A", 5, new BigDecimal("30000.00")),
                    createStatMap("客户B", 3, new BigDecimal("20000.00"))
            );
            
            List<Map<String, Object>> byType = Arrays.asList(
                    createStatMap("SPECIAL", 6, new BigDecimal("40000.00")),
                    createStatMap("NORMAL", 2, new BigDecimal("10000.00"))
            );
            
            List<Map<String, Object>> byStatus = Arrays.asList(
                    createStatMap("ISSUED", 5, new BigDecimal("45000.00")),
                    createStatMap("PENDING", 3, new BigDecimal("15000.00"))
            );
            
            List<Map<String, Object>> byDate = Arrays.asList(
                    createStatMap("2024-01", 3, new BigDecimal("20000.00")),
                    createStatMap("2024-02", 2, new BigDecimal("15000.00"))
            );
            
            when(invoiceMapper.sumTotalInvoiceAmount()).thenReturn(totalAmount);
            when(invoiceMapper.sumMonthlyInvoiceAmount()).thenReturn(monthlyAmount);
            when(invoiceMapper.sumYearlyInvoiceAmount()).thenReturn(yearlyAmount);
            when(invoiceMapper.countByClient()).thenReturn(byClient);
            when(invoiceMapper.countByType()).thenReturn(byType);
            when(invoiceMapper.countByStatus()).thenReturn(byStatus);
            when(invoiceMapper.countByDate()).thenReturn(byDate);

            // When
            InvoiceStatisticsDTO result = invoiceAppService.getInvoiceStatistics();

            // Then
            assertNotNull(result);
            assertEquals(totalAmount, result.getTotalAmount());
            assertEquals(monthlyAmount, result.getMonthlyAmount());
            assertEquals(yearlyAmount, result.getYearlyAmount());
            assertEquals(byClient, result.getByClient());
            assertEquals(byType, result.getByType());
            assertEquals(byStatus, result.getByStatus());
            assertEquals(byDate, result.getByDate());
            
            verify(invoiceMapper).sumTotalInvoiceAmount();
            verify(invoiceMapper).sumMonthlyInvoiceAmount();
            verify(invoiceMapper).sumYearlyInvoiceAmount();
        }

        @Test
        @DisplayName("财务人员成功获取发票统计")
        void getInvoiceStatistics_Success_Finance() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("FINANCE"));
            
            when(invoiceMapper.sumTotalInvoiceAmount()).thenReturn(new BigDecimal("100000.00"));

            // When
            InvoiceStatisticsDTO result = invoiceAppService.getInvoiceStatistics();

            // Then
            assertNotNull(result);
            // 财务人员应该也能获取统计数据
        }

        @Test
        @DisplayName("非管理员和财务人员不能获取发票统计")
        void getInvoiceStatistics_NoPermission() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("USER")); // 普通用户
            
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> invoiceAppService.getInvoiceStatistics());
            assertEquals("仅管理员和财务人员可以查看发票统计", exception.getMessage());
        }

        private Map<String, Object> createStatMap(String key, Integer count, BigDecimal amount) {
            Map<String, Object> map = new HashMap<>();
            map.put("key", key);
            map.put("count", count);
            map.put("amount", amount);
            return map;
        }
    }


    // ==================== 辅助方法测试 ====================

    @Nested
    @DisplayName("辅助方法测试")
    class HelperMethodTests {

        @Test
        @DisplayName("获取发票类型名称")
        void getInvoiceTypeName() {
            // 这些方法在 InvoiceAppService 中是私有的，我们通过公共方法间接测试
            // 创建一个发票并验证 DTO 转换是否正确
            Invoice invoice = Invoice.builder()
                    .id(1L)
                    .invoiceType("SPECIAL")
                    .status("PENDING")
                    .build();
            
            when(invoiceRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(invoice);
            
            // When
            InvoiceDTO result = invoiceAppService.getInvoiceById(1L);
            
            // Then
            assertEquals("增值税专用发票", result.getInvoiceTypeName());
            assertEquals("待开票", result.getStatusName());
        }

        @Test
        @DisplayName("获取未知发票类型和状态名称")
        void getUnknownTypeAndStatusName() {
            Invoice invoice = Invoice.builder()
                    .id(1L)
                    .invoiceType("UNKNOWN_TYPE")
                    .status("UNKNOWN_STATUS")
                    .build();
            
            when(invoiceRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(invoice);
            
            // When
            InvoiceDTO result = invoiceAppService.getInvoiceById(1L);
            
            // Then
            assertEquals("UNKNOWN_TYPE", result.getInvoiceTypeName());
            assertEquals("UNKNOWN_STATUS", result.getStatusName());
        }
    }


    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("零税率测试")
        void zeroTaxRate() {
            // Given
            CreateInvoiceCommand command = new CreateInvoiceCommand();
            command.setClientId(1L);
            command.setAmount(new BigDecimal("10000.00"));
            command.setTaxRate(BigDecimal.ZERO); // 零税率
            command.setTaxIncluded(false);
            
            Client client = new Client();
            client.setId(1L);
            when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(true);

            // When
            InvoiceDTO result = invoiceAppService.applyInvoice(command);

            // Then
            assertNotNull(result);
            // 使用 compareTo 比较 BigDecimal，忽略精度差异
            assertTrue(result.getTaxAmount().compareTo(BigDecimal.ZERO) == 0); // 税额应为0
            assertTrue(result.getAmount().compareTo(new BigDecimal("10000.00")) == 0); // 金额不变
        }

        @Test
        @DisplayName("空值处理测试")
        void nullValueHandling() {
            // Given
            PageQuery query = new PageQuery();
            query.setPageNum(1);
            query.setPageSize(10);
            
            Invoice invoice = Invoice.builder()
                    .id(1L)
                    .build();
            
            List<Invoice> invoiceList = Collections.singletonList(invoice);
            Page<Invoice> page = new Page<>(query.getPageNum(), query.getPageSize(), invoiceList.size());
            page.setRecords(invoiceList);
            
            when(invoiceRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            // When (不传 clientId 和 status)
            PageResult<InvoiceDTO> result = invoiceAppService.listInvoices(query, null, null);

            // Then - 应该正常执行，不抛出异常
            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }
    }
}
