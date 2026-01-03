package com.lawfirm.application.matter.service;

import com.lawfirm.application.matter.command.CreateTaskCommentCommand;
import com.lawfirm.application.matter.dto.TaskCommentDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.entity.TaskComment;
import com.lawfirm.domain.matter.repository.TaskCommentRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务评论应用服务（M3-057~M3-059）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCommentAppService {

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    /**
     * 创建任务评论（M3-057）
     */
    @Transactional
    public TaskCommentDTO createComment(CreateTaskCommentCommand command) {
        // 验证任务存在
        Task task = taskRepository.getByIdOrThrow(command.getTaskId(), "任务不存在");

        // 创建评论
        TaskComment comment = TaskComment.builder()
                .taskId(command.getTaskId())
                .content(command.getContent())
                .attachments(command.getAttachments())
                .mentionedUserIds(command.getMentionedUserIds())
                .createdBy(SecurityUtils.getUserId())
                .build();

        taskCommentRepository.save(comment);

        log.info("创建任务评论: taskId={}, commentId={}, userId={}", 
                command.getTaskId(), comment.getId(), SecurityUtils.getUserId());

        // TODO: M3-059 - 发送@提醒通知（如果mentionedUserIds不为空）

        return toDTO(comment);
    }

    /**
     * 获取任务的所有评论
     */
    public List<TaskCommentDTO> getTaskComments(Long taskId) {
        // 验证任务存在
        taskRepository.getByIdOrThrow(taskId, "任务不存在");

        List<TaskComment> comments = taskCommentRepository.findByTaskId(taskId);
        return comments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除评论
     */
    @Transactional
    public void deleteComment(Long commentId) {
        TaskComment comment = taskCommentRepository.getByIdOrThrow(commentId, "评论不存在");

        // 只能删除自己的评论
        if (!comment.getCreatedBy().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能删除自己的评论");
        }

        taskCommentRepository.softDelete(commentId);
        log.info("删除任务评论: commentId={}, userId={}", commentId, SecurityUtils.getUserId());
    }

    /**
     * 转换为DTO
     */
    private TaskCommentDTO toDTO(TaskComment comment) {
        TaskCommentDTO dto = new TaskCommentDTO();
        dto.setId(comment.getId());
        dto.setTaskId(comment.getTaskId());
        dto.setContent(comment.getContent());
        dto.setAttachments(comment.getAttachments());
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
            List<String> mentionedNames = comment.getMentionedUserIds().stream()
                    .map(userId -> {
                        var user = userRepository.findById(userId);
                        return user != null ? user.getRealName() : null;
                    })
                    .filter(name -> name != null)
                    .collect(Collectors.toList());
            dto.setMentionedUserNames(mentionedNames);
        }

        return dto;
    }
}

