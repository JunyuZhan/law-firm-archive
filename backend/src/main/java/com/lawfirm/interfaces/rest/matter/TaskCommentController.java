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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 任务评论管理接口（M3-057~M3-059）
 */
@Tag(name = "任务协作", description = "任务评论、附件、@提醒等协作功能")
@RestController
@RequestMapping("/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentAppService taskCommentAppService;

    /**
     * 获取任务的所有评论（M3-057）
     */
    @GetMapping
    @RequirePermission("task:view")
    @Operation(summary = "获取任务评论列表", description = "获取指定任务的所有评论")
    public Result<List<TaskCommentDTO>> getTaskComments(@PathVariable Long taskId) {
        List<TaskCommentDTO> comments = taskCommentAppService.getTaskComments(taskId);
        return Result.success(comments);
    }

    /**
     * 创建任务评论（M3-057：评论功能，M3-058：附件功能，M3-059：@提醒功能）
     */
    @PostMapping
    @RequirePermission("task:comment")
    @Operation(summary = "创建任务评论", description = "创建任务评论，支持附件和@提醒")
    @OperationLog(module = "任务协作", action = "创建任务评论")
    public Result<TaskCommentDTO> createComment(@PathVariable Long taskId,
                                                @RequestBody @Valid CreateTaskCommentCommand command) {
        command.setTaskId(taskId);
        TaskCommentDTO comment = taskCommentAppService.createComment(command);
        return Result.success(comment);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    @RequirePermission("task:comment")
    @Operation(summary = "删除任务评论", description = "删除指定的任务评论")
    @OperationLog(module = "任务协作", action = "删除任务评论")
    public Result<Void> deleteComment(@PathVariable Long taskId,
                                       @PathVariable Long commentId) {
        taskCommentAppService.deleteComment(commentId);
        return Result.success();
    }

    /**
     * 上传任务评论附件（M3-058：附件功能）
     * 上传文件到MinIO并返回标准化的JSONB格式附件信息
     */
    @PostMapping(value = "/upload-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("task:comment")
    @Operation(summary = "上传任务评论附件", description = "上传文件到MinIO并返回标准化的JSONB格式附件信息，用于创建评论时使用")
    @OperationLog(module = "任务协作", action = "上传任务评论附件")
    public Result<Map<String, Object>> uploadAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> attachmentInfo = taskCommentAppService.uploadAttachmentFile(file, taskId);
        return Result.success(attachmentInfo);
    }
}

