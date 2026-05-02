package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.archive.DigitalFileDTO;
import com.archivesystem.dto.archive.FileUrlResponse;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 文件上传控制器.
 * 
 * 权限说明：
 * - 上传：所有已认证用户可提交入库材料
 * - 删除：仅系统管理员和档案管理员
 * - 下载/预览：所有已认证用户
 * @author junyuzhan
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传下载")
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传单个文件（不关联档案）.
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传文件，返回文件ID，后续可关联到档案")
    @PreAuthorize("isAuthenticated()")
    public Result<DigitalFileDTO> upload(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件分类") @RequestParam(required = false) String fileCategory,
            @Parameter(description = "案卷卷号") @RequestParam(required = false) Integer volumeNo,
            @Parameter(description = "案卷分段类型") @RequestParam(required = false) String sectionType,
            @Parameter(description = "件号/文号") @RequestParam(required = false) String documentNo,
            @Parameter(description = "起始页码") @RequestParam(required = false) Integer pageStart,
            @Parameter(description = "截止页码") @RequestParam(required = false) Integer pageEnd,
            @Parameter(description = "版本标识") @RequestParam(required = false) String versionLabel,
            @Parameter(description = "文件来源类型") @RequestParam(required = false) String fileSourceType,
            @Parameter(description = "扫描批次号") @RequestParam(required = false) String scanBatchNo,
            @Parameter(description = "扫描操作人") @RequestParam(required = false) String scanOperator,
            @Parameter(description = "扫描时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scanTime,
            @Parameter(description = "扫描复核状态") @RequestParam(required = false) String scanCheckStatus,
            @Parameter(description = "扫描复核人") @RequestParam(required = false) String scanCheckBy,
            @Parameter(description = "扫描复核时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scanCheckTime) {
        
        DigitalFile digitalFile = fileStorageService.upload(file, null, fileCategory,
                volumeNo, sectionType, documentNo, pageStart, pageEnd, versionLabel,
                fileSourceType, scanBatchNo, scanOperator, scanTime, scanCheckStatus, scanCheckBy, scanCheckTime);

        return Result.success("上传成功", buildUploadResponse(digitalFile));
    }

    /**
     * 批量上传文件.
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件")
    @PreAuthorize("isAuthenticated()")
    public Result<List<DigitalFileDTO>> uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "文件分类") @RequestParam(required = false) String fileCategory,
            @Parameter(description = "案卷卷号") @RequestParam(required = false) Integer volumeNo,
            @Parameter(description = "案卷分段类型") @RequestParam(required = false) String sectionType,
            @Parameter(description = "件号/文号") @RequestParam(required = false) String documentNo,
            @Parameter(description = "起始页码") @RequestParam(required = false) Integer pageStart,
            @Parameter(description = "截止页码") @RequestParam(required = false) Integer pageEnd,
            @Parameter(description = "版本标识") @RequestParam(required = false) String versionLabel,
            @Parameter(description = "文件来源类型") @RequestParam(required = false) String fileSourceType,
            @Parameter(description = "扫描批次号") @RequestParam(required = false) String scanBatchNo,
            @Parameter(description = "扫描操作人") @RequestParam(required = false) String scanOperator,
            @Parameter(description = "扫描时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scanTime,
            @Parameter(description = "扫描复核状态") @RequestParam(required = false) String scanCheckStatus,
            @Parameter(description = "扫描复核人") @RequestParam(required = false) String scanCheckBy,
            @Parameter(description = "扫描复核时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scanCheckTime) {
        if (files == null || files.length == 0) {
            return Result.error("400", "请选择至少一个文件");
        }

        List<DigitalFileDTO> results = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                DigitalFile digitalFile = fileStorageService.upload(file, null, fileCategory,
                        volumeNo, sectionType, documentNo, pageStart, pageEnd, versionLabel,
                        fileSourceType, scanBatchNo, scanOperator, scanTime, scanCheckStatus, scanCheckBy, scanCheckTime);

                results.add(buildUploadResponse(digitalFile));
            } catch (Exception e) {
                failedFiles.add(file.getOriginalFilename());
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            }
        }

        if (results.isEmpty()) {
            return Result.error("500", String.format("批量上传失败，未成功上传任何文件，共失败 %d 个", failedFiles.size()));
        }

        if (!failedFiles.isEmpty()) {
            return Result.success(
                    String.format("上传完成，成功 %d 个，失败 %d 个", results.size(), failedFiles.size()),
                    results
            );
        }

        return Result.success(String.format("上传完成，成功 %d 个", results.size()), results);
    }

    /**
     * 获取文件下载链接.
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "获取下载链接")
    @PreAuthorize("isAuthenticated()")
    public Result<FileUrlResponse> getDownloadUrl(@PathVariable Long id) {
        String url = fileStorageService.getDownloadUrl(id);
        return Result.success(FileUrlResponse.of(url));
    }

    /**
     * 获取文件预览链接.
     */
    @GetMapping("/{id}/preview")
    @Operation(summary = "获取预览链接")
    @PreAuthorize("isAuthenticated()")
    public Result<FileUrlResponse> getPreviewUrl(@PathVariable Long id) {
        String url = fileStorageService.getPreviewUrl(id);
        return Result.success(FileUrlResponse.of(url));
    }

    /**
     * 获取文件预览信息（包含URL和预览类型）.
     */
    @GetMapping("/{id}/preview-info")
    @Operation(summary = "获取预览信息")
    @PreAuthorize("isAuthenticated()")
    public Result<com.archivesystem.dto.file.FilePreviewInfo> getPreviewInfo(@PathVariable Long id) {
        return Result.success(fileStorageService.getPreviewInfo(id));
    }

    /**
     * 删除文件.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        fileStorageService.delete(id);
        return Result.success("删除成功", null);
    }

    private DigitalFileDTO buildUploadResponse(DigitalFile digitalFile) {
        return DigitalFileDTO.builder()
                .id(digitalFile.getId())
                .fileName(digitalFile.getFileName())
                .originalName(digitalFile.getOriginalName())
                .build();
    }
}
