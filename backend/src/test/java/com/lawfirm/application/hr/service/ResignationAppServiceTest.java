package com.lawfirm.application.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.ApproveResignationCommand;
import com.lawfirm.application.hr.command.CreateResignationCommand;
import com.lawfirm.application.hr.dto.ResignationDTO;
import com.lawfirm.application.hr.dto.ResignationQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.EmployeeStatus;
import com.lawfirm.common.constant.ResignationStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.entity.Resignation;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.hr.repository.ResignationRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ResignationMapper;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** ResignationAppService 单元测试 测试离职申请服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResignationAppService 离职申请服务测试")
class ResignationAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_RESIGNATION_ID = 100L;
  private static final Long TEST_EMPLOYEE_ID = 200L;
  private static final Long TEST_APPROVER_ID = 300L;
  private static final Long TEST_HANDOVER_PERSON_ID = 400L;

  @Mock private ResignationRepository resignationRepository;

  @Mock private ResignationMapper resignationMapper;

  @Mock private EmployeeRepository employeeRepository;

  @Mock private UserRepository userRepository;

  @Mock private ApprovalService approvalService;

  @Mock private ApproverService approverService;

  @InjectMocks private ResignationAppService resignationAppService;

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
  @DisplayName("创建离职申请测试")
  class CreateResignationTests {

    @Test
    @DisplayName("应该成功创建离职申请")
    void createResignation_shouldSuccess() {
      // Given
      CreateResignationCommand command = new CreateResignationCommand();
      command.setEmployeeId(TEST_EMPLOYEE_ID);
      command.setResignationType("VOLUNTARY");
      command.setResignationDate(LocalDate.now());
      command.setLastWorkDate(LocalDate.now().plusDays(30));
      command.setReason("个人原因");
      command.setHandoverPersonId(TEST_HANDOVER_PERSON_ID);

      Employee employee = Employee.builder().id(TEST_EMPLOYEE_ID).employeeNo("EMP001").build();

      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);
      when(resignationRepository.findByEmployeeId(TEST_EMPLOYEE_ID))
          .thenReturn(Collections.emptyList());
      when(resignationRepository.save(any(Resignation.class)))
          .thenAnswer(
              invocation -> {
                Resignation res = invocation.getArgument(0);
                res.setId(TEST_RESIGNATION_ID);
                res.setApplicationNo("RZ2024001");
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
      ResignationDTO result = resignationAppService.createResignation(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getEmployeeId()).isEqualTo(TEST_EMPLOYEE_ID);
      assertThat(result.getStatus()).isEqualTo(ResignationStatus.PENDING);
      verify(resignationRepository).save(any(Resignation.class));
    }

    @Test
    @DisplayName("已有待处理申请不能重复创建")
    void createResignation_shouldFail_whenPendingExists() {
      // Given
      CreateResignationCommand command = new CreateResignationCommand();
      command.setEmployeeId(TEST_EMPLOYEE_ID);

      Employee employee = Employee.builder().id(TEST_EMPLOYEE_ID).build();

      Resignation pending =
          Resignation.builder().id(TEST_RESIGNATION_ID).status(ResignationStatus.PENDING).build();

      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);
      when(resignationRepository.findByEmployeeId(TEST_EMPLOYEE_ID))
          .thenReturn(Collections.singletonList(pending));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> resignationAppService.createResignation(command));
      assertThat(exception.getMessage()).contains("已有待处理");
    }
  }

  @Nested
  @DisplayName("审批离职申请测试")
  class ApproveResignationTests {

    @Test
    @DisplayName("应该成功审批通过离职申请")
    void approveResignation_shouldSuccess_whenApproved() {
      // Given
      Resignation resignation =
          Resignation.builder()
              .id(TEST_RESIGNATION_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .status(ResignationStatus.PENDING)
              .lastWorkDate(LocalDate.now().plusDays(30))
              .reason("个人原因")
              .build();

      Employee employee =
          Employee.builder()
              .id(TEST_EMPLOYEE_ID)
              .employeeNo("EMP001")
              .workStatus(EmployeeStatus.ACTIVE)
              .build();

      ApproveResignationCommand command = new ApproveResignationCommand();
      command.setApproved(true);
      command.setComment("同意离职");

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);
      when(resignationRepository.updateById(any(Resignation.class))).thenReturn(true);
      when(employeeRepository.getByIdOrThrow(eq(TEST_EMPLOYEE_ID), anyString()))
          .thenReturn(employee);
      when(employeeRepository.updateById(any(Employee.class))).thenReturn(true);

      // When
      ResignationDTO result =
          resignationAppService.approveResignation(TEST_RESIGNATION_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(resignation.getStatus()).isEqualTo(ResignationStatus.APPROVED);
      assertThat(employee.getWorkStatus()).isEqualTo(EmployeeStatus.RESIGNED);
      assertThat(employee.getResignationDate()).isNotNull();
      verify(resignationRepository).updateById(resignation);
      verify(employeeRepository).updateById(employee);
    }

    @Test
    @DisplayName("应该成功拒绝离职申请")
    void approveResignation_shouldSuccess_whenRejected() {
      // Given
      Resignation resignation =
          Resignation.builder().id(TEST_RESIGNATION_ID).status(ResignationStatus.PENDING).build();

      ApproveResignationCommand command = new ApproveResignationCommand();
      command.setApproved(false);
      command.setComment("暂不同意");

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);
      when(resignationRepository.updateById(any(Resignation.class))).thenReturn(true);

      // When
      ResignationDTO result =
          resignationAppService.approveResignation(TEST_RESIGNATION_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(resignation.getStatus()).isEqualTo(ResignationStatus.REJECTED);
      verify(resignationRepository).updateById(resignation);
      verify(employeeRepository, never()).updateById(any(Employee.class));
    }

    @Test
    @DisplayName("非待审批状态不能审批")
    void approveResignation_shouldFail_whenNotPending() {
      // Given
      Resignation resignation =
          Resignation.builder().id(TEST_RESIGNATION_ID).status(ResignationStatus.APPROVED).build();

      ApproveResignationCommand command = new ApproveResignationCommand();

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> resignationAppService.approveResignation(TEST_RESIGNATION_ID, command));
      assertThat(exception.getMessage()).contains("不允许审批");
    }
  }

  @Nested
  @DisplayName("完成交接测试")
  class CompleteHandoverTests {

    @Test
    @DisplayName("交接人应该成功完成交接")
    void completeHandover_shouldSuccess_whenHandoverPerson() {
      // Given
      Resignation resignation =
          Resignation.builder()
              .id(TEST_RESIGNATION_ID)
              .handoverPersonId(TEST_USER_ID) // 当前用户是交接人
              .status(ResignationStatus.APPROVED)
              .handoverStatus(ResignationStatus.HANDOVER_PENDING)
              .build();

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);
      when(resignationRepository.updateById(any(Resignation.class))).thenReturn(true);

      // When
      ResignationDTO result = resignationAppService.completeHandover(TEST_RESIGNATION_ID, "交接完成");

      // Then
      assertThat(result).isNotNull();
      assertThat(resignation.getHandoverStatus()).isEqualTo(ResignationStatus.HANDOVER_COMPLETED);
      assertThat(resignation.getStatus()).isEqualTo(ResignationStatus.COMPLETED);
      verify(resignationRepository).updateById(resignation);
    }

    @Test
    @DisplayName("HR管理员可以代完成交接")
    void completeHandover_shouldSuccess_whenAdmin() {
      // Given
      Resignation resignation =
          Resignation.builder()
              .id(TEST_RESIGNATION_ID)
              .handoverPersonId(TEST_HANDOVER_PERSON_ID) // 其他用户
              .status(ResignationStatus.APPROVED)
              .handoverStatus(ResignationStatus.HANDOVER_PENDING)
              .build();

      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("ADMIN", "HR_MANAGER", "HR"))
          .thenReturn(true);

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);
      when(resignationRepository.updateById(any(Resignation.class))).thenReturn(true);

      // When
      ResignationDTO result = resignationAppService.completeHandover(TEST_RESIGNATION_ID, "HR代完成");

      // Then
      assertThat(result).isNotNull();
      assertThat(resignation.getHandoverStatus()).isEqualTo(ResignationStatus.HANDOVER_COMPLETED);
    }

    @Test
    @DisplayName("非交接人且非管理员无权完成交接")
    void completeHandover_shouldFail_whenNoPermission() {
      // Given
      Resignation resignation =
          Resignation.builder()
              .id(TEST_RESIGNATION_ID)
              .handoverPersonId(TEST_HANDOVER_PERSON_ID) // 其他用户
              .status(ResignationStatus.APPROVED)
              .build();

      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("ADMIN", "HR_MANAGER", "HR"))
          .thenReturn(false);

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> resignationAppService.completeHandover(TEST_RESIGNATION_ID, ""));
      assertThat(exception.getMessage()).contains("权限不足");
    }

    @Test
    @DisplayName("只有已审批通过的申请才能完成交接")
    void completeHandover_shouldFail_whenNotApproved() {
      // Given
      Resignation resignation =
          Resignation.builder().id(TEST_RESIGNATION_ID).status(ResignationStatus.PENDING).build();

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> resignationAppService.completeHandover(TEST_RESIGNATION_ID, ""));
      assertThat(exception.getMessage()).contains("只有已审批通过");
    }
  }

  @Nested
  @DisplayName("查询离职申请测试")
  class QueryResignationTests {

    @Test
    @DisplayName("应该成功分页查询离职申请")
    void listResignations_shouldSuccess() {
      // Given
      ResignationQueryDTO query = new ResignationQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Resignation resignation =
          Resignation.builder()
              .id(TEST_RESIGNATION_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .applicationNo("RZ2024001")
              .build();

      Page<Resignation> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(resignation));
      page.setTotal(1L);

      @SuppressWarnings("unchecked")
      Page<Resignation> pageParam = any(Page.class);
      when(resignationMapper.selectResignationPage(pageParam, any(), any())).thenReturn(page);
      when(employeeRepository.listByIds(anyList())).thenReturn(Collections.emptyList());
      when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

      // When
      PageResult<ResignationDTO> result = resignationAppService.listResignations(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getApplicationNo()).isEqualTo("RZ2024001");
    }

    @Test
    @DisplayName("应该成功获取离职申请详情")
    void getResignationById_shouldSuccess() {
      // Given
      Resignation resignation =
          Resignation.builder()
              .id(TEST_RESIGNATION_ID)
              .employeeId(TEST_EMPLOYEE_ID)
              .applicationNo("RZ2024001")
              .build();

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);
      when(employeeRepository.findById(TEST_EMPLOYEE_ID)).thenReturn(new Employee());
      when(userRepository.findById(any())).thenReturn(new User());

      // When
      ResignationDTO result = resignationAppService.getResignationById(TEST_RESIGNATION_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getApplicationNo()).isEqualTo("RZ2024001");
    }
  }

  @Nested
  @DisplayName("删除离职申请测试")
  class DeleteResignationTests {

    @Test
    @DisplayName("应该成功删除待审批状态的申请")
    void deleteResignation_shouldSuccess() {
      // Given
      Resignation resignation =
          Resignation.builder().id(TEST_RESIGNATION_ID).status(ResignationStatus.PENDING).build();

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);
      when(resignationRepository.softDelete(TEST_RESIGNATION_ID)).thenReturn(true);

      // When
      resignationAppService.deleteResignation(TEST_RESIGNATION_ID);

      // Then
      verify(resignationRepository).softDelete(TEST_RESIGNATION_ID);
    }

    @Test
    @DisplayName("非待审批状态不能删除")
    void deleteResignation_shouldFail_whenNotPending() {
      // Given
      Resignation resignation =
          Resignation.builder().id(TEST_RESIGNATION_ID).status(ResignationStatus.APPROVED).build();

      when(resignationRepository.getByIdOrThrow(eq(TEST_RESIGNATION_ID), anyString()))
          .thenReturn(resignation);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> resignationAppService.deleteResignation(TEST_RESIGNATION_ID));
      assertThat(exception.getMessage()).contains("只有待审批状态");
    }
  }
}
