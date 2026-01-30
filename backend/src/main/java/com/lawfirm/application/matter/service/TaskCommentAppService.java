package com.lawfirm.application.matter.service;

import com.lawfirm.application.matter.command.CreateTaskCommentCommand;
import com.lawfirm.application.matter.dto.TaskCommentDTO;
import com.lawfirm.application.system.command.SendNotificationCommand;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.FileHashUtil;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.entity.TaskComment;
import com.lawfirm.domain.matter.repository.TaskCommentRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.system.entity.Notification;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 任务评论应用服务（M3-057~M3-059）. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCommentAppService {

  /** 任务评论仓储. */
  private final TaskCommentRepository taskCommentRepository;

  /** 任务仓储. */
  private final TaskRepository taskRepository;

  /** 用户仓储. */
  private final UserRepository userRepository;

  /** 通知应用服务. */
  private final NotificationAppService notificationAppService;

  /** MinIO服务. */
  private final MinioService minioService;

  /**
   * 创建任务评论（M3-057）.
   *
   * @param command 创建评论命令
   * @return 任务评论DTO
   */
  @Transactional
  public TaskCommentDTO createComment(final CreateTaskCommentCommand command) {
    // 验证任务存在
    Task task = taskRepository.getByIdOrThrow(command.getTaskId(), "任务不存在");

    // 创建评论
    // 转换attachments：List<String> -> List<Object>（JSONB格式）
    List<Object> attachments = null;
    if (command.getAttachments() != null && !command.getAttachments().isEmpty()) {
      attachments = new ArrayList<>(command.getAttachments());
    }

    TaskComment comment =
        TaskComment.builder()
            .taskId(command.getTaskId())
            .content(command.getContent())
            .attachments(attachments)
            .mentionedUserIds(command.getMentionedUserIds())
            .createdBy(SecurityUtils.getUserId())
            .build();

    taskCommentRepository.save(comment);

    log.info(
        "创建任务评论: taskId={}, commentId={}, userId={}",
        command.getTaskId(),
        comment.getId(),
        SecurityUtils.getUserId());

    // 发送@提醒通知（M3-059）
    sendMentionNotifications(command.getMentionedUserIds(), task, comment);

    return toDTO(comment);
  }

  /**
   * 获取任务的所有评论.
   *
   * @param taskId 任务ID
   * @return 任务评论DTO列表
   */
  public List<TaskCommentDTO> getTaskComments(final Long taskId) {
    // 验证任务存在
    taskRepository.getByIdOrThrow(taskId, "任务不存在");

    List<TaskComment> comments = taskCommentRepository.findByTaskId(taskId);
    return comments.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 删除评论.
   *
   * @param commentId 评论ID
   */
  @Transactional
  public void deleteComment(final Long commentId) {
    TaskComment comment = taskCommentRepository.getByIdOrThrow(commentId, "评论不存在");

    // 只能删除自己的评论
    if (!comment.getCreatedBy().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("只能删除自己的评论");
    }

    taskCommentRepository.softDelete(commentId);
    log.info("删除任务评论: commentId={}, userId={}", commentId, SecurityUtils.getUserId());
  }

  /**
   * 发送@提醒通知（M3-059）.
   *
   * @param mentionedUserIds 被@的用户ID列表
   * @param task 任务对象
   * @param comment 评论对象
   */
  private void sendMentionNotifications(
      final List<Long> mentionedUserIds, final Task task, final TaskComment comment) {
    if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
      return;
    }

    try {
      // 获取当前用户信息
      Long currentUserId = SecurityUtils.getUserId();
      String currentUserName = "系统";
      if (currentUserId != null) {
        var currentUser = userRepository.findById(currentUserId);
        if (currentUser != null) {
          currentUserName = currentUser.getRealName();
        }
      }

      // 构建通知标题和内容
      String title = "任务评论@提醒";
      String content = String.format("%s 在任务「%s」中评论提到了您", currentUserName, task.getTitle());

      // 构建通知命令
      SendNotificationCommand notificationCommand = new SendNotificationCommand();
      notificationCommand.setReceiverIds(mentionedUserIds);
      notificationCommand.setTitle(title);
      notificationCommand.setContent(content);
      notificationCommand.setType(Notification.TYPE_REMINDER);
      notificationCommand.setBusinessType("TASK_COMMENT");
      notificationCommand.setBusinessId(comment.getId());

      // 批量发送通知
      notificationAppService.sendNotification(notificationCommand);

      log.info(
          "发送@提醒通知成功: taskId={}, commentId={}, mentionedUserIds={}",
          task.getId(),
          comment.getId(),
          mentionedUserIds);

    } catch (Exception e) {
      // 通知发送失败不影响评论创建，只记录日志
      log.warn(
          "发送@提醒通知失败: taskId={}, commentId={}, error={}",
          task.getId(),
          comment.getId(),
          e.getMessage());
    }
  }

  /**
   * 转换为DTO.
   *
   * @param comment 评论实体
   * @return DTO
   */
  private TaskCommentDTO toDTO(final TaskComment comment) {
    TaskCommentDTO dto = new TaskCommentDTO();
    dto.setId(comment.getId());
    dto.setTaskId(comment.getTaskId());
    dto.setContent(comment.getContent());
    // 转换attachments：List<Object> -> List<String>（提取file_url或original_name）
    if (comment.getAttachments() != null && !comment.getAttachments().isEmpty()) {
      List<String> attachmentUrls = new ArrayList<>();
      for (Object attachment : comment.getAttachments()) {
        if (attachment instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> attMap = (Map<String, Object>) attachment;
          // 优先使用file_url，否则使用original_name
          String url = (String) attMap.get("file_url");
          if (url == null) {
            url = (String) attMap.get("original_name");
          }
          if (url != null) {
            attachmentUrls.add(url);
          }
        } else if (attachment instanceof String) {
          attachmentUrls.add((String) attachment);
        }
      }
      dto.setAttachments(attachmentUrls);
    }
    dto.setMentionedUserIds(comment.getMentionedUserIds());
    dto.setCreatedAt(comment.getCreatedAt());

    // 获取评论人姓名
    if (comment.getCreatedBy() != null) {
      var user = userRepository.findById(comment.getCreatedBy());
      if (user != null) {
        dto.setCreatorName(user.getRealName());
      }
    }

    // 获取@提醒的用户姓名列表
    if (comment.getMentionedUserIds() != null && !comment.getMentionedUserIds().isEmpty()) {
      List<String> mentionedNames =
          comment.getMentionedUserIds().stream()
              .map(
                  userId -> {
                    var user = userRepository.findById(userId);
                    return user != null ? user.getRealName() : null;
                  })
              .filter(name -> name != null)
              .collect(Collectors.toList());
      dto.setMentionedUserNames(mentionedNames);
    }

    return dto;
  }

  /**
   * 上传任务评论附件文件 上传文件到MinIO并返回标准化的JSONB格式附件信息.
   *
   * @param file 上传的文件
   * @param taskId 任务ID
   * @return 标准化的附件信息（JSONB格式）
   */
  @Transactional
  public Map<String, Object> uploadAttachmentFile(final MultipartFile file, final Long taskId) {
    // 验证任务存在
    Task task = taskRepository.getByIdOrThrow(taskId, "任务不存在");

    // 生成存储路径
    String storagePath =
        MinioPathGenerator.generateStandardPath(
            MinioPathGenerator.FileType.TASK, task.getMatterId(), "任务附件");

    // 生成物理文件名
    String originalFilename = file.getOriginalFilename();
    String physicalName = MinioPathGenerator.generatePhysicalName(originalFilename);
    String objectName = MinioPathGenerator.buildObjectName(storagePath, physicalName);

    try {
      // 计算文件Hash
      String fileHash = FileHashUtil.calculateHash(file);

      // 上传到MinIO
      String fileUrl =
          minioService.uploadFile(file.getInputStream(), objectName, file.getContentType());

      // 构建标准化的JSONB格式附件信息
      Map<String, Object> attachmentInfo = new HashMap<>();
      attachmentInfo.put("bucket_name", minioService.getBucketName());
      attachmentInfo.put("storage_path", storagePath);
      attachmentInfo.put("physical_name", physicalName);
      attachmentInfo.put("file_hash", fileHash);
      attachmentInfo.put("original_name", originalFilename);
      attachmentInfo.put("file_size", file.getSize());
      attachmentInfo.put("mime_type", file.getContentType());
      attachmentInfo.put("uploaded_at", Instant.now().toString());
      attachmentInfo.put("file_url", fileUrl); // 向后兼容字段

      log.info(
          "任务评论附件文件上传成功: taskId={}, fileName={}, storagePath={}, hash={}",
          taskId,
          originalFilename,
          storagePath,
          fileHash);

      return attachmentInfo;
    } catch (Exception e) {
      log.error("任务评论附件文件上传失败", e);
      throw new BusinessException("文件上传失败: " + e.getMessage());
    }
  }
}
