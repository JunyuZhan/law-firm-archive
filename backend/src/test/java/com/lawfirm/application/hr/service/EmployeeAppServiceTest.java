package com.lawfirm.application.hr.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateEmployeeCommand;
import com.lawfirm.application.hr.command.UpdateEmployeeCommand;
import com.lawfirm.application.hr.dto.EmployeeDTO;
import com.lawfirm.application.hr.dto.EmployeeQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.entity.PayrollItem;
import com.lawfirm.domain.hr.repository.ContractRepository;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.hr.repository.PayrollItemRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.EmployeeMapper;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * EmployeeAppService 单元测试 测试HR模块员工档案管理核心业务逻辑：员工档案创建、查询、更新、删除等
 *
 * <p>状态：进行中（另一个工程师在同时工作，请注意协调）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeAppServiceTest {

  @Mock private EmployeeRepository employeeRepository;

  @Mock private EmployeeMapper employeeMapper;

  @Mock private UserRepository userRepository;

  @Mock private PayrollItemRepository payrollItemRepository;

  @Mock private ContractRepository hrContractRepository;

  @InjectMocks private EmployeeAppService employeeAppService;

  private LambdaQueryChainWrapper<PayrollItem> payrollQueryWrapper;

  @BeforeEach
  void setUp() {
    // 模拟 LambdaQueryChainWrapper 用于删除测试
    @SuppressWarnings("unchecked")
    LambdaQueryChainWrapper<PayrollItem> mockWrapper = mock(LambdaQueryChainWrapper.class);
    payrollQueryWrapper = mockWrapper;
    when(payrollItemRepository.lambdaQuery()).thenReturn(payrollQueryWrapper);
    when(payrollQueryWrapper.eq(any(), any())).thenReturn(payrollQueryWrapper);
    when(payrollQueryWrapper.list()).thenReturn(Collections.emptyList());
  }

  // ==================== 基础测试数据 ====================

  private Employee createTestEmployee() {
    return Employee.builder()
        .id(1L)
        .userId(100L)
        .employeeNo("EMP2024010001")
        .gender("MALE")
        .birthDate(LocalDate.of(1990, 1, 1))
        .idCard("110101199001011234")
        .nationality("中国")
        .nativePlace("北京")
        .politicalStatus("中共党员")
        .education("硕士")
        .major("法学")
        .graduationSchool("北京大学")
        .graduationDate(LocalDate.of(2015, 7, 1))
        .emergencyContact("张三")
        .emergencyPhone("13800138000")
        .address("北京市朝阳区")
        .lawyerLicenseNo("12345678")
        .licenseIssueDate(LocalDate.of(2016, 6, 1))
        .licenseExpireDate(LocalDate.of(2026, 6, 1))
        .licenseStatus("VALID")
        .practiceArea("民事诉讼法")
        .practiceYears(8)
        .position("资深律师")
        .level("P8")
        .entryDate(LocalDate.of(2018, 3, 1))
        .probationEndDate(LocalDate.of(2018, 9, 1))
        .status("ACTIVE")
        .workStatus("ACTIVE")
        .remark("测试员工")
        .build();
  }

  private User createTestUser() {
    User user = new User();
    user.setId(100L);
    user.setRealName("测试员工");
    user.setEmail("test@example.com");
    user.setPhone("13800138000");
    user.setDepartmentId(10L);
    return user;
  }

  // ==================== 分页查询测试 ====================

  @Nested
  @DisplayName("分页查询员工档案测试")
  class ListEmployeesTests {

    @Test
    @DisplayName("成功查询员工档案列表")
    @SuppressWarnings("unchecked")
    void listEmployees_Success() {
      // Given
      EmployeeQueryDTO query = new EmployeeQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Employee employee = createTestEmployee();
      List<Employee> employees = Collections.singletonList(employee);

      IPage<Employee> page = mock(IPage.class);
      when(page.getRecords()).thenReturn(employees);
      when(page.getTotal()).thenReturn(1L);

      when(employeeMapper.selectEmployeePage(any(Page.class), any(), any(), any(), any(), any()))
          .thenReturn(page);

      when(userRepository.listByIds(anyList()))
          .thenReturn(Collections.singletonList(createTestUser()));

      // When
      PageResult<EmployeeDTO> result = employeeAppService.listEmployees(query);

      // Then
      assertNotNull(result);
      assertEquals(1, result.getTotal());
      assertEquals(1, result.getList().size());

      EmployeeDTO dto = result.getList().get(0);
      assertEquals("EMP2024010001", dto.getEmployeeNo());
      assertEquals("测试员工", dto.getRealName());
      assertEquals("在职", dto.getWorkStatusName());

      verify(employeeMapper).selectEmployeePage(any(Page.class), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("查询员工档案列表 - 无结果")
    void listEmployees_NoResults() {
      // Given
      EmployeeQueryDTO query = new EmployeeQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      @SuppressWarnings("unchecked")
      IPage<Employee> page = mock(IPage.class);
      when(page.getRecords()).thenReturn(Collections.emptyList());
      when(page.getTotal()).thenReturn(0L);

      @SuppressWarnings("unchecked")
      Page<Employee> pageParam = any(Page.class);
      when(employeeMapper.selectEmployeePage(pageParam, any(), any(), any(), any(), any()))
          .thenReturn(page);

      // When
      PageResult<EmployeeDTO> result = employeeAppService.listEmployees(query);

      // Then
      assertNotNull(result);
      assertEquals(0, result.getTotal());
      assertTrue(result.getList().isEmpty());
    }
  }

  // ==================== 按ID查询测试 ====================

  @Nested
  @DisplayName("按ID查询员工档案测试")
  class GetEmployeeByIdTests {

    @Test
    @DisplayName("成功按ID查询员工档案")
    void getEmployeeById_Success() {
      // Given
      Long employeeId = 1L;
      Employee employee = createTestEmployee();
      User user = createTestUser();

      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString())).thenReturn(employee);
      when(userRepository.findById(eq(100L))).thenReturn(user);

      // When
      EmployeeDTO result = employeeAppService.getEmployeeById(employeeId);

      // Then
      assertNotNull(result);
      assertEquals("EMP2024010001", result.getEmployeeNo());
      assertEquals("测试员工", result.getRealName());
      assertEquals("test@example.com", result.getEmail());
      assertEquals("在职", result.getWorkStatusName());

      verify(employeeRepository).getByIdOrThrow(eq(employeeId), anyString());
    }

    @Test
    @DisplayName("按ID查询员工档案 - 档案不存在")
    void getEmployeeById_NotFound() {
      // Given
      Long employeeId = 999L;
      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString()))
          .thenThrow(new BusinessException("员工档案不存在"));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> employeeAppService.getEmployeeById(employeeId));
      assertEquals("员工档案不存在", exception.getMessage());
    }
  }

  // ==================== 按用户ID查询测试 ====================

  @Nested
  @DisplayName("按用户ID查询员工档案测试")
  class GetEmployeeByUserIdTests {

    @Test
    @DisplayName("成功按用户ID查询员工档案")
    void getEmployeeByUserId_Success() {
      // Given
      Long userId = 100L;
      Employee employee = createTestEmployee();
      User user = createTestUser();

      when(employeeRepository.findByUserId(eq(userId))).thenReturn(Optional.of(employee));
      when(userRepository.findById(eq(100L))).thenReturn(user);

      // When
      EmployeeDTO result = employeeAppService.getEmployeeByUserId(userId);

      // Then
      assertNotNull(result);
      assertEquals("EMP2024010001", result.getEmployeeNo());
      assertEquals("测试员工", result.getRealName());

      verify(employeeRepository).findByUserId(eq(userId));
    }

    @Test
    @DisplayName("按用户ID查询员工档案 - 档案不存在")
    void getEmployeeByUserId_NotFound() {
      // Given
      Long userId = 999L;
      when(employeeRepository.findByUserId(eq(userId))).thenReturn(Optional.empty());

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> employeeAppService.getEmployeeByUserId(userId));
      assertEquals("员工档案不存在", exception.getMessage());
    }
  }

  // ==================== 创建员工档案测试 ====================

  @Nested
  @DisplayName("创建员工档案测试")
  class CreateEmployeeTests {

    @Test
    @DisplayName("成功创建员工档案")
    void createEmployee_Success() {
      // Given
      CreateEmployeeCommand command = new CreateEmployeeCommand();
      command.setUserId(100L);
      command.setGender("MALE");
      command.setBirthDate(LocalDate.of(1990, 1, 1));
      command.setIdCard("110101199001011234");
      command.setPosition("律师");
      command.setLevel("P6");
      command.setEntryDate(LocalDate.now());

      User user = createTestUser();
      when(userRepository.getByIdOrThrow(eq(100L), anyString())).thenReturn(user);
      when(userRepository.findById(eq(100L))).thenReturn(user);
      when(employeeRepository.findByUserId(eq(100L))).thenReturn(Optional.empty());
      when(employeeRepository.findByEmployeeNo(anyString())).thenReturn(Optional.empty());
      when(employeeRepository.save(any(Employee.class)))
          .thenAnswer(
              invocation -> {
                Employee emp = invocation.getArgument(0);
                emp.setId(1L);
                return true; // save 返回 boolean
              });

      // When
      EmployeeDTO result = employeeAppService.createEmployee(command);

      // Then
      assertNotNull(result);
      assertTrue(result.getEmployeeNo().startsWith("EMP"));
      assertEquals("测试员工", result.getRealName());
      assertEquals("律师", result.getPosition());
      assertEquals("ACTIVE", result.getWorkStatus());

      verify(userRepository).getByIdOrThrow(eq(100L), anyString());
      verify(employeeRepository).findByUserId(eq(100L));
      verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("创建员工档案 - 用户不存在")
    void createEmployee_UserNotFound() {
      // Given
      CreateEmployeeCommand command = new CreateEmployeeCommand();
      command.setUserId(999L);

      when(userRepository.getByIdOrThrow(eq(999L), anyString()))
          .thenThrow(new BusinessException("用户不存在"));

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> employeeAppService.createEmployee(command));
      assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("创建员工档案 - 员工档案已存在")
    void createEmployee_AlreadyExists() {
      // Given
      CreateEmployeeCommand command = new CreateEmployeeCommand();
      command.setUserId(100L);

      User user = createTestUser();
      Employee existingEmployee = createTestEmployee();

      when(userRepository.getByIdOrThrow(eq(100L), anyString())).thenReturn(user);
      when(employeeRepository.findByUserId(eq(100L))).thenReturn(Optional.of(existingEmployee));

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> employeeAppService.createEmployee(command));
      assertEquals("该用户已有员工档案", exception.getMessage());
    }
  }

  // ==================== 更新员工档案测试 ====================

  @Nested
  @DisplayName("更新员工档案测试")
  class UpdateEmployeeTests {

    @Test
    @DisplayName("成功更新员工档案")
    void updateEmployee_Success() {
      // Given
      Long employeeId = 1L;
      UpdateEmployeeCommand command = new UpdateEmployeeCommand();
      command.setPosition("高级律师");
      command.setLevel("P7");
      command.setRemark("晋升为高级律师");

      Employee employee = createTestEmployee();
      User user = createTestUser();

      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString())).thenReturn(employee);
      when(employeeRepository.findByEmployeeNo(anyString())).thenReturn(Optional.empty());
      when(userRepository.findById(eq(100L))).thenReturn(user);
      when(employeeRepository.updateById(any(Employee.class))).thenReturn(true);

      // When
      EmployeeDTO result = employeeAppService.updateEmployee(employeeId, command);

      // Then
      assertNotNull(result);
      assertEquals("高级律师", result.getPosition());
      assertEquals("P7", result.getLevel());
      assertEquals("晋升为高级律师", result.getRemark());

      verify(employeeRepository).getByIdOrThrow(eq(employeeId), anyString());
      verify(employeeRepository).updateById(any(Employee.class));
    }

    @Test
    @DisplayName("更新员工档案 - 档案不存在")
    void updateEmployee_NotFound() {
      // Given
      Long employeeId = 999L;
      UpdateEmployeeCommand command = new UpdateEmployeeCommand();

      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString()))
          .thenThrow(new BusinessException("员工档案不存在"));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> employeeAppService.updateEmployee(employeeId, command));
      assertEquals("员工档案不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新员工档案 - 工号重复")
    void updateEmployee_DuplicateEmployeeNo() {
      // Given
      Long employeeId = 1L;
      UpdateEmployeeCommand command = new UpdateEmployeeCommand();
      command.setEmployeeNo("EMP2024010002");

      Employee employee = createTestEmployee();
      Employee otherEmployee = createTestEmployee();
      otherEmployee.setId(2L);

      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString())).thenReturn(employee);
      when(employeeRepository.findByEmployeeNo(eq("EMP2024010002")))
          .thenReturn(Optional.of(otherEmployee));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> employeeAppService.updateEmployee(employeeId, command));
      assertEquals("工号已存在", exception.getMessage());
    }
  }

  // ==================== 删除员工档案测试 ====================

  @Nested
  @DisplayName("删除员工档案测试")
  class DeleteEmployeeTests {

    @Test
    @DisplayName("成功删除员工档案")
    void deleteEmployee_Success() {
      // Given
      Long employeeId = 1L;
      Employee employee = createTestEmployee();

      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString())).thenReturn(employee);
      // 使用@BeforeEach中配置的payrollQueryWrapper，不需要重复设置
      when(hrContractRepository.findActiveContractByEmployeeId(eq(employeeId)))
          .thenReturn(Optional.empty());
      when(employeeRepository.softDelete(eq(employeeId))).thenReturn(true);

      // When
      employeeAppService.deleteEmployee(employeeId);

      // Then
      verify(employeeRepository).getByIdOrThrow(eq(employeeId), anyString());
      // 验证lambdaQuery被调用，并且payrollQueryWrapper的eq方法被调用两次（employeeId和deleted）
      verify(payrollItemRepository).lambdaQuery();
      verify(payrollQueryWrapper, times(2)).eq(any(), any());
      verify(payrollQueryWrapper).list();
      verify(hrContractRepository).findActiveContractByEmployeeId(eq(employeeId));
      verify(employeeRepository).softDelete(eq(employeeId));
    }

    @Test
    @DisplayName("删除员工档案 - 存在工资记录")
    void deleteEmployee_HasPayrollItems() {
      // Given
      Long employeeId = 1L;
      Employee employee = createTestEmployee();
      PayrollItem payrollItem = new PayrollItem();
      payrollItem.setId(1L);
      payrollItem.setEmployeeId(employeeId);

      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString())).thenReturn(employee);
      when(payrollItemRepository
              .lambdaQuery()
              .eq(PayrollItem::getEmployeeId, employeeId)
              .eq(PayrollItem::getDeleted, false)
              .list())
          .thenReturn(Collections.singletonList(payrollItem));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> employeeAppService.deleteEmployee(employeeId));
      assertEquals("该员工存在工资记录，无法删除。如需删除请先处理相关工资数据。", exception.getMessage());
    }

    @Test
    @DisplayName("删除员工档案 - 存在生效中的劳动合同")
    void deleteEmployee_HasActiveContract() {
      // Given
      Long employeeId = 1L;
      Employee employee = createTestEmployee();

      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString())).thenReturn(employee);
      when(payrollItemRepository
              .lambdaQuery()
              .eq(PayrollItem::getEmployeeId, employeeId)
              .eq(PayrollItem::getDeleted, false)
              .list())
          .thenReturn(Collections.emptyList());
      when(hrContractRepository.findActiveContractByEmployeeId(eq(employeeId)))
          .thenReturn(Optional.of(mock(com.lawfirm.domain.hr.entity.Contract.class)));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> employeeAppService.deleteEmployee(employeeId));
      assertEquals("该员工存在生效中的劳动合同，无法删除。请先终止劳动合同。", exception.getMessage());
    }
  }

  // ==================== 边界条件测试 ====================

  @Nested
  @DisplayName("边界条件测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("空值处理测试")
    void nullValueHandling() {
      // Given
      EmployeeQueryDTO query = new EmployeeQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      @SuppressWarnings("unchecked")
      IPage<Employee> page = mock(IPage.class);
      when(page.getRecords()).thenReturn(Collections.emptyList());
      when(page.getTotal()).thenReturn(0L);

      @SuppressWarnings("unchecked")
      Page<Employee> pageParam3 = any(Page.class);
      when(employeeMapper.selectEmployeePage(pageParam3, any(), any(), any(), any(), any()))
          .thenReturn(page);

      // When (不传任何过滤条件)
      PageResult<EmployeeDTO> result = employeeAppService.listEmployees(query);

      // Then - 应该正常执行，不抛出异常
      assertNotNull(result);
      assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("状态名称转换测试")
    void statusNameConversion() {
      // Given
      Long employeeId = 1L;
      Employee employee = createTestEmployee();
      employee.setStatus("PROBATION");
      employee.setWorkStatus("PROBATION");
      User user = createTestUser();

      when(employeeRepository.getByIdOrThrow(eq(employeeId), anyString())).thenReturn(employee);
      when(userRepository.findById(eq(100L))).thenReturn(user);

      // When
      EmployeeDTO result = employeeAppService.getEmployeeById(employeeId);

      // Then
      assertNotNull(result);
      assertEquals("试用", result.getWorkStatusName());
    }
  }
}
