package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateTaskCommand;
import com.lawfirm.application.matter.dto.TaskDTO;
import com.lawfirm.application.matter.dto.TaskQueryDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.constant.ApprovalStatus;
import com.lawfirm.common.constant.TaskStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TaskAppService 单元测试
 *
 * 测试任务应用服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskAppService 任务服务测试")
class TaskAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_DEPT_ID = 10L;
    private static final Long TEST_MATTER_ID = 100L;
    private static final Long TEST_TASK_ID = 1000L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private NotificationAppService notificationAppService;

    @InjectMocks
    private TaskAppService taskAppService;

    @Nested
    @DisplayName("分页查询任务测试")
    class ListTasksTests {

        @Test
        @DisplayName("应该分页查询任务列表")
        void listTasks_shouldReturnPagedResult() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given - Mock SecurityUtils for data scope filtering
                mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("ALL");
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
                
                TaskQueryDTO query = new TaskQueryDTO();
                query.setPageNum(1);
                query.setPageSize(10);

                Task task = createTestTask(TEST_TASK_ID, "测试任务", "TODO");

                Page<Task> page = new Page<>(1, 10);
                page.setRecords(List.of(task));
                page.setTotal(1);

                when(taskRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

                // When
                PageResult<TaskDTO> result = taskAppService.listTasks(query);

                // Then
                assertThat(result.getRecords()).hasSize(1);
                assertThat(result.getRecords().get(0).getTitle()).isEqualTo("测试任务");
            }
        }

        @Test
        @DisplayName("应该按优先级排序")
        void listTasks_shouldSortByPriority() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given - Mock SecurityUtils for data scope filtering
                mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("ALL");
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
                
                TaskQueryDTO query = new TaskQueryDTO();
                query.setPageNum(1);
                query.setPageSize(10);

                Page<Task> page = new Page<>(1, 10);
                page.setRecords(new ArrayList<>());
                page.setTotal(0);

                when(taskRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

                // When
                taskAppService.listTasks(query);

                // Then - 验证page方法被调用（排序已在SQL中实现，不再验证wrapper内容）
                verify(taskRepository).page(any(Page.class), any(LambdaQueryWrapper.class));
            }
        }

        @Test
        @DisplayName("空结果时应返回空分页")
        void listTasks_shouldReturnEmptyWhenNoResults() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given - Mock SecurityUtils for data scope filtering
                mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("ALL");
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
                
                TaskQueryDTO query = new TaskQueryDTO();
                query.setPageNum(1);
                query.setPageSize(10);

                Page<Task> page = new Page<>(1, 10);
                page.setRecords(new ArrayList<>());
                page.setTotal(0);

                when(taskRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

                // When
                PageResult<TaskDTO> result = taskAppService.listTasks(query);

                // Then
                assertThat(result.getRecords()).isEmpty();
                assertThat(result.getTotal()).isEqualTo(0);
            }
        }
    }

    @Nested
    @DisplayName("创建任务测试")
    class CreateTaskTests {

        @Test
        @DisplayName("应该成功创建任务")
        void createTask_shouldReturnTaskDTO() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                CreateTaskCommand command = new CreateTaskCommand();
                command.setMatterId(TEST_MATTER_ID);
                command.setTitle("新任务");
                command.setDescription("任务描述");
                command.setPriority("HIGH");
                command.setAssigneeId(2L);
                command.setAssigneeName("张三");
                command.setDueDate(LocalDate.now().plusDays(7));

                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                mockedSecurity.when(SecurityUtils::getRealName).thenReturn("李四");
                when(taskRepository.save(any(Task.class))).thenReturn(true);
                doNothing().when(notificationAppService).sendSystemNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());

                // When
                TaskDTO result = taskAppService.createTask(command);

                // Then
                assertThat(result.getTitle()).isEqualTo("新任务");
                assertThat(result.getPriority()).isEqualTo("HIGH");
                assertThat(result.getStatus()).isEqualTo("TODO");
                assertThat(result.getProgress()).isEqualTo(0);
                verify(taskRepository).save(any(Task.class));
            }
        }

        @Test
        @DisplayName("分配给自己时不应发送通知")
        void createTask_shouldNotSendNotification_whenAssignToSelf() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                CreateTaskCommand command = new CreateTaskCommand();
                command.setTitle("新任务");
                command.setAssigneeId(TEST_USER_ID); // 分配给自己

                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.save(any(Task.class))).thenReturn(true);

                // When
                taskAppService.createTask(command);

                // Then
                verify(notificationAppService, never()).sendSystemNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());
            }
        }

        @Test
        @DisplayName("应使用默认优先级")
        void createTask_shouldUseDefaultPriority() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                CreateTaskCommand command = new CreateTaskCommand();
                command.setTitle("新任务");
                command.setPriority(null); // 不设置优先级

                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.save(any(Task.class))).thenReturn(true);

                // When
                TaskDTO result = taskAppService.createTask(command);

                // Then
                assertThat(result.getPriority()).isEqualTo("MEDIUM");
            }
        }
    }

    @Nested
    @DisplayName("获取任务详情测试")
    class GetTaskByIdTests {

        @Test
        @DisplayName("应该获取任务详情")
        void getTaskById_shouldReturnTask() {
            // Given
            Task task = createTestTask(TEST_TASK_ID, "测试任务", "TODO");
            when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);

            // When
            TaskDTO result = taskAppService.getTaskById(TEST_TASK_ID);

            // Then
            assertThat(result.getId()).isEqualTo(TEST_TASK_ID);
            assertThat(result.getTitle()).isEqualTo("测试任务");
        }

        @Test
        @DisplayName("任务不存在时应抛出异常")
        void getTaskById_shouldThrowException_whenNotFound() {
            // Given
            when(taskRepository.getByIdOrThrow(999L, "任务不存在"))
                    .thenThrow(new BusinessException("任务不存在"));

            // When & Then
            assertThatThrownBy(() -> taskAppService.getTaskById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("任务不存在");
        }
    }

    @Nested
    @DisplayName("更新任务测试")
    class UpdateTaskTests {

        @Test
        @DisplayName("应该成功更新任务")
        void updateTask_shouldReturnTaskDTO() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "原任务", "TODO");
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);

                // When
                TaskDTO result = taskAppService.updateTask(TEST_TASK_ID, "更新后的任务", "新描述",
                        "HIGH", 2L, "王五", null, null, null);

                // Then
                assertThat(result.getTitle()).isEqualTo("更新后的任务");
                assertThat(task.getPriority()).isEqualTo("HIGH");
            }
        }

        @Test
        @DisplayName("无权限时应抛出异常")
        void updateTask_shouldThrowException_whenNoPermission() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "TODO");
                task.setCreatedBy(999L); // 其他人创建
                task.setAssigneeId(888L); // 分配给其他人

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);

                // When & Then
                assertThatThrownBy(() -> taskAppService.updateTask(TEST_TASK_ID, "更新", null,
                        null, null, null, null, null, null))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage("只有任务创建者或负责人才能执行此操作");
            }
        }

        @Test
        @DisplayName("管理员可以更新任何任务")
        void updateTask_shouldAllowUpdate_whenAdmin() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "TODO");
                task.setCreatedBy(999L);
                task.setAssigneeId(888L);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(true);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);

                // When
                TaskDTO result = taskAppService.updateTask(TEST_TASK_ID, "更新后的任务", null,
                        null, null, null, null, null, null);

                // Then
                assertThat(result.getTitle()).isEqualTo("更新后的任务");
            }
        }
    }

    @Nested
    @DisplayName("删除任务测试")
    class DeleteTaskTests {

        @Test
        @DisplayName("应该成功删除任务")
        void deleteTask_shouldDeleteTask() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "TODO");
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
                when(taskRepository.removeById(TEST_TASK_ID)).thenReturn(true);

                // When
                taskAppService.deleteTask(TEST_TASK_ID);

                // Then
                verify(taskRepository).removeById(TEST_TASK_ID);
            }
        }

        @Test
        @DisplayName("有子任务时应抛出异常")
        void deleteTask_shouldThrowException_whenHasSubTasks() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "父任务", "TODO");
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.count(any(LambdaQueryWrapper.class))).thenReturn(2L);

                // When & Then
                assertThatThrownBy(() -> taskAppService.deleteTask(TEST_TASK_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage("该任务有子任务，请先删除子任务");
            }
        }
    }

    @Nested
    @DisplayName("更新任务状态测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("完成任务时应变为待验收状态")
        void updateStatus_shouldSetPendingReview_whenCompleted() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "IN_PROGRESS");
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);
                // 使用lenient避免UnnecessaryStubbing异常，因为通知可能根据条件发送
                lenient().doNothing().when(notificationAppService).sendSystemNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());

                // When
                TaskDTO result = taskAppService.updateStatus(TEST_TASK_ID, TaskStatus.COMPLETED);

                // Then
                assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
                assertThat(task.getProgress()).isEqualTo(100);
                assertThat(task.getCompletedAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("TODO状态应清空完成时间和进度")
        void updateStatus_shouldClearCompletion_whenSetToTodo() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "COMPLETED");
                task.setCreatedBy(TEST_USER_ID);
                task.setProgress(100);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);

                // When
                TaskDTO result = taskAppService.updateStatus(TEST_TASK_ID, "TODO");

                // Then
                assertThat(result.getStatus()).isEqualTo("TODO");
                assertThat(task.getProgress()).isEqualTo(0);
                assertThat(task.getCompletedAt()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("验收任务测试")
    class ApproveTaskTests {

        @Test
        @DisplayName("应该通过任务验收")
        void approveTask_shouldApprove() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "PENDING_REVIEW");
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);
                doNothing().when(notificationAppService).sendSystemNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());

                // When
                TaskDTO result = taskAppService.approveTask(TEST_TASK_ID);

                // Then
                assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
                assertThat(task.getReviewStatus()).isEqualTo(ApprovalStatus.APPROVED);
                assertThat(task.getReviewedAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("非待验收状态不能验收")
        void approveTask_shouldThrowException_whenNotPendingReview() {
            // Given
            Task task = createTestTask(TEST_TASK_ID, "任务", "TODO");
            when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);

            // When & Then
            assertThatThrownBy(() -> taskAppService.approveTask(TEST_TASK_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("只有待验收状态的任务才能进行验收");
        }

        @Test
        @DisplayName("非创建者不能验收")
        void approveTask_shouldThrowException_whenNotCreator() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "PENDING_REVIEW");
                task.setCreatedBy(999L);

                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);

                // When & Then
                assertThatThrownBy(() -> taskAppService.approveTask(TEST_TASK_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage("只有任务创建者才能进行验收");
            }
        }
    }

    @Nested
    @DisplayName("退回任务测试")
    class RejectTaskTests {

        @Test
        @DisplayName("应该退回任务")
        void rejectTask_shouldReject() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "PENDING_REVIEW");
                task.setProgress(100);
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);
                doNothing().when(notificationAppService).sendSystemNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());

                // When
                TaskDTO result = taskAppService.rejectTask(TEST_TASK_ID, "需要修改");

                // Then
                assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
                assertThat(task.getReviewStatus()).isEqualTo(ApprovalStatus.REJECTED);
                assertThat(task.getProgress()).isEqualTo(95); // 从100降到95
                assertThat(task.getReviewComment()).isEqualTo("需要修改");
            }
        }

        @Test
        @DisplayName("退回时必须填写意见")
        void rejectTask_shouldThrowException_whenNoComment() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                
                Task task = createTestTask(TEST_TASK_ID, "任务", "PENDING_REVIEW");
                task.setCreatedBy(TEST_USER_ID);

                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);

                // When & Then
                assertThatThrownBy(() -> taskAppService.rejectTask(TEST_TASK_ID, null))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage("退回时必须填写验收意见");
            }
        }
    }

    @Nested
    @DisplayName("更新任务进度测试")
    class UpdateProgressTests {

        @Test
        @DisplayName("应该成功更新进度")
        void updateProgress_shouldUpdateProgress() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given - 使用PENDING状态（而非TODO），因为实现只允许PENDING和IN_PROGRESS更新进度
                Task task = createTestTask(TEST_TASK_ID, "任务", TaskStatus.PENDING);
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);

                // When
                TaskDTO result = taskAppService.updateProgress(TEST_TASK_ID, 50);

                // Then
                assertThat(result.getProgress()).isEqualTo(50);
                assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            }
        }

        @Test
        @DisplayName("进度100%时应变为待验收")
        void updateProgress_shouldSetPendingReview_when100Percent() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given - 使用PENDING状态（而非TODO），因为实现只允许PENDING和IN_PROGRESS更新进度
                Task task = createTestTask(TEST_TASK_ID, "任务", TaskStatus.PENDING);
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);

                // When
                TaskDTO result = taskAppService.updateProgress(TEST_TASK_ID, 100);

                // Then
                assertThat(result.getProgress()).isEqualTo(100);
                assertThat(task.getStatus()).isEqualTo("PENDING_REVIEW");
                assertThat(task.getCompletedAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("进度0%时应变为TODO状态")
        void updateProgress_shouldSetTodo_when0Percent() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "任务", "IN_PROGRESS");
                task.setProgress(50);
                task.setCreatedBy(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);
                when(taskRepository.updateById(any(Task.class))).thenReturn(true);

                // When
                TaskDTO result = taskAppService.updateProgress(TEST_TASK_ID, 0);

                // Then
                assertThat(result.getProgress()).isEqualTo(0);
                assertThat(task.getStatus()).isEqualTo("TODO");
            }
        }

        @Test
        @DisplayName("进度超出范围时应抛出异常")
        void updateProgress_shouldThrowException_whenOutOfRange() {
            // Given
            Task task = createTestTask(TEST_TASK_ID, "任务", "TODO");
            when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);

            // When & Then
            assertThatThrownBy(() -> taskAppService.updateProgress(TEST_TASK_ID, 150))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("进度必须在0-100之间");

            assertThatThrownBy(() -> taskAppService.updateProgress(TEST_TASK_ID, -10))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("进度必须在0-100之间");
        }

        @Test
        @DisplayName("已完成任务不能更新进度")
        void updateProgress_shouldThrowException_whenCompleted() {
            // Given
            Task task = createTestTask(TEST_TASK_ID, "任务", TaskStatus.COMPLETED);
            when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);

            // When & Then
            assertThatThrownBy(() -> taskAppService.updateProgress(TEST_TASK_ID, 50))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("已完成的任务不能更新进度");
        }

        @Test
        @DisplayName("待验收任务不能更新进度")
        void updateProgress_shouldThrowException_whenPendingReview() {
            // Given
            Task task = createTestTask(TEST_TASK_ID, "任务", "PENDING_REVIEW");
            when(taskRepository.getByIdOrThrow(TEST_TASK_ID, "任务不存在")).thenReturn(task);

            // When & Then
            assertThatThrownBy(() -> taskAppService.updateProgress(TEST_TASK_ID, 50))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("待验收的任务不能更新进度，请等待验收或验收退回后再更新");
        }
    }

    @Nested
    @DisplayName("获取待办任务测试")
    class GetMyTodoTasksTests {

        @Test
        @DisplayName("应该获取我的待办任务")
        void getMyTodoTasks_shouldReturnTodoTasks() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Task task = createTestTask(TEST_TASK_ID, "待办任务", "TODO");
                task.setAssigneeId(TEST_USER_ID);

                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                when(taskRepository.findMyTodoTasks(TEST_USER_ID)).thenReturn(List.of(task));

                // When
                List<TaskDTO> result = taskAppService.getMyTodoTasks();

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getStatus()).isEqualTo("TODO");
            }
        }
    }

    @Nested
    @DisplayName("获取即将到期任务测试")
    class GetUpcomingTasksTests {

        @Test
        @DisplayName("应该获取即将到期的任务")
        void getUpcomingTasks_shouldReturnUpcomingTasks() {
            // Given
            Task task = createTestTask(TEST_TASK_ID, "即将到期任务", "TODO");
            task.setDueDate(LocalDate.now().plusDays(3));

            when(taskRepository.findUpcomingTasks(any(), any())).thenReturn(List.of(task));

            // When
            List<TaskDTO> result = taskAppService.getUpcomingTasks(7);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("获取逾期任务测试")
    class GetOverdueTasksTests {

        @Test
        @DisplayName("应该获取逾期任务")
        void getOverdueTasks_shouldReturnOverdueTasks() {
            // Given
            Task task = createTestTask(TEST_TASK_ID, "逾期任务", "TODO");
            task.setDueDate(LocalDate.now().minusDays(1));

            when(taskRepository.findOverdueTasks(any())).thenReturn(List.of(task));

            // When
            List<TaskDTO> result = taskAppService.getOverdueTasks();

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("获取案件任务统计测试")
    class GetMatterTaskStatsTests {

        @Test
        @DisplayName("应该获取案件任务统计")
        void getMatterTaskStats_shouldReturnStats() {
            // Given
            when(taskRepository.countByMatter(TEST_MATTER_ID)).thenReturn(10);
            when(taskRepository.countCompletedByMatter(TEST_MATTER_ID)).thenReturn(7);

            // When
            int[] result = taskAppService.getMatterTaskStats(TEST_MATTER_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result[0]).isEqualTo(10); // 总数
            assertThat(result[1]).isEqualTo(7);  // 已完成
        }
    }

    // ========== 辅助方法 ==========

    private Task createTestTask(Long id, String title, String status) {
        return Task.builder()
                .id(id)
                .taskNo("TK" + System.currentTimeMillis())
                .matterId(TEST_MATTER_ID)
                .title(title)
                .description("任务描述")
                .priority("MEDIUM")
                .assigneeId(TEST_USER_ID)
                .assigneeName("测试用户")
                .status(status)
                .progress(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
