package com.lawfirm.application.matter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.matter.command.CreateTaskCommentCommand;
import com.lawfirm.application.matter.dto.TaskCommentDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.entity.TaskComment;
import com.lawfirm.domain.matter.repository.TaskCommentRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** TaskCommentAppService 单元测试 测试任务评论服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskCommentAppService 任务评论服务测试")
class TaskCommentAppServiceTest {

  private static final Long TEST_COMMENT_ID = 100L;
  private static final Long TEST_TASK_ID = 200L;
  private static final Long TEST_USER_ID = 300L;
  private static final Long TEST_MENTIONED_USER_ID = 400L;

  @Mock private TaskCommentRepository taskCommentRepository;

  @Mock private TaskRepository taskRepository;

  @Mock private UserRepository userRepository;

  @Mock private NotificationAppService notificationAppService;

  @InjectMocks private TaskCommentAppService taskCommentAppService;

  @Nested
  @DisplayName("创建评论测试")
  class CreateCommentTests {

    @Test
    @DisplayName("应该成功创建评论")
    void createComment_shouldSuccess() {
      // Given
      CreateTaskCommentCommand command = new CreateTaskCommentCommand();
      command.setTaskId(TEST_TASK_ID);
      command.setContent("这是一条测试评论");
      command.setMentionedUserIds(null);

      Task task = Task.builder().id(TEST_TASK_ID).title("测试任务").build();

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(taskRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString())).thenReturn(task);
      when(userRepository.findById(TEST_USER_ID)).thenReturn(user);
      when(taskCommentRepository.save(any(TaskComment.class)))
          .thenAnswer(
              invocation -> {
                TaskComment comment = invocation.getArgument(0);
                comment.setId(TEST_COMMENT_ID);
                return true;
              });

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        TaskCommentDTO result = taskCommentAppService.createComment(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("这是一条测试评论");
        verify(taskCommentRepository).save(any(TaskComment.class));
      }
    }

    @Test
    @DisplayName("创建评论时应该发送@提醒通知")
    void createComment_shouldSendMentionNotification() {
      // Given
      CreateTaskCommentCommand command = new CreateTaskCommentCommand();
      command.setTaskId(TEST_TASK_ID);
      command.setContent("这是一条@提醒评论");
      command.setMentionedUserIds(List.of(TEST_MENTIONED_USER_ID));

      Task task = Task.builder().id(TEST_TASK_ID).title("测试任务").build();

      User currentUser = new User();
      currentUser.setId(TEST_USER_ID);
      currentUser.setRealName("当前用户");

      User mentionedUser = new User();
      mentionedUser.setId(TEST_MENTIONED_USER_ID);
      mentionedUser.setRealName("被@用户");

      when(taskRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString())).thenReturn(task);
      when(userRepository.findById(TEST_USER_ID)).thenReturn(currentUser);
      when(userRepository.findById(TEST_MENTIONED_USER_ID)).thenReturn(mentionedUser);
      when(taskCommentRepository.save(any(TaskComment.class)))
          .thenAnswer(
              invocation -> {
                TaskComment comment = invocation.getArgument(0);
                comment.setId(TEST_COMMENT_ID);
                return true;
              });
      lenient().doNothing().when(notificationAppService).sendNotification(any());

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        TaskCommentDTO result = taskCommentAppService.createComment(command);

        // Then
        assertThat(result).isNotNull();
        verify(notificationAppService).sendNotification(any());
      }
    }
  }

  @Nested
  @DisplayName("查询评论测试")
  class GetTaskCommentsTests {

    @Test
    @DisplayName("应该成功获取任务评论列表")
    void getTaskComments_shouldSuccess() {
      // Given
      Task task = Task.builder().id(TEST_TASK_ID).build();

      TaskComment comment1 =
          TaskComment.builder()
              .id(TEST_COMMENT_ID)
              .taskId(TEST_TASK_ID)
              .content("评论1")
              .createdBy(TEST_USER_ID)
              .build();

      TaskComment comment2 =
          TaskComment.builder()
              .id(200L)
              .taskId(TEST_TASK_ID)
              .content("评论2")
              .createdBy(TEST_USER_ID)
              .build();

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(taskRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString())).thenReturn(task);
      when(taskCommentRepository.findByTaskId(TEST_TASK_ID))
          .thenReturn(List.of(comment1, comment2));
      when(userRepository.findById(TEST_USER_ID)).thenReturn(user);

      // When
      List<TaskCommentDTO> result = taskCommentAppService.getTaskComments(TEST_TASK_ID);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getContent()).isEqualTo("评论1");
    }

    @Test
    @DisplayName("任务不存在应该抛出异常")
    void getTaskComments_shouldFail_whenTaskNotExists() {
      // Given
      when(taskRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString()))
          .thenThrow(new BusinessException("任务不存在"));

      // When & Then
      assertThrows(
          BusinessException.class, () -> taskCommentAppService.getTaskComments(TEST_TASK_ID));
    }
  }

  @Nested
  @DisplayName("删除评论测试")
  class DeleteCommentTests {

    @Test
    @DisplayName("应该成功删除自己的评论")
    void deleteComment_shouldSuccess() {
      // Given
      TaskComment comment =
          TaskComment.builder().id(TEST_COMMENT_ID).createdBy(TEST_USER_ID).build();

      when(taskCommentRepository.getByIdOrThrow(eq(TEST_COMMENT_ID), anyString()))
          .thenReturn(comment);
      lenient().when(taskCommentRepository.softDelete(TEST_COMMENT_ID)).thenReturn(true);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        taskCommentAppService.deleteComment(TEST_COMMENT_ID);

        // Then
        verify(taskCommentRepository).softDelete(TEST_COMMENT_ID);
      }
    }

    @Test
    @DisplayName("不能删除别人的评论")
    void deleteComment_shouldFail_whenNotOwner() {
      // Given
      TaskComment comment =
          TaskComment.builder()
              .id(TEST_COMMENT_ID)
              .createdBy(999L) // 其他用户
              .build();

      when(taskCommentRepository.getByIdOrThrow(eq(TEST_COMMENT_ID), anyString()))
          .thenReturn(comment);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When & Then
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> taskCommentAppService.deleteComment(TEST_COMMENT_ID));
        assertThat(exception.getMessage()).contains("只能删除自己的评论");
      }
    }
  }
}
