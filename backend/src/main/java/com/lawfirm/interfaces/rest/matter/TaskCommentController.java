package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateTaskCommentCommand;
import com.lawfirm.application.matter.dto.TaskCommentDTO;
import com.lawfirm.application.matter.service.TaskCommentAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 任务评论管理接口（M3-057~M3-059） */
@Tag(name = "任务协作", description = "任务评论、附件、@提醒等协作功能")
@RestController
@RequestMapping("/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

  /** 任务评论应用服务 */
  private final TaskCommentAppService taskCommentAppService;

  /**
   * 获取任务的所有评论（M3-057）
   *
   * @param taskId 任务ID
   * @return 任务评论列表
   */
  @GetMapping
  @RequirePermission("task:view")
  @Operation(summary = "获取任务评论列表", description = "获取指定任务的所有评论")
  public Result<List<TaskCommentDTO>> getTaskComments(@PathVariable final Long taskId) {
    List<TaskCommentDTO> comments = taskCommentAppService.getTaskComments(taskId);
    return Result.success(comments);
  }

  /**
   * 创建任务评论（M3-057：评论功能，M3-058：附件功能，M3-059：@提醒功能）
   *
   * @param taskId 任务ID
   * @param command 创建任务评论命令
   * @return 任务评论信息
   */
  @PostMapping
  @RequirePermission("task:comment")
  @Operation(summary = "创建任务评论", description = "创建任务评论，支持附件和@提醒")
  @OperationLog(module = "任务协作", action = "创建任务评论")
  public Result<TaskCommentDTO> createComment(
      @PathVariable final Long taskId, @RequestBody @Valid final CreateTaskCommentCommand command) {
    command.setTaskId(taskId);
    TaskCommentDTO comment = taskCommentAppService.createComment(command);
    return Result.success(comment);
  }

  /**
   * 删除评论
   *
   * @param taskId 任务ID
   * @param commentId 评论ID
   * @return 空结果
   */
  @DeleteMapping("/{commentId}")
  @RequirePermission("task:comment")
  @Operation(summary = "删除任务评论", description = "删除指定的任务评论")
  @OperationLog(module = "任务协作", action = "删除任务评论")
  public Result<Void> deleteComment(
      @PathVariable final Long taskId, @PathVariable final Long commentId) {
    taskCommentAppService.deleteComment(commentId);
    return Result.success();
  }

  /**
   * 上传任务评论附件（M3-058：附件功能） 上传文件到MinIO并返回标准化的JSONB格式附件信息
   *
   * @param taskId 任务ID
   * @param file 上传的文件
   * @return 附件信息
   */
  @PostMapping(value = "/upload-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RequirePermission("task:comment")
  @Operation(summary = "上传任务评论附件", description = "上传文件到MinIO并返回标准化的JSONB格式附件信息，用于创建评论时使用")
  @OperationLog(module = "任务协作", action = "上传任务评论附件")
  public Result<Map<String, Object>> uploadAttachment(
      @PathVariable final Long taskId, @RequestParam("file") final MultipartFile file) {
    Map<String, Object> attachmentInfo = taskCommentAppService.uploadAttachmentFile(file, taskId);
    return Result.success(attachmentInfo);
  }
}
