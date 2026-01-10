package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateTrainingNoticeCommand;
import com.lawfirm.application.hr.dto.TrainingCompletionDTO;
import com.lawfirm.application.hr.dto.TrainingNoticeDTO;
import com.lawfirm.application.hr.service.TrainingNoticeAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.infrastructure.external.minio.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 培训通知 Controller（简化版）
 * 
 * 功能：
 * 1. 行政发布培训通知
 * 2. 律师上传合格证
 * 3. 行政查看完成情况
 */
@Slf4j
@Tag(name = "培训通知", description = "培训通知管理相关接口")
@RestController
@RequestMapping("/hr/training-notice")
@RequiredArgsConstructor
public class TrainingNoticeController {

    private final TrainingNoticeAppService trainingNoticeAppService;
    private final MinioService minioService;

    /**
     * 分页查询培训通知列表
     */
    @Operation(summary = "分页查询培训通知列表")
    @GetMapping
    public Result<PageResult<TrainingNoticeDTO>> listNotices(PageQuery query) {
        PageResult<TrainingNoticeDTO> result = trainingNoticeAppService.listNotices(query);
        return Result.success(result);
    }

    /**
     * 获取培训通知详情
     */
    @Operation(summary = "获取培训通知详情")
    @GetMapping("/{id}")
    public Result<TrainingNoticeDTO> getNotice(@PathVariable Long id) {
        TrainingNoticeDTO notice = trainingNoticeAppService.getNoticeById(id);
        return Result.success(notice);
    }

    /**
     * 发布培训通知（管理员）
     */
    @Operation(summary = "发布培训通知")
    @PostMapping
    @RequirePermission("hr:training:create")
    @OperationLog(module = "培训管理", action = "发布培训通知")
    public Result<TrainingNoticeDTO> createNotice(@RequestBody @Valid CreateTrainingNoticeCommand command) {
        TrainingNoticeDTO notice = trainingNoticeAppService.createNotice(command);
        return Result.success(notice);
    }

    /**
     * 删除培训通知（管理员）
     */
    @Operation(summary = "删除培训通知")
    @DeleteMapping("/{id}")
    @RequirePermission("hr:training:delete")
    @OperationLog(module = "培训管理", action = "删除培训通知")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        trainingNoticeAppService.deleteNotice(id);
        return Result.success();
    }

    /**
     * 完成培训（上传合格证）
     */
    @Operation(summary = "完成培训（上传合格证）")
    @PostMapping("/{id}/complete")
    @OperationLog(module = "培训管理", action = "上传培训合格证")
    public Result<Void> completeTraining(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        String certificateUrl = body.get("certificateUrl");
        String certificateName = body.get("certificateName");
        trainingNoticeAppService.completeTraining(id, certificateUrl, certificateName);
        return Result.success();
    }

    /**
     * 获取完成情况列表（管理员）
     */
    @Operation(summary = "获取完成情况列表")
    @GetMapping("/completions")
    @RequirePermission("hr:training:list")
    public Result<PageResult<TrainingCompletionDTO>> listCompletions(PageQuery query) {
        PageResult<TrainingCompletionDTO> result = trainingNoticeAppService.listCompletions(query);
        return Result.success(result);
    }

    /**
     * 获取文件下载URL（预签名URL，有效期1小时）
     */
    @Operation(summary = "获取文件下载URL")
    @GetMapping("/download-url")
    public Result<Map<String, String>> getDownloadUrl(@RequestParam String fileUrl,
                                                      @RequestParam(required = false) String fileName) {
        Map<String, String> result = new HashMap<>();
        try {
            String objectName = minioService.extractObjectName(fileUrl);
            if (objectName != null) {
                // 生成1小时有效的预签名URL
                String presignedUrl = minioService.getPresignedUrl(objectName, 3600);
                result.put("downloadUrl", presignedUrl);
            } else {
                result.put("downloadUrl", fileUrl);
            }
        } catch (Exception e) {
            log.warn("生成下载URL失败: {}", e.getMessage());
            result.put("downloadUrl", fileUrl);
        }
        if (fileName != null) {
            result.put("fileName", fileName);
        }
        return Result.success(result);
    }
}

