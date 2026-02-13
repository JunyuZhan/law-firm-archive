package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.archive.DigitalFileDTO;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传控制器.
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传下载")
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传单个文件（不关联档案）.
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传文件，返回文件ID，后续可关联到档案")
    public Result<DigitalFileDTO> upload(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件分类") @RequestParam(required = false) String fileCategory) {
        
        DigitalFile digitalFile = fileStorageService.upload(file, null, fileCategory);
        
        DigitalFileDTO dto = DigitalFileDTO.builder()
                .id(digitalFile.getId())
                .fileName(digitalFile.getFileName())
                .originalName(digitalFile.getOriginalName())
                .fileExtension(digitalFile.getFileExtension())
                .mimeType(digitalFile.getMimeType())
                .fileSize(digitalFile.getFileSize())
                .hashValue(digitalFile.getHashValue())
                .uploadAt(digitalFile.getUploadAt())
                .build();
        
        return Result.success("上传成功", dto);
    }

    /**
     * 批量上传文件.
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件")
    public Result<List<DigitalFileDTO>> uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "文件分类") @RequestParam(required = false) String fileCategory) {
        
        List<DigitalFileDTO> results = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                DigitalFile digitalFile = fileStorageService.upload(file, null, fileCategory);
                
                DigitalFileDTO dto = DigitalFileDTO.builder()
                        .id(digitalFile.getId())
                        .fileName(digitalFile.getFileName())
                        .originalName(digitalFile.getOriginalName())
                        .fileSize(digitalFile.getFileSize())
                        .mimeType(digitalFile.getMimeType())
                        .build();
                
                results.add(dto);
            } catch (Exception e) {
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            }
        }
        
        return Result.success("上传完成", results);
    }

    /**
     * 获取文件下载链接.
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "获取下载链接")
    public Result<String> getDownloadUrl(@PathVariable Long id) {
        String url = fileStorageService.getDownloadUrl(id);
        return Result.success(url);
    }

    /**
     * 获取文件预览链接.
     */
    @GetMapping("/{id}/preview")
    @Operation(summary = "获取预览链接")
    public Result<String> getPreviewUrl(@PathVariable Long id) {
        String url = fileStorageService.getPreviewUrl(id);
        return Result.success(url);
    }

    /**
     * 删除文件.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    public Result<Void> delete(@PathVariable Long id) {
        fileStorageService.delete(id);
        return Result.success("删除成功", null);
    }
}
