package com.lawfirm.application.workbench.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.workbench.dto.WorkbenchDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.*;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.Schedule;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.repository.ScheduleRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.NotificationMapper;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** WorkbenchAppService 单元测试 测试工作台应用服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkbenchAppService 工作台服务测试")
class WorkbenchAppServiceTest {

  private static final Long TEST_USER_ID = 1L;

  @Mock private ApprovalRepository approvalRepository;

  @Mock private TaskRepository taskRepository;

  @Mock private ScheduleRepository scheduleRepository;

  @Mock private MatterMapper matterMapper;

  @Mock private TimesheetMapper timesheetMapper;

  @Mock private NotificationMapper notificationMapper;

  @InjectMocks private WorkbenchAppService workbenchAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("获取工作台数据测试")
  class GetWorkbenchDataTests {

    @Test
    @DisplayName("应该成功获取完整工作台数据")
    void getWorkbenchData_shouldReturnCompleteData() {
      // Given
      lenient().when(approvalRepository.countPendingByApproverId(TEST_USER_ID)).thenReturn(5);
      lenient().when(taskRepository.countPendingByAssigneeId(TEST_USER_ID)).thenReturn(3);
      lenient().when(taskRepository.countOverdueByAssigneeId(TEST_USER_ID)).thenReturn(2);
      lenient()
          .when(matterMapper.countByUserIdAndStatus(TEST_USER_ID, "IN_PROGRESS"))
          .thenReturn(10);
      lenient()
          .when(matterMapper.countByUserIdAndStatus(TEST_USER_ID, "PENDING_APPROVAL"))
          .thenReturn(2);
      lenient().when(matterMapper.countByUserIdAndStatus(TEST_USER_ID, "CLOSED")).thenReturn(5);
      lenient()
          .when(
              timesheetMapper.sumHoursByUserIdAndDateRange(
                  anyLong(), any(LocalDate.class), any(LocalDate.class)))
          .thenReturn(BigDecimal.valueOf(160));
      lenient()
          .when(
              timesheetMapper.sumBillableHoursByUserIdAndDateRange(
                  anyLong(), any(LocalDate.class), any(LocalDate.class)))
          .thenReturn(BigDecimal.valueOf(120));
      lenient()
          .when(
              timesheetMapper.countByUserIdAndDateRange(
                  anyLong(), any(LocalDate.class), any(LocalDate.class)))
          .thenReturn(20);
      lenient()
          .when(approvalRepository.findPendingByApproverId(TEST_USER_ID, 5))
          .thenReturn(Collections.emptyList());
      lenient()
          .when(taskRepository.findPendingByAssigneeId(TEST_USER_ID, 5))
          .thenReturn(Collections.emptyList());
      lenient()
          .when(scheduleRepository.findByUserAndDate(TEST_USER_ID, LocalDate.now()))
          .thenReturn(Collections.emptyList());
      lenient()
          .when(matterMapper.selectRecentByUserId(TEST_USER_ID, 5))
          .thenReturn(Collections.emptyList());
      lenient().when(notificationMapper.countUnread(TEST_USER_ID)).thenReturn(3);

      // When
      WorkbenchDTO result = workbenchAppService.getWorkbenchData();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTodoSummary()).isNotNull();
      assertThat(result.getProjectSummary()).isNotNull();
      assertThat(result.getTimesheetSummary()).isNotNull();
      assertThat(result.getTodoItems()).isNotNull();
      assertThat(result.getTodaySchedules()).isNotNull();
      assertThat(result.getRecentProjects()).isNotNull();
    }

    @Test
    @DisplayName("部分模块失败时应返回默认值")
    void getWorkbenchData_shouldReturnDefaults_whenPartialFailure() {
      // Given
      when(approvalRepository.countPendingByApproverId(TEST_USER_ID))
          .thenThrow(new RuntimeException("Database error"));
      when(matterMapper.countByUserIdAndStatus(TEST_USER_ID, "IN_PROGRESS")).thenReturn(10);
      when(timesheetMapper.sumHoursByUserIdAndDateRange(anyLong(), any(), any()))
          .thenReturn(BigDecimal.valueOf(160));
      when(approvalRepository.findPendingByApproverId(TEST_USER_ID, 5))
          .thenReturn(Collections.emptyList());
      when(taskRepository.findPendingByAssigneeId(TEST_USER_ID, 5))
          .thenReturn(Collections.emptyList());
      when(scheduleRepository.findByUserAndDate(TEST_USER_ID, LocalDate.now()))
          .thenReturn(Collections.emptyList());
      when(matterMapper.selectRecentByUserId(TEST_USER_ID, 5)).thenReturn(Collections.emptyList());

      // When
      WorkbenchDTO result = workbenchAppService.getWorkbenchData();

      // Then
      assertThat(result).isNotNull();
      // 待办统计应该返回默认值（0）
      assertThat(result.getTodoSummary().getPendingApproval()).isEqualTo(0);
      assertThat(result.getTodoSummary().getTotal()).isEqualTo(0);
      // 其他模块应该正常返回
      assertThat(result.getProjectSummary()).isNotNull();
    }
  }

  @Nested
  @DisplayName("获取待办事项统计测试")
  class GetTodoSummaryTests {

    @Test
    @DisplayName("应该正确计算待办统计")
    void getTodoSummary_shouldCalculateCorrectly() {
      // Given
      when(approvalRepository.countPendingByApproverId(TEST_USER_ID)).thenReturn(5);
      when(taskRepository.countPendingByAssigneeId(TEST_USER_ID)).thenReturn(3);
      when(taskRepository.countOverdueByAssigneeId(TEST_USER_ID)).thenReturn(2);

      // When
      TodoSummary result = workbenchAppService.getTodoSummary(TEST_USER_ID);

      // Then
      assertThat(result.getPendingApproval()).isEqualTo(5);
      assertThat(result.getPendingTask()).isEqualTo(3);
      assertThat(result.getOverdueTask()).isEqualTo(2);
      assertThat(result.getTotal()).isEqualTo(8); // 5 + 3
    }

    @Test
    @DisplayName("异常时应返回空统计")
    void getTodoSummary_shouldReturnEmpty_whenException() {
      // Given
      when(approvalRepository.countPendingByApproverId(TEST_USER_ID))
          .thenThrow(new RuntimeException("Database error"));

      // When
      TodoSummary result = workbenchAppService.getTodoSummary(TEST_USER_ID);

      // Then
      assertThat(result.getPendingApproval()).isEqualTo(0);
      assertThat(result.getPendingTask()).isEqualTo(0);
      assertThat(result.getOverdueTask()).isEqualTo(0);
      assertThat(result.getTotal()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("获取项目统计测试")
  class GetProjectSummaryTests {

    @Test
    @DisplayName("应该正确计算项目统计")
    void getProjectSummary_shouldCalculateCorrectly() {
      // Given
      when(matterMapper.countByUserIdAndStatus(TEST_USER_ID, "IN_PROGRESS")).thenReturn(10);
      when(matterMapper.countByUserIdAndStatus(TEST_USER_ID, "PENDING_APPROVAL")).thenReturn(2);
      when(matterMapper.countByUserIdAndStatus(TEST_USER_ID, "CLOSED")).thenReturn(5);

      // When
      ProjectSummary result = workbenchAppService.getProjectSummary(TEST_USER_ID);

      // Then
      assertThat(result.getInProgress()).isEqualTo(10);
      assertThat(result.getPendingApproval()).isEqualTo(2);
      assertThat(result.getClosed()).isEqualTo(5);
      assertThat(result.getTotal()).isEqualTo(17); // 10 + 2 + 5
    }
  }

  @Nested
  @DisplayName("获取工时统计测试")
  class GetTimesheetSummaryTests {

    @Test
    @DisplayName("应该正确计算工时统计")
    void getTimesheetSummary_shouldCalculateCorrectly() {
      // Given
      LocalDate now = LocalDate.now();
      LocalDate startOfMonth = now.withDayOfMonth(1);
      LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

      when(timesheetMapper.sumHoursByUserIdAndDateRange(
              eq(TEST_USER_ID), eq(startOfMonth), eq(endOfMonth)))
          .thenReturn(BigDecimal.valueOf(160));
      when(timesheetMapper.sumBillableHoursByUserIdAndDateRange(
              eq(TEST_USER_ID), eq(startOfMonth), eq(endOfMonth)))
          .thenReturn(BigDecimal.valueOf(120));
      when(timesheetMapper.countByUserIdAndDateRange(
              eq(TEST_USER_ID), eq(startOfMonth), eq(endOfMonth)))
          .thenReturn(20);

      // When
      TimesheetSummary result = workbenchAppService.getTimesheetSummary(TEST_USER_ID);

      // Then
      assertThat(result.getTotalHours()).isEqualByComparingTo(BigDecimal.valueOf(160));
      assertThat(result.getBillableHours()).isEqualByComparingTo(BigDecimal.valueOf(120));
      assertThat(result.getRecordCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("null值时应返回零")
    void getTimesheetSummary_shouldReturnZero_whenNull() {
      // Given
      when(timesheetMapper.sumHoursByUserIdAndDateRange(anyLong(), any(), any())).thenReturn(null);
      when(timesheetMapper.sumBillableHoursByUserIdAndDateRange(anyLong(), any(), any()))
          .thenReturn(null);
      when(timesheetMapper.countByUserIdAndDateRange(anyLong(), any(), any())).thenReturn(0);

      // When
      TimesheetSummary result = workbenchAppService.getTimesheetSummary(TEST_USER_ID);

      // Then
      assertThat(result.getTotalHours()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.getBillableHours()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.getRecordCount()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("获取待办事项列表测试")
  class GetTodoItemsTests {

    @Test
    @DisplayName("应该返回待办事项列表")
    void getTodoItems_shouldReturnTodoItems() {
      // Given
      Approval approval = new Approval();
      approval.setId(1L);
      approval.setBusinessTitle("审批事项1");
      approval.setBusinessType("CONTRACT");
      approval.setBusinessId(100L);
      approval.setCreatedAt(LocalDateTime.now());

      Task task = new Task();
      task.setId(2L);
      task.setTitle("任务1");
      task.setDescription("任务描述");
      task.setPriority("HIGH");
      task.setStatus("PENDING");
      task.setDueDate(LocalDate.now());
      task.setMatterId(200L);

      when(approvalRepository.findPendingByApproverId(TEST_USER_ID, 5))
          .thenReturn(List.of(approval));
      when(taskRepository.findPendingByAssigneeId(TEST_USER_ID, 5)).thenReturn(List.of(task));

      // When
      List<TodoItemDTO> result = workbenchAppService.getTodoItems(TEST_USER_ID);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getType()).isEqualTo("APPROVAL");
      assertThat(result.get(1).getType()).isEqualTo("TASK");
    }

    @Test
    @DisplayName("无待办事项时应返回空列表")
    void getTodoItems_shouldReturnEmpty_whenNoItems() {
      // Given
      when(approvalRepository.findPendingByApproverId(TEST_USER_ID, 5))
          .thenReturn(Collections.emptyList());
      when(taskRepository.findPendingByAssigneeId(TEST_USER_ID, 5))
          .thenReturn(Collections.emptyList());

      // When
      List<TodoItemDTO> result = workbenchAppService.getTodoItems(TEST_USER_ID);

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("获取今日日程测试")
  class GetTodaySchedulesTests {

    @Test
    @DisplayName("应该返回今日日程列表")
    void getTodaySchedules_shouldReturnSchedules() {
      // Given
      Schedule schedule = new Schedule();
      schedule.setId(1L);
      schedule.setTitle("会议");
      LocalDate today = LocalDate.now();
      schedule.setStartTime(LocalDateTime.of(today, LocalTime.of(9, 0)));
      schedule.setEndTime(LocalDateTime.of(today, LocalTime.of(10, 0)));
      schedule.setScheduleType("MEETING");
      schedule.setLocation("会议室A");
      schedule.setMatterId(100L);

      when(scheduleRepository.findByUserAndDate(TEST_USER_ID, LocalDate.now()))
          .thenReturn(List.of(schedule));

      // When
      List<ScheduleItemDTO> result = workbenchAppService.getTodaySchedules(TEST_USER_ID);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("会议");
      assertThat(result.get(0).getStartTime()).isEqualTo("09:00");
      assertThat(result.get(0).getEndTime()).isEqualTo("10:00");
    }

    @Test
    @DisplayName("无日程时应返回空列表")
    void getTodaySchedules_shouldReturnEmpty_whenNoSchedules() {
      // Given
      when(scheduleRepository.findByUserAndDate(TEST_USER_ID, LocalDate.now()))
          .thenReturn(Collections.emptyList());

      // When
      List<ScheduleItemDTO> result = workbenchAppService.getTodaySchedules(TEST_USER_ID);

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("获取最近项目测试")
  class GetRecentProjectsTests {

    @Test
    @DisplayName("应该返回最近项目列表")
    void getRecentProjects_shouldReturnProjects() {
      // Given
      Matter matter = new Matter();
      matter.setId(1L);
      matter.setMatterNo("M2024001");
      matter.setName("项目1");
      matter.setStatus("IN_PROGRESS");
      matter.setUpdatedAt(LocalDateTime.now());

      when(matterMapper.selectRecentByUserId(TEST_USER_ID, 5)).thenReturn(List.of(matter));

      // When
      List<RecentProjectDTO> result = workbenchAppService.getRecentProjects(TEST_USER_ID);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getMatterNo()).isEqualTo("M2024001");
      assertThat(result.get(0).getMatterName()).isEqualTo("项目1");
    }

    @Test
    @DisplayName("无项目时应返回空列表")
    void getRecentProjects_shouldReturnEmpty_whenNoProjects() {
      // Given
      when(matterMapper.selectRecentByUserId(TEST_USER_ID, 5)).thenReturn(Collections.emptyList());

      // When
      List<RecentProjectDTO> result = workbenchAppService.getRecentProjects(TEST_USER_ID);

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("获取未读消息数量测试")
  class GetUnreadNotificationCountTests {

    @Test
    @DisplayName("应该返回未读消息数量")
    void getUnreadNotificationCount_shouldReturnCount() {
      // Given
      when(notificationMapper.countUnread(TEST_USER_ID)).thenReturn(5);

      // When
      int result = workbenchAppService.getUnreadNotificationCount(TEST_USER_ID);

      // Then
      assertThat(result).isEqualTo(5);
    }

    @Test
    @DisplayName("异常时应返回0")
    void getUnreadNotificationCount_shouldReturnZero_whenException() {
      // Given
      when(notificationMapper.countUnread(TEST_USER_ID))
          .thenThrow(new RuntimeException("Database error"));

      // When
      int result = workbenchAppService.getUnreadNotificationCount(TEST_USER_ID);

      // Then
      assertThat(result).isEqualTo(0);
    }
  }
}
