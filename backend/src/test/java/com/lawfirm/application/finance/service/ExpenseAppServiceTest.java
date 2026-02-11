package com.lawfirm.application.finance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.finance.command.AllocateCostCommand;
import com.lawfirm.application.finance.command.ApproveExpenseCommand;
import com.lawfirm.application.finance.command.CreateExpenseCommand;
import com.lawfirm.application.finance.command.SplitCostCommand;
import com.lawfirm.application.finance.dto.ExpenseDTO;
import com.lawfirm.application.finance.dto.ExpenseQueryDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.ExpenseStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.CostAllocation;
import com.lawfirm.domain.finance.entity.CostSplit;
import com.lawfirm.domain.finance.entity.Expense;
import com.lawfirm.domain.finance.repository.CostAllocationRepository;
import com.lawfirm.domain.finance.repository.CostSplitRepository;
import com.lawfirm.domain.finance.repository.ExpenseRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.CostAllocationMapper;
import com.lawfirm.infrastructure.persistence.mapper.ExpenseMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** ExpenseAppService 单元测试 测试财务模块费用报销核心业务逻辑：费用申请、审批、支付、成本归集与分摊等 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseAppServiceTest {

  @Mock private ExpenseRepository expenseRepository;

  @Mock private ExpenseMapper expenseMapper;

  @Mock private CostAllocationRepository costAllocationRepository;

  @Mock private CostAllocationMapper costAllocationMapper;

  @Mock private CostSplitRepository costSplitRepository;

  @Mock private MatterRepository matterRepository;

  @Mock private UserRepository userRepository;

  @Mock private ApprovalService approvalService;

  @Mock private ApproverService approverService;

  @Mock private ObjectMapper objectMapper;

  @Mock private MatterAppService matterAppService;

  @InjectMocks private ExpenseAppService expenseAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
    securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(100L);
    securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN", "FINANCE"));
    securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

    // 手动设置 matterAppService，因为它是通过 @Lazy setter 注入的
    expenseAppService.setMatterAppService(matterAppService);

    // 设置一些常用的 mock 返回值
    when(expenseRepository.getBaseMapper())
        .thenReturn(mock(com.lawfirm.infrastructure.persistence.mapper.ExpenseMapper.class));
    when(costAllocationRepository.getBaseMapper())
        .thenReturn(mock(com.lawfirm.infrastructure.persistence.mapper.CostAllocationMapper.class));
    when(costSplitRepository.getBaseMapper())
        .thenReturn(mock(com.lawfirm.infrastructure.persistence.mapper.CostSplitMapper.class));
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  // ==================== 分页查询测试 ====================

  @Nested
  @DisplayName("分页查询费用报销测试")
  class ListExpensesTests {

    @Test
    @DisplayName("成功查询费用报销列表 - ALL权限")
    void listExpenses_Success_AllPermission() {
      // Given
      ExpenseQueryDTO query = new ExpenseQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Expense expense =
          Expense.builder()
              .id(1L)
              .expenseNo("EXP2024010001")
              .amount(new BigDecimal("5000.00"))
              .status(ExpenseStatus.PENDING)
              .applicantId(1L)
              .build();

      List<Expense> expenses = Collections.singletonList(expense);

      when(matterAppService.getAccessibleMatterIds(eq("ALL"), eq(1L), eq(100L)))
          .thenReturn(null); // null表示所有项目都可访问
      when(expenseMapper.selectExpensePage(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
          .thenReturn(expenses);
      when(userRepository.listByIds(any())).thenReturn(Collections.emptyList());
      when(matterRepository.listByIds(any())).thenReturn(Collections.emptyList());

      // When
      PageResult<ExpenseDTO> result = expenseAppService.listExpenses(query);

      // Then
      assertNotNull(result);
      assertEquals(1, result.getTotal());
      assertEquals(1, result.getList().size());

      ExpenseDTO dto = result.getList().get(0);
      assertEquals("EXP2024010001", dto.getExpenseNo());
      assertEquals(new BigDecimal("5000.00"), dto.getAmount());
      assertEquals(ExpenseStatus.PENDING, dto.getStatus());

      verify(expenseMapper).selectExpensePage(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("查询费用报销列表 - SELF权限只能查看自己申请的")
    void listExpenses_SelfPermission() {
      // Given
      securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("SELF");

      ExpenseQueryDTO query = new ExpenseQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);
      query.setApplicantId(1L); // 当前用户

      Expense expense = Expense.builder().id(1L).applicantId(1L).build();

      List<Expense> expenses = Collections.singletonList(expense);

      // 当accessibleMatterIds返回空列表时，方法会提前返回空结果
      // 我们需要测试SELF权限逻辑，所以需要模拟返回null（表示所有项目可访问）或者有数据的列表
      when(matterAppService.getAccessibleMatterIds(eq("SELF"), eq(1L), eq(100L)))
          .thenReturn(null); // 改为null，让逻辑继续执行
      when(expenseMapper.selectExpensePage(any(), any(), eq(1L), any(), any(), any(), any(), anyInt(), anyInt()))
          .thenReturn(expenses);
      when(userRepository.listByIds(any())).thenReturn(Collections.emptyList());
      when(matterRepository.listByIds(any())).thenReturn(Collections.emptyList());

      // When
      PageResult<ExpenseDTO> result = expenseAppService.listExpenses(query);

      // Then
      assertNotNull(result);
      // 验证查询时传入了正确的applicantId
      verify(expenseMapper).selectExpensePage(any(), any(), eq(1L), any(), any(), any(), any(), anyInt(), anyInt());
    }
  }

  // ==================== 创建费用报销测试 ====================

  @Nested
  @DisplayName("创建费用报销测试")
  class CreateExpenseTests {

    @Test
    @DisplayName("成功创建费用报销申请")
    void createExpense_Success() throws Exception {
      // Given
      CreateExpenseCommand command = new CreateExpenseCommand();
      command.setMatterId(1L);
      command.setExpenseType("BUSINESS_TRAVEL");
      command.setExpenseCategory("TRANSPORTATION");
      command.setExpenseDate(LocalDate.now());
      command.setAmount(new BigDecimal("1000.00"));
      command.setDescription("差旅交通费");
      command.setVendorName("航空公司");
      command.setInvoiceNo("INV001");

      Matter matter = new Matter();
      matter.setId(1L);
      // mock两次调用：一次在createExpense中验证项目存在，一次在toDTO中获取项目名称
      when(matterRepository.findById(eq(1L))).thenReturn(matter).thenReturn(matter);

      // mock userRepository.findById，用于toDTO中获取申请人、审批人等
      User applicant = new User();
      applicant.setId(1L);
      applicant.setRealName("测试用户");
      when(userRepository.findById(eq(1L))).thenReturn(applicant);
      // 模拟插入操作并设置expense的ID
      when(expenseRepository.getBaseMapper().insert(any(Expense.class)))
          .thenAnswer(
              invocation -> {
                Expense exp = invocation.getArgument(0);
                // 设置一个模拟ID
                exp.setId(100L);
                return 1;
              });
      when(approverService.findDefaultApprover()).thenReturn(2L);
      doReturn("{}").when(objectMapper).writeValueAsString(any());
      // 匹配所有参数，使用any()更灵活
      when(approvalService.createApproval(
              anyString(),
              anyLong(),
              anyString(),
              anyString(),
              anyLong(),
              anyString(),
              anyString(),
              anyString()))
          .thenReturn(1L);

      // When
      ExpenseDTO result = expenseAppService.createExpense(command);

      // Then
      assertNotNull(result);
      assertTrue(result.getExpenseNo().startsWith("EXP"));
      assertEquals(new BigDecimal("1000.00"), result.getAmount());
      assertEquals(ExpenseStatus.PENDING, result.getStatus());

      verify(matterRepository, times(2)).findById(eq(1L));
      verify(expenseRepository.getBaseMapper()).insert(any(Expense.class));
      verify(approvalService)
          .createApproval(
              anyString(),
              anyLong(),
              anyString(),
              anyString(),
              anyLong(),
              anyString(),
              anyString(),
              anyString());
    }

    @Test
    @DisplayName("创建费用报销 - 项目不存在时失败")
    void createExpense_MatterNotFound() {
      // Given
      CreateExpenseCommand command = new CreateExpenseCommand();
      command.setMatterId(999L);
      command.setAmount(new BigDecimal("1000.00"));

      when(matterRepository.findById(eq(999L))).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> expenseAppService.createExpense(command));
      assertEquals("项目不存在", exception.getMessage());
    }
  }

  // ==================== 审批费用报销测试 ====================

  @Nested
  @DisplayName("审批费用报销测试")
  class ApproveExpenseTests {

    @Test
    @DisplayName("成功审批通过费用报销")
    void approveExpense_Success_Approve() {
      // Given
      ApproveExpenseCommand command = new ApproveExpenseCommand();
      command.setExpenseId(1L);
      command.setAction("APPROVE");
      command.setComment("同意报销");

      Expense expense =
          Expense.builder().id(1L).expenseNo("EXP2024010001").status(ExpenseStatus.PENDING).build();

      when(expenseRepository.findById(eq(1L))).thenReturn(expense);
      when(expenseRepository.getBaseMapper().updateById(any(Expense.class))).thenReturn(1);
      when(userRepository.findRoleCodesByUserId(eq(1L)))
          .thenReturn(Arrays.asList("ADMIN", "FINANCE"));

      // When
      ExpenseDTO result = expenseAppService.approveExpense(command);

      // Then
      assertNotNull(result);
      assertEquals(ExpenseStatus.APPROVED, result.getStatus());
      assertEquals(1L, result.getApproverId()); // 当前用户
      assertNotNull(result.getApprovedAt());

      verify(expenseRepository).findById(eq(1L));
      verify(expenseRepository.getBaseMapper()).updateById(any(Expense.class));
    }

    @Test
    @DisplayName("成功驳回费用报销")
    void approveExpense_Success_Reject() {
      // Given
      ApproveExpenseCommand command = new ApproveExpenseCommand();
      command.setExpenseId(1L);
      command.setAction("REJECT");
      command.setComment("缺少发票");

      Expense expense = Expense.builder().id(1L).status(ExpenseStatus.PENDING).build();

      when(expenseRepository.findById(eq(1L))).thenReturn(expense);
      when(expenseRepository.getBaseMapper().updateById(any(Expense.class))).thenReturn(1);
      when(userRepository.findRoleCodesByUserId(eq(1L)))
          .thenReturn(Arrays.asList("ADMIN", "FINANCE"));

      // When
      ExpenseDTO result = expenseAppService.approveExpense(command);

      // Then
      assertNotNull(result);
      assertEquals(ExpenseStatus.REJECTED, result.getStatus());
      assertEquals("缺少发票", result.getApprovalComment());
    }

    @Test
    @DisplayName("审批费用报销 - 非待审批状态时失败")
    void approveExpense_NotPending() {
      // Given
      ApproveExpenseCommand command = new ApproveExpenseCommand();
      command.setExpenseId(1L);
      command.setAction("APPROVE");

      Expense expense =
          Expense.builder()
              .id(1L)
              .status(ExpenseStatus.APPROVED) // 已审批状态
              .build();

      when(expenseRepository.findById(eq(1L))).thenReturn(expense);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> expenseAppService.approveExpense(command));
      assertEquals("只能审批待审批状态的报销单", exception.getMessage());
    }
  }

  // ==================== 确认支付测试 ====================

  @Nested
  @DisplayName("确认支付测试")
  class ConfirmPaymentTests {

    @Test
    @DisplayName("成功确认支付")
    void confirmPayment_Success() {
      // Given
      Long expenseId = 1L;
      String paymentMethod = "BANK_TRANSFER";

      Expense expense =
          Expense.builder()
              .id(expenseId)
              .expenseNo("EXP2024010001")
              .status(ExpenseStatus.APPROVED)
              .build();

      when(expenseRepository.findById(eq(expenseId))).thenReturn(expense);
      when(expenseRepository.getBaseMapper().updateById(any(Expense.class))).thenReturn(1);
      when(userRepository.findRoleCodesByUserId(eq(1L)))
          .thenReturn(Arrays.asList("ADMIN", "FINANCE"));

      // When
      ExpenseDTO result = expenseAppService.confirmPayment(expenseId, paymentMethod);

      // Then
      assertNotNull(result);
      assertEquals(ExpenseStatus.PAID, result.getStatus());
      assertEquals(paymentMethod, result.getPaymentMethod());
      assertNotNull(result.getPaidAt());
      assertEquals(1L, result.getPaidBy()); // 当前用户

      verify(expenseRepository).findById(eq(expenseId));
      verify(expenseRepository.getBaseMapper()).updateById(any(Expense.class));
    }

    @Test
    @DisplayName("确认支付 - 非已审批状态时失败")
    void confirmPayment_NotApproved() {
      // Given
      Long expenseId = 1L;

      Expense expense =
          Expense.builder()
              .id(expenseId)
              .status(ExpenseStatus.PENDING) // 待审批状态
              .build();

      when(expenseRepository.findById(eq(expenseId))).thenReturn(expense);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> expenseAppService.confirmPayment(expenseId, "BANK_TRANSFER"));
      assertEquals("只能支付已审批的报销单", exception.getMessage());
    }
  }

  // ==================== 成本归集测试 ====================

  @Nested
  @DisplayName("成本归集测试")
  class AllocateCostTests {

    @Test
    @DisplayName("成功归集成本到项目")
    void allocateCost_Success() {
      // Given
      AllocateCostCommand command = new AllocateCostCommand();
      command.setMatterId(1L);
      command.setExpenseIds(Arrays.asList(1L, 2L));

      Matter matter = new Matter();
      matter.setId(1L);

      Expense expense1 =
          Expense.builder()
              .id(1L)
              .expenseNo("EXP001")
              .amount(new BigDecimal("1000.00"))
              .status(ExpenseStatus.PAID)
              .build();

      Expense expense2 =
          Expense.builder()
              .id(2L)
              .expenseNo("EXP002")
              .amount(new BigDecimal("2000.00"))
              .status(ExpenseStatus.PAID)
              .build();

      when(matterRepository.findById(eq(1L))).thenReturn(matter);
      when(expenseRepository.findById(eq(1L))).thenReturn(expense1);
      when(expenseRepository.findById(eq(2L))).thenReturn(expense2);
      when(costAllocationRepository.getBaseMapper().insert(any(CostAllocation.class)))
          .thenReturn(1);
      when(expenseRepository.getBaseMapper().updateById(any(Expense.class))).thenReturn(1);

      // When
      expenseAppService.allocateCost(command);

      // Then
      verify(matterRepository).findById(eq(1L));
      verify(expenseRepository, times(2)).findById(anyLong());
      verify(costAllocationRepository.getBaseMapper(), times(2)).insert(any(CostAllocation.class));
      verify(expenseRepository.getBaseMapper(), times(2)).updateById(any(Expense.class));

      // 验证费用已被标记为已归集
      assertTrue(expense1.getIsCostAllocation());
      assertEquals(1L, expense1.getAllocatedToMatterId());
      assertTrue(expense2.getIsCostAllocation());
      assertEquals(1L, expense2.getAllocatedToMatterId());
    }

    @Test
    @DisplayName("成本归集 - 项目不存在时失败")
    void allocateCost_MatterNotFound() {
      // Given
      AllocateCostCommand command = new AllocateCostCommand();
      command.setMatterId(999L);
      command.setExpenseIds(Collections.singletonList(1L));

      when(matterRepository.findById(eq(999L))).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> expenseAppService.allocateCost(command));
      assertEquals("项目不存在", exception.getMessage());
    }
  }

  // ==================== 成本分摊测试 ====================

  @Nested
  @DisplayName("成本分摊测试")
  class SplitCostTests {

    @Test
    @DisplayName("成功平均分摊成本到多个项目")
    void splitCost_Success_Equal() {
      // Given
      SplitCostCommand command = new SplitCostCommand();
      command.setExpenseId(1L);
      command.setSplitMethod("EQUAL");
      command.setMatterIds(Arrays.asList(1L, 2L));
      command.setRemark("平均分摊");

      Expense expense =
          Expense.builder()
              .id(1L)
              .expenseNo("EXP001")
              .amount(new BigDecimal("1000.00"))
              .status(ExpenseStatus.PAID)
              .build();

      Matter matter1 = new Matter();
      matter1.setId(1L);
      Matter matter2 = new Matter();
      matter2.setId(2L);

      when(expenseRepository.findById(eq(1L))).thenReturn(expense);
      when(matterRepository.findById(eq(1L))).thenReturn(matter1);
      when(matterRepository.findById(eq(2L))).thenReturn(matter2);
      when(costSplitRepository.getBaseMapper().insert(any(CostSplit.class))).thenReturn(1);

      // When
      expenseAppService.splitCost(command);

      // Then
      verify(expenseRepository).findById(eq(1L));
      verify(matterRepository, times(2)).findById(anyLong());
      verify(costSplitRepository.getBaseMapper(), times(2)).insert(any(CostSplit.class));
    }

    @Test
    @DisplayName("成本分摊 - 费用不存在时失败")
    void splitCost_ExpenseNotFound() {
      // Given
      SplitCostCommand command = new SplitCostCommand();
      command.setExpenseId(999L);
      command.setSplitMethod("EQUAL");
      command.setMatterIds(Arrays.asList(1L, 2L));

      when(expenseRepository.findById(eq(999L))).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> expenseAppService.splitCost(command));
      assertEquals("费用报销记录不存在", exception.getMessage());
    }
  }

  // ==================== 删除费用报销测试 ====================

  @Nested
  @DisplayName("删除费用报销测试")
  class DeleteExpenseTests {

    @Test
    @DisplayName("成功删除待审批状态的费用报销")
    void deleteExpense_Success() {
      // Given
      Long expenseId = 1L;

      Expense expense =
          Expense.builder()
              .id(expenseId)
              .expenseNo("EXP001")
              .applicantId(1L) // 当前用户
              .status(ExpenseStatus.PENDING)
              .build();

      when(expenseRepository.findById(eq(expenseId))).thenReturn(expense);
      // softDelete返回Boolean类型，表示是否成功
      when(expenseRepository.softDelete(eq(expenseId))).thenReturn(true);

      // When
      expenseAppService.deleteExpense(expenseId);

      // Then
      verify(expenseRepository).findById(eq(expenseId));
      verify(expenseRepository).softDelete(eq(expenseId));
    }

    @Test
    @DisplayName("删除费用报销 - 非待审批状态时失败")
    void deleteExpense_NotPending() {
      // Given
      Long expenseId = 1L;

      Expense expense =
          Expense.builder()
              .id(expenseId)
              .applicantId(1L)
              .status(ExpenseStatus.APPROVED) // 已审批状态
              .build();

      when(expenseRepository.findById(eq(expenseId))).thenReturn(expense);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> expenseAppService.deleteExpense(expenseId));
      assertEquals("只能删除待审批状态的报销单", exception.getMessage());
    }
  }

  // ==================== 边界条件测试 ====================

  @Nested
  @DisplayName("边界条件测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("零金额费用报销测试")
    void zeroAmountExpense() throws Exception {
      // Given
      CreateExpenseCommand command = new CreateExpenseCommand();
      command.setMatterId(1L);
      command.setAmount(BigDecimal.ZERO);
      command.setDescription("测试零金额费用");

      Matter matter = new Matter();
      matter.setId(1L);
      when(matterRepository.findById(eq(1L))).thenReturn(matter);
      when(expenseRepository.getBaseMapper().insert(any(Expense.class))).thenReturn(1);
      when(approverService.findDefaultApprover()).thenReturn(2L);
      doReturn("{}").when(objectMapper).writeValueAsString(any());

      // When
      ExpenseDTO result = expenseAppService.createExpense(command);

      // Then
      assertNotNull(result);
      assertEquals(BigDecimal.ZERO, result.getAmount());
    }

    @Test
    @DisplayName("空值处理测试")
    void nullValueHandling() {
      // Given
      ExpenseQueryDTO query = new ExpenseQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      when(matterAppService.getAccessibleMatterIds(eq("ALL"), eq(1L), eq(100L))).thenReturn(null);
      when(expenseMapper.selectExpensePage(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
          .thenReturn(Collections.emptyList());

      // When (不传任何过滤条件)
      PageResult<ExpenseDTO> result = expenseAppService.listExpenses(query);

      // Then - 应该正常执行，不抛出异常
      assertNotNull(result);
      assertEquals(0, result.getTotal());
    }
  }
}
