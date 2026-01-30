package com.lawfirm.application.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.ApproveRegularizationCommand;
import com.lawfirm.application.hr.command.CreateRegularizationCommand;
import com.lawfirm.application.hr.dto.RegularizationDTO;
import com.lawfirm.application.hr.dto.RegularizationQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.entity.Regularization;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.hr.repository.RegularizationRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.RegularizationMapper;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** RegularizationAppService 单元测试 测试转正申请服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RegularizationAppService 转正申请服务测试")
class RegularizationAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_REGULARIZATION_ID = 100L;
  private static final Long TEST_EMPLOYEE_ID = 200L;
  private static final Long TEST_APPROVER_ID = 300L;

  @Mock private RegularizationRepository regularizationRepository;

  @Mock private RegularizationMapper regularizationMapper;

  @Mock private EmployeeRepository employeeRepository;

  @Mock private UserRepository userRepository;

  @Mock private ApprovalService approvalService;

  @Mock private ApproverService approverService;

  @InjectMocks private RegularizationAppService regularizationAppService;

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
  @DisplayName("创建转正申请测试")
  class CreateRegularizationTests {

    @Test
    @DisplayName("应该成功创建转正申请")
    void createRegularization_shouldSuccess() {
      // Given
      CreateRegularizationCommand command = new CreateRegularizationCommand();
      command.setEmployeeId(TEST_EMPLOYEE_ID);
      command.setProbationStartDate(LocalDate.now().minusMonths(3));
      command.setProbationEndDate(LocalDate.now());
      command.setExpectedRegularDate(LocalDate.now());
      command.setSelfEvaluation("自我评价");

      Employee employee = Employee.builder().id(TEST_EMPLOYEE_ID).employeeNo("EMP001").build();

      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);
      when(regularizationRepository.findByEmployeeId(TEST_EMPLOYEE_ID))
          .thenReturn(Collections.emptyList());
      when(regularizationRepository.save(any(Regularization.class)))
          .thenAnswer(
              invocation -> {
                Regularization reg = invocation.getArgument(0);
                reg.setId(TEST_REGULARIZATION_ID);
                reg.setApplicationNo("RZ2024001");
                return true;
              });
      when(approverService.findDefaultApprover()).thenReturn(TEST_APPROVER_ID);
      lenient()
          .when(
              approvalService.createApproval(
                  anyString(),
                  anyLong(),
                  anyString(),
                  anyString(),
                  anyLong(),
                  anyString(),
                  anyString(),
                  any()))
          .thenReturn(1L);

      // When
      RegularizationDTO result = regularizationAppService.createRegularization(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getEmployeeId()).isEqualTo(TEST_EMPLOYEE_ID);
      assertThat(result.getStatus()).isEqualTo("PENDING");
      verify(regularizationRepository).save(any(Regularization.class));
    }

    @Test
    @DisplayName("已有待审批申请不能重复创建")
    void createRegularization_shouldFail_whenPendingExists() {
      // Given
      CreateRegularizationCommand command = new CreateRegularizationCommand();
      command.setEmployeeId(TEST_EMPLOYEE_ID);

      Employee employee = Employee.builder().id(TEST_EMPLOYEE_ID).build();

      Regularization pending =
          Regularization.builder().id(TEST_REGULARIZATION_ID).status("PENDING").build();

      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);
      when(regularizationRepository.findByEmployeeId(TEST_EMPLOYEE_ID))
          .thenReturn(Collections.singletonList(pending));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> regularizationAppService.createRegularization(command));
      assertThat(exception.getMessage()).contains("已有待审批的转正申请");
    }
  }

  @Nested
  @DisplayName("审批转正申请测试")
  class ApproveRegularizationTests {

    @Test
    @DisplayName("应该成功审批通过转正申请")
    void approveRegularization_shouldSuccess_whenApproved() {
      // Given
      Regularization regularization =
          Regularization.builder()
              .id(TEST_REGULARIZATION_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .status("PENDING")
              .expectedRegularDate(LocalDate.now())
              .build();

      Employee employee =
          Employee.builder()
              .id(TEST_EMPLOYEE_ID)
              .employeeNo("EMP001")
              .workStatus("PROBATION")
              .build();

      ApproveRegularizationCommand command = new ApproveRegularizationCommand();
      command.setApproved(true);
      command.setComment("同意转正");

      when(regularizationRepository.getByIdOrThrow(eq(TEST_REGULARIZATION_ID), anyString()))
          .thenReturn(regularization);
      when(regularizationRepository.updateById(any(Regularization.class))).thenReturn(true);
      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);
      when(employeeRepository.updateById(any(Employee.class))).thenReturn(true);

      // When
      RegularizationDTO result =
          regularizationAppService.approveRegularization(TEST_REGULARIZATION_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(regularization.getStatus()).isEqualTo("APPROVED");
      assertThat(employee.getWorkStatus()).isEqualTo("ACTIVE");
      assertThat(employee.getRegularDate()).isNotNull();
      verify(regularizationRepository).updateById(regularization);
      verify(employeeRepository).updateById(employee);
    }

    @Test
    @DisplayName("应该成功拒绝转正申请")
    void approveRegularization_shouldSuccess_whenRejected() {
      // Given
      Regularization regularization =
          Regularization.builder().id(TEST_REGULARIZATION_ID).status("PENDING").build();

      ApproveRegularizationCommand command = new ApproveRegularizationCommand();
      command.setApproved(false);
      command.setComment("不符合转正条件");

      when(regularizationRepository.getByIdOrThrow(eq(TEST_REGULARIZATION_ID), anyString()))
          .thenReturn(regularization);
      when(regularizationRepository.updateById(any(Regularization.class))).thenReturn(true);

      // When
      RegularizationDTO result =
          regularizationAppService.approveRegularization(TEST_REGULARIZATION_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(regularization.getStatus()).isEqualTo("REJECTED");
      verify(regularizationRepository).updateById(regularization);
      verify(employeeRepository, never()).updateById(any(Employee.class));
    }

    @Test
    @DisplayName("非待审批状态不能审批")
    void approveRegularization_shouldFail_whenNotPending() {
      // Given
      Regularization regularization =
          Regularization.builder().id(TEST_REGULARIZATION_ID).status("APPROVED").build();

      ApproveRegularizationCommand command = new ApproveRegularizationCommand();

      when(regularizationRepository.getByIdOrThrow(eq(TEST_REGULARIZATION_ID), anyString()))
          .thenReturn(regularization);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () ->
                  regularizationAppService.approveRegularization(TEST_REGULARIZATION_ID, command));
      assertThat(exception.getMessage()).contains("不允许审批");
    }
  }

  @Nested
  @DisplayName("查询转正申请测试")
  class QueryRegularizationTests {

    @Test
    @DisplayName("应该成功分页查询转正申请")
    void listRegularizations_shouldSuccess() {
      // Given
      RegularizationQueryDTO query = new RegularizationQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Regularization regularization =
          Regularization.builder()
              .id(TEST_REGULARIZATION_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .applicationNo("RZ2024001")
              .build();

      Page<Regularization> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(regularization));
      page.setTotal(1L);

      @SuppressWarnings("unchecked")
      Page<Regularization> pageParam = any(Page.class);
      when(regularizationMapper.selectRegularizationPage(pageParam, any(), any())).thenReturn(page);
      when(employeeRepository.listByIds(anyList())).thenReturn(Collections.emptyList());
      when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

      // When
      PageResult<RegularizationDTO> result = regularizationAppService.listRegularizations(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getApplicationNo()).isEqualTo("RZ2024001");
    }

    @Test
    @DisplayName("应该成功获取转正申请详情")
    void getRegularizationById_shouldSuccess() {
      // Given
      Regularization regularization =
          Regularization.builder()
              .id(TEST_REGULARIZATION_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .applicationNo("RZ2024001")
              .build();

      when(regularizationRepository.getByIdOrThrow(eq(TEST_REGULARIZATION_ID), anyString()))
          .thenReturn(regularization);
      when(employeeRepository.findById(TEST_EMPLOYEE_ID)).thenReturn(new Employee());
      when(userRepository.findById(any())).thenReturn(new User());

      // When
      RegularizationDTO result =
          regularizationAppService.getRegularizationById(TEST_REGULARIZATION_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getApplicationNo()).isEqualTo("RZ2024001");
    }
  }

  @Nested
  @DisplayName("删除转正申请测试")
  class DeleteRegularizationTests {

    @Test
    @DisplayName("应该成功删除待审批状态的申请")
    void deleteRegularization_shouldSuccess() {
      // Given
      Regularization regularization =
          Regularization.builder().id(TEST_REGULARIZATION_ID).status("PENDING").build();

      when(regularizationRepository.getByIdOrThrow(eq(TEST_REGULARIZATION_ID), anyString()))
          .thenReturn(regularization);
      when(regularizationRepository.softDelete(TEST_REGULARIZATION_ID)).thenReturn(true);

      // When
      regularizationAppService.deleteRegularization(TEST_REGULARIZATION_ID);

      // Then
      verify(regularizationRepository).softDelete(TEST_REGULARIZATION_ID);
    }

    @Test
    @DisplayName("非待审批状态不能删除")
    void deleteRegularization_shouldFail_whenNotPending() {
      // Given
      Regularization regularization =
          Regularization.builder().id(TEST_REGULARIZATION_ID).status("APPROVED").build();

      when(regularizationRepository.getByIdOrThrow(eq(TEST_REGULARIZATION_ID), anyString()))
          .thenReturn(regularization);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> regularizationAppService.deleteRegularization(TEST_REGULARIZATION_ID));
      assertThat(exception.getMessage()).contains("只有待审批状态");
    }
  }
}
