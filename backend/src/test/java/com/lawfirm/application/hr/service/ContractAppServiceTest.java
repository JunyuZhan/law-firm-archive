package com.lawfirm.application.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateContractCommand;
import com.lawfirm.application.hr.command.UpdateContractCommand;
import com.lawfirm.application.hr.dto.ContractDTO;
import com.lawfirm.application.hr.dto.ContractQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Contract;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.repository.ContractRepository;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** ContractAppService (HR) 单元测试 测试劳动合同服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContractAppService (HR) 劳动合同服务测试")
class ContractAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_CONTRACT_ID = 100L;
  private static final Long TEST_EMPLOYEE_ID = 200L;

  @Mock private ContractRepository contractRepository;

  @Mock private ContractMapper contractMapper;

  @Mock private EmployeeRepository employeeRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private ContractAppService contractAppService;

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
  @DisplayName("创建劳动合同测试")
  class CreateContractTests {

    @Test
    @DisplayName("应该成功创建劳动合同")
    void createContract_shouldSuccess() {
      // Given
      CreateContractCommand command = new CreateContractCommand();
      command.setEmployeeId(TEST_EMPLOYEE_ID);
      command.setContractType("FIXED");
      command.setStartDate(LocalDate.now());
      command.setEndDate(LocalDate.now().plusYears(3));
      command.setBaseSalary(new BigDecimal("15000"));
      command.setProbationMonths(3);

      Employee employee = Employee.builder().id(TEST_EMPLOYEE_ID).employeeNo("EMP001").build();

      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);
      when(contractRepository.findByContractNo(anyString())).thenReturn(Optional.empty());
      when(contractRepository.save(any(Contract.class)))
          .thenAnswer(
              invocation -> {
                Contract contract = invocation.getArgument(0);
                contract.setId(TEST_CONTRACT_ID);
                contract.setContractNo("HT2024001");
                return true;
              });

      // When
      ContractDTO result = contractAppService.createContract(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getEmployeeId()).isEqualTo(TEST_EMPLOYEE_ID);
      assertThat(result.getStatus()).isEqualTo("ACTIVE");
      verify(contractRepository).save(any(Contract.class));
    }

    @Test
    @DisplayName("开始日期晚于结束日期应该失败")
    void createContract_shouldFail_whenStartAfterEnd() {
      // Given
      CreateContractCommand command = new CreateContractCommand();
      command.setEmployeeId(TEST_EMPLOYEE_ID);
      command.setStartDate(LocalDate.now().plusYears(1));
      command.setEndDate(LocalDate.now());

      Employee employee = Employee.builder().id(TEST_EMPLOYEE_ID).build();

      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> contractAppService.createContract(command));
      assertThat(exception.getMessage()).contains("开始日期不能晚于结束日期");
    }

    @Test
    @DisplayName("合同编号已存在应该失败")
    void createContract_shouldFail_whenContractNoExists() {
      // Given
      CreateContractCommand command = new CreateContractCommand();
      command.setEmployeeId(TEST_EMPLOYEE_ID);
      command.setContractNo("HT2024001");

      Employee employee = Employee.builder().id(TEST_EMPLOYEE_ID).build();

      Contract existing = Contract.builder().id(999L).contractNo("HT2024001").build();

      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);
      when(contractRepository.findByContractNo("HT2024001")).thenReturn(Optional.of(existing));

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> contractAppService.createContract(command));
      assertThat(exception.getMessage()).contains("合同编号已存在");
    }
  }

  @Nested
  @DisplayName("查询劳动合同测试")
  class QueryContractTests {

    @Test
    @DisplayName("应该成功分页查询劳动合同")
    @SuppressWarnings("unchecked")
    void listContracts_shouldSuccess() {
      // Given
      ContractQueryDTO query = new ContractQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Contract contract =
          Contract.builder()
              .id(TEST_CONTRACT_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .contractNo("HT2024001")
              .build();

      securityUtilsMock.when(() -> SecurityUtils.hasAllDataScope()).thenReturn(true);

      Page<Contract> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(contract));
      page.setTotal(1L);

      when(contractMapper.selectContractPage(any(Page.class), any(), any(), any()))
          .thenReturn(page);
      when(employeeRepository.listByIds(anyList())).thenReturn(Collections.emptyList());
      when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

      // When
      PageResult<ContractDTO> result = contractAppService.listContracts(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getContractNo()).isEqualTo("HT2024001");
    }

    @Test
    @DisplayName("普通员工只能查看自己的合同")
    @SuppressWarnings("unchecked")
    void listContracts_shouldFilter_whenNotAdmin() {
      // Given
      ContractQueryDTO query = new ContractQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Employee currentEmployee =
          Employee.builder().id(TEST_EMPLOYEE_ID).userId(TEST_USER_ID).build();

      securityUtilsMock.when(() -> SecurityUtils.hasAllDataScope()).thenReturn(false);
      when(employeeRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(currentEmployee));

      Page<Contract> page = new Page<>(1, 10);
      page.setRecords(Collections.emptyList());
      page.setTotal(0L);

      when(contractMapper.selectContractPage(any(Page.class), eq(TEST_EMPLOYEE_ID), any(), any()))
          .thenReturn(page);

      // When
      PageResult<ContractDTO> result = contractAppService.listContracts(query);

      // Then
      assertThat(result).isNotNull();
      verify(contractMapper)
          .selectContractPage(any(Page.class), eq(TEST_EMPLOYEE_ID), any(), any());
    }

    @Test
    @DisplayName("应该成功获取合同详情")
    void getContractById_shouldSuccess() {
      // Given
      Contract contract =
          Contract.builder()
              .id(TEST_CONTRACT_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .contractNo("HT2024001")
              .build();

      securityUtilsMock.when(() -> SecurityUtils.hasAllDataScope()).thenReturn(true);

      when(contractRepository.getByIdOrThrow(eq(TEST_CONTRACT_ID), anyString()))
          .thenReturn(contract);
      when(employeeRepository.findById(TEST_EMPLOYEE_ID)).thenReturn(new Employee());
      when(userRepository.findById(any())).thenReturn(new User());

      // When
      ContractDTO result = contractAppService.getContractById(TEST_CONTRACT_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContractNo()).isEqualTo("HT2024001");
    }

    @Test
    @DisplayName("普通员工无权查看他人的合同")
    void getContractById_shouldFail_whenNotOwner() {
      // Given
      Contract contract =
          Contract.builder()
              .id(TEST_CONTRACT_ID)
              .employeeId(999L) // 其他员工
              .build();

      Employee currentEmployee =
          Employee.builder().id(TEST_EMPLOYEE_ID).userId(TEST_USER_ID).build();

      securityUtilsMock.when(() -> SecurityUtils.hasAllDataScope()).thenReturn(false);
      when(contractRepository.getByIdOrThrow(eq(TEST_CONTRACT_ID), anyString()))
          .thenReturn(contract);
      when(employeeRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(currentEmployee));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> contractAppService.getContractById(TEST_CONTRACT_ID));
      assertThat(exception.getMessage()).contains("无权查看");
    }
  }

  @Nested
  @DisplayName("更新劳动合同测试")
  class UpdateContractTests {

    @Test
    @DisplayName("应该成功更新劳动合同")
    void updateContract_shouldSuccess() {
      // Given
      Contract contract =
          Contract.builder()
              .id(TEST_CONTRACT_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .contractNo("HT2024001")
              .baseSalary(new BigDecimal("15000"))
              .build();

      UpdateContractCommand command = new UpdateContractCommand();
      command.setBaseSalary(new BigDecimal("18000"));

      when(contractRepository.getByIdOrThrow(eq(TEST_CONTRACT_ID), anyString()))
          .thenReturn(contract);
      when(contractRepository.updateById(any(Contract.class))).thenReturn(true);
      when(employeeRepository.findById(TEST_EMPLOYEE_ID)).thenReturn(new Employee());
      when(userRepository.findById(any())).thenReturn(new User());

      // When
      ContractDTO result = contractAppService.updateContract(TEST_CONTRACT_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(contract.getBaseSalary()).isEqualByComparingTo(new BigDecimal("18000"));
      verify(contractRepository).updateById(contract);
    }
  }

  @Nested
  @DisplayName("续签合同测试")
  class RenewContractTests {

    @Test
    @DisplayName("应该成功续签合同")
    void renewContract_shouldSuccess() {
      // Given
      Contract oldContract =
          Contract.builder()
              .id(TEST_CONTRACT_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .contractNo("HT2024001")
              .renewCount(0)
              .endDate(LocalDate.now())
              .status("ACTIVE")
              .build();

      when(contractRepository.getByIdOrThrow(eq(TEST_CONTRACT_ID), anyString()))
          .thenReturn(oldContract);
      when(contractRepository.findByContractNo(anyString())).thenReturn(Optional.empty());
      when(contractRepository.save(any(Contract.class)))
          .thenAnswer(
              invocation -> {
                Contract newContract = invocation.getArgument(0);
                newContract.setId(200L);
                newContract.setContractNo("HT2024002");
                return true;
              });
      when(contractRepository.updateById(any(Contract.class))).thenReturn(true);
      when(employeeRepository.findById(TEST_EMPLOYEE_ID)).thenReturn(new Employee());
      when(userRepository.findById(any())).thenReturn(new User());

      // When
      ContractDTO result =
          contractAppService.renewContract(
              TEST_CONTRACT_ID, LocalDate.now().plusDays(1), LocalDate.now().plusYears(3));

      // Then
      assertThat(result).isNotNull();
      verify(contractRepository).save(any(Contract.class));
      verify(contractRepository).updateById(oldContract);
    }
  }
}
