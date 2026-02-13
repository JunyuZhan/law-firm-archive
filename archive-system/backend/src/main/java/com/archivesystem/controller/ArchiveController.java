package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.archive.*;
import com.archivesystem.service.ArchiveService;
import com.archivesystem.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 档案控制器.
 */
@Slf4j
@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
@Tag(name = "档案管理", description = "档案CRUD、文件上传下载")
public class ArchiveController {

    private final ArchiveService archiveService;
    private final FileStorageService fileStorageService;

    /**
     * 创建档案.
     */
    @PostMapping
    @Operation(summary = "创建档案")
    public Result<ArchiveDTO> create(@Valid @RequestBody ArchiveCreateRequest request) {
        ArchiveDTO archive = archiveService.create(request);
        return Result.success("创建成功", archive);
    }

    /**
     * 更新档案.
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新档案")
    public Result<ArchiveDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveCreateRequest request) {
        ArchiveDTO archive = archiveService.update(id, request);
        return Result.success("更新成功", archive);
    }

    /**
     * 获取档案详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取档案详情")
    public Result<ArchiveDTO> getById(@PathVariable Long id) {
        ArchiveDTO archive = archiveService.getById(id);
        return Result.success(archive);
    }

    /**
     * 根据档案号获取详情.
     */
    @GetMapping("/no/{archiveNo}")
    @Operation(summary = "根据档案号获取详情")
    public Result<ArchiveDTO> getByArchiveNo(@PathVariable String archiveNo) {
        ArchiveDTO archive = archiveService.getByArchiveNo(archiveNo);
        return Result.success(archive);
    }

    /**
     * 分页查询档案列表.
     */
    @GetMapping
    @Operation(summary = "查询档案列表")
    public Result<PageResult<ArchiveDTO>> query(ArchiveQueryRequest request) {
        PageResult<ArchiveDTO> result = archiveService.query(request);
        return Result.success(result);
    }

    /**
     * 删除档案.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除档案")
    public Result<Void> delete(@PathVariable Long id) {
        archiveService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 更新档案状态.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新档案状态")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        archiveService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }

    /**
     * 上传文件到档案.
     */
    @PostMapping("/{archiveId}/files")
    @Operation(summary = "上传文件到档案")
    public Result<DigitalFileDTO> uploadFile(
            @PathVariable Long archiveId,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件分类") @RequestParam(required = false) String fileCategory) {
        var digitalFile = fileStorageService.upload(file, archiveId, fileCategory);
        
        DigitalFileDTO dto = DigitalFileDTO.builder()
                .id(digitalFile.getId())
                .archiveId(digitalFile.getArchiveId())
                .fileName(digitalFile.getFileName())
                .originalName(digitalFile.getOriginalName())
                .fileSize(digitalFile.getFileSize())
                .mimeType(digitalFile.getMimeType())
                .build();
        
        return Result.success("上传成功", dto);
    }

    /**
     * 获取文件下载链接.
     */
    @GetMapping("/files/{fileId}/download-url")
    @Operation(summary = "获取文件下载链接")
    public Result<Map<String, String>> getDownloadUrl(@PathVariable Long fileId) {
        String url = fileStorageService.getDownloadUrl(fileId);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success(result);
    }

    /**
     * 获取文件预览链接.
     */
    @GetMapping("/files/{fileId}/preview-url")
    @Operation(summary = "获取文件预览链接")
    public Result<Map<String, String>> getPreviewUrl(@PathVariable Long fileId) {
        String url = fileStorageService.getPreviewUrl(fileId);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success(result);
    }

    /**
     * 删除文件.
     */
    @DeleteMapping("/files/{fileId}")
    @Operation(summary = "删除文件")
    public Result<Void> deleteFile(@PathVariable Long fileId) {
        fileStorageService.delete(fileId);
        return Result.success("删除成功", null);
    }
}
