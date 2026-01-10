package com.lawfirm.interfaces.rest.evidence;

import com.lawfirm.application.evidence.command.CreateCrossExamCommand;
import com.lawfirm.application.evidence.command.CreateEvidenceCommand;
import com.lawfirm.application.evidence.command.UpdateEvidenceCommand;
import com.lawfirm.application.evidence.dto.EvidenceCrossExamDTO;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceExportItemDTO;
import com.lawfirm.application.evidence.dto.EvidenceQueryDTO;
import com.lawfirm.application.evidence.service.EvidenceAppService;
import com.lawfirm.application.evidence.service.EvidenceExportService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.external.document.DocumentContentExtractor;
import com.lawfirm.infrastructure.external.file.FileTypeService;
import com.lawfirm.infrastructure.external.file.ThumbnailService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 证据管理接口
 */
@Slf4j
@RestController
@RequestMapping("/evidence")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceAppService evidenceAppService;
    private final EvidenceExportService evidenceExportService;
    private final MinioService minioService;
    private final DocumentContentExtractor documentContentExtractor;
    private final FileTypeService fileTypeService;
    private final ThumbnailService thumbnailService;

    /**
     * 分页查询证据
     */
    @GetMapping
    @RequirePermission("evidence:view")
    public Result<PageResult<EvidenceDTO>> list(EvidenceQueryDTO query) {
        return Result.success(evidenceAppService.listEvidence(query));
    }

    /**
     * 获取证据详情
     */
    @GetMapping("/{id}")
    @RequirePermission("evidence:view")
    public Result<EvidenceDTO> getById(@PathVariable Long id) {
        return Result.success(evidenceAppService.getEvidenceById(id));
    }

    /**
     * 创建证据
     */
    @PostMapping
    @RequirePermission("evidence:create")
    @OperationLog(module = "证据管理", action = "添加证据")
    public Result<EvidenceDTO> create(@Valid @RequestBody CreateEvidenceCommand command) {
        return Result.success(evidenceAppService.createEvidence(command));
    }

    /**
     * 更新证据
     */
    @PutMapping("/{id}")
    @RequirePermission("evidence:edit")
    @OperationLog(module = "证据管理", action = "更新证据")
    public Result<EvidenceDTO> update(@PathVariable Long id,
                                      @Valid @RequestBody UpdateEvidenceCommand command) {
        return Result.success(evidenceAppService.updateEvidence(id, command));
    }

    /**
     * 删除证据
     */
    @DeleteMapping("/{id}")
    @RequirePermission("evidence:delete")
    @OperationLog(module = "证据管理", action = "删除证据")
    public Result<Void> delete(@PathVariable Long id) {
        evidenceAppService.deleteEvidence(id);
        return Result.success();
    }

    /**
     * 调整排序
     */
    @PutMapping("/{id}/sort")
    @RequirePermission("evidence:edit")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam Integer sortOrder) {
        evidenceAppService.updateSortOrder(id, sortOrder);
        return Result.success();
    }

    /**
     * 批量调整分组
     */
    @PostMapping("/batch-group")
    @RequirePermission("evidence:edit")
    @OperationLog(module = "证据管理", action = "批量调整分组")
    public Result<Void> batchUpdateGroup(@RequestBody List<Long> ids, @RequestParam String groupName) {
        evidenceAppService.batchUpdateGroup(ids, groupName);
        return Result.success();
    }

    /**
     * 添加质证记录
     */
    @PostMapping("/{id}/cross-exam")
    @RequirePermission("evidence:crossExam")
    @OperationLog(module = "证据管理", action = "添加质证记录")
    public Result<EvidenceCrossExamDTO> addCrossExam(@PathVariable Long id,
                                                     @Valid @RequestBody CreateCrossExamCommand command) {
        command.setEvidenceId(id);
        return Result.success(evidenceAppService.addCrossExam(command));
    }

    /**
     * 完成质证
     */
    @PostMapping("/{id}/complete-cross-exam")
    @RequirePermission("evidence:crossExam")
    @OperationLog(module = "证据管理", action = "完成质证")
    public Result<Void> completeCrossExam(@PathVariable Long id) {
        evidenceAppService.completeCrossExam(id);
        return Result.success();
    }

    /**
     * 按案件获取证据列表
     */
    @GetMapping("/matter/{matterId}")
    @RequirePermission("evidence:view")
    public Result<List<EvidenceDTO>> getByMatter(@PathVariable Long matterId) {
        return Result.success(evidenceAppService.getEvidenceByMatter(matterId));
    }

    /**
     * 获取案件的证据分组
     */
    @GetMapping("/matter/{matterId}/groups")
    @RequirePermission("evidence:view")
    public Result<List<String>> getGroups(@PathVariable Long matterId) {
        return Result.success(evidenceAppService.getEvidenceGroups(matterId));
    }

    /**
     * 上传证据文件
     */
    @PostMapping("/upload")
    @RequirePermission("evidence:create")
    @OperationLog(module = "证据管理", action = "上传证据文件")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // ✅ 安全验证：使用 FileValidator 验证文件（防止恶意文件）
            FileValidator.ValidationResult securityResult = FileValidator.validate(file);
            if (!securityResult.isValid()) {
                log.warn("证据文件安全验证失败: {}, 原因: {}", file.getOriginalFilename(), securityResult.getErrorMessage());
                throw new com.lawfirm.common.exception.BusinessException(securityResult.getErrorMessage());
            }
            
            // 业务验证（文件类型）
            String validationError = fileTypeService.validateFile(file);
            if (validationError != null) {
                throw new com.lawfirm.common.exception.BusinessException(validationError);
            }

            // 上传原始文件
            String fileUrl = minioService.uploadFile(file, "evidence/");
            
            // 获取文件类型信息
            String fileName = file.getOriginalFilename();
            FileTypeService.FileTypeInfo typeInfo = fileTypeService.getFileTypeInfo(fileName);
            
            // 生成缩略图（仅图片文件）
            String thumbnailUrl = null;
            if (typeInfo.isCanGenerateThumbnail()) {
                thumbnailUrl = thumbnailService.generateThumbnail(file, fileUrl);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", fileName);
            result.put("fileSize", file.getSize());
            result.put("fileType", typeInfo.getType());
            result.put("thumbnailUrl", thumbnailUrl);
            result.put("canPreview", typeInfo.isCanPreview());
            return Result.success(result);
        } catch (com.lawfirm.common.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new com.lawfirm.common.exception.BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件预览URL（预签名URL，有效期1小时）
     */
    @GetMapping("/{id}/preview")
    @RequirePermission("evidence:view")
    public Result<Map<String, Object>> getPreviewUrl(@PathVariable Long id) {
        EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
        if (evidence.getFileUrl() == null) {
            throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
        }
        
        FileTypeService.FileTypeInfo typeInfo = fileTypeService.getFileTypeInfo(evidence.getFileName());
        
        Map<String, Object> result = new HashMap<>();
        try {
            // 从 fileUrl 提取 objectName 并生成预签名 URL
            String objectName = minioService.extractObjectName(evidence.getFileUrl());
            if (objectName != null) {
                // 生成1小时有效的预签名URL
                String presignedUrl = minioService.getPresignedUrl(objectName, 3600);
                result.put("fileUrl", presignedUrl);
            } else {
                result.put("fileUrl", evidence.getFileUrl());
            }
        } catch (Exception e) {
            log.warn("生成预签名URL失败，使用原始URL: {}", e.getMessage());
            result.put("fileUrl", evidence.getFileUrl());
        }
        result.put("fileName", evidence.getFileName());
        result.put("fileType", typeInfo.getType());
        result.put("canPreview", typeInfo.isCanPreview());
        result.put("icon", typeInfo.getIcon());
        return Result.success(result);
    }

    /**
     * 获取文件下载URL（预签名URL，有效期1小时）
     */
    @GetMapping("/{id}/download-url")
    @RequirePermission("evidence:view")
    public Result<Map<String, String>> getDownloadUrl(@PathVariable Long id) {
        EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
        if (evidence.getFileUrl() == null) {
            throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
        }
        
        Map<String, String> result = new HashMap<>();
        try {
            String objectName = minioService.extractObjectName(evidence.getFileUrl());
            if (objectName != null) {
                String presignedUrl = minioService.getPresignedUrl(objectName, 3600);
                result.put("downloadUrl", presignedUrl);
            } else {
                result.put("downloadUrl", evidence.getFileUrl());
            }
        } catch (Exception e) {
            log.warn("生成下载URL失败: {}", e.getMessage());
            result.put("downloadUrl", evidence.getFileUrl());
        }
        result.put("fileName", evidence.getFileName());
        return Result.success(result);
    }

    /**
     * 批量下载证据文件（打包为 ZIP）
     */
    @PostMapping("/batch-download")
    @RequirePermission("evidence:download")
    @OperationLog(module = "证据管理", action = "批量下载证据")
    @Operation(summary = "批量下载证据", description = "将多个证据文件打包为 ZIP 文件下载")
    public void batchDownload(
            @RequestBody List<Long> ids,
            HttpServletResponse response) {
        if (ids == null || ids.isEmpty()) {
            throw new com.lawfirm.common.exception.BusinessException("请选择要下载的证据");
        }
        if (ids.size() > 100) {
            throw new com.lawfirm.common.exception.BusinessException("单次最多下载100个证据文件");
        }
        
        try {
            // 收集文件数据
            Map<String, byte[]> filesMap = new java.util.LinkedHashMap<>();
            for (Long id : ids) {
                EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
                if (evidence != null && evidence.getFileUrl() != null) {
                    String objectName = minioService.extractObjectName(evidence.getFileUrl());
                    if (objectName != null) {
                        byte[] fileBytes = minioService.downloadFileAsBytes(objectName);
                        // 使用"编号_文件名"格式避免重名
                        String fileName = evidence.getEvidenceNo() + "_" + evidence.getFileName();
                        filesMap.put(fileName, fileBytes);
                    }
                }
            }
            
            if (filesMap.isEmpty()) {
                throw new com.lawfirm.common.exception.BusinessException("没有找到可下载的证据文件");
            }
            
            // 压缩并输出
            byte[] zipData = com.lawfirm.common.util.CompressUtils.zipDataToBytes(filesMap);
            
            String zipFileName = "evidence_" + System.currentTimeMillis() + ".zip";
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + 
                    java.net.URLEncoder.encode(zipFileName, java.nio.charset.StandardCharsets.UTF_8) + "\"");
            response.setContentLength(zipData.length);
            response.getOutputStream().write(zipData);
            response.getOutputStream().flush();
            
            log.info("证据批量下载完成: {} 个文件, 大小: {} bytes", filesMap.size(), zipData.length);
        } catch (Exception e) {
            log.error("证据批量下载失败", e);
            throw new com.lawfirm.common.exception.BusinessException("批量下载失败: " + e.getMessage());
        }
    }

    /**
     * 获取 OnlyOffice 预览URL（Docker 容器可访问的预签名URL）
     */
    @GetMapping("/{id}/onlyoffice-url")
    @RequirePermission("evidence:view")
    public Result<Map<String, Object>> getOnlyOfficeUrl(@PathVariable Long id) {
        EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
        if (evidence.getFileUrl() == null) {
            throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
        }
        
        FileTypeService.FileTypeInfo typeInfo = fileTypeService.getFileTypeInfo(evidence.getFileName());
        
        Map<String, Object> result = new HashMap<>();
        // 使用后端代理 URL，OnlyOffice 通过后端下载文件
        // 后端运行在宿主机，OnlyOffice 通过 host.docker.internal 访问
        String proxyUrl = "http://host.docker.internal:8080/evidence/" + id + "/file-proxy";
        result.put("fileUrl", proxyUrl);
        result.put("fileName", evidence.getFileName());
        result.put("fileType", typeInfo.getType());
        return Result.success(result);
    }

    /**
     * 文件代理接口（供 OnlyOffice 容器下载文件）
     */
    @GetMapping("/{id}/file-proxy")
    public void fileProxy(@PathVariable Long id, HttpServletResponse response) {
        try {
            EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
            if (evidence.getFileUrl() == null) {
                response.sendError(404, "文件不存在");
                return;
            }
            
            String objectName = minioService.extractObjectName(evidence.getFileUrl());
            if (objectName == null) {
                response.sendError(404, "文件路径无效");
                return;
            }
            
            byte[] fileBytes = minioService.downloadFileAsBytes(objectName);
            
            // 设置响应头
            String contentType = getContentType(evidence.getFileName());
            response.setContentType(contentType);
            response.setContentLength(fileBytes.length);
            response.setHeader("Content-Disposition", "inline; filename=\"" + evidence.getFileName() + "\"");
            
            // 写入文件内容
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("文件代理失败", e);
            try {
                response.sendError(500, "文件下载失败");
            } catch (Exception ignored) {}
        }
    }

    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String ext = fileName.toLowerCase();
        if (ext.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (ext.endsWith(".doc")) return "application/msword";
        if (ext.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (ext.endsWith(".xls")) return "application/vnd.ms-excel";
        if (ext.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (ext.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (ext.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    /**
     * 获取文件缩略图URL
     */
    @GetMapping("/{id}/thumbnail")
    @RequirePermission("evidence:view")
    public Result<Map<String, Object>> getThumbnailUrl(@PathVariable Long id) {
        EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
        if (evidence.getFileUrl() == null) {
            throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
        }
        
        FileTypeService.FileTypeInfo typeInfo = fileTypeService.getFileTypeInfo(evidence.getFileName());
        
        Map<String, Object> result = new HashMap<>();
        if (fileTypeService.isImageFile(evidence.getFileName())) {
            // 图片文件：返回缩略图URL或原文件URL
            result.put("thumbnailUrl", evidence.getThumbnailUrl() != null ? 
                    evidence.getThumbnailUrl() : evidence.getFileUrl());
        } else {
            // 非图片文件返回null，前端显示默认图标
            result.put("thumbnailUrl", null);
        }
        result.put("fileType", typeInfo.getType());
        result.put("icon", typeInfo.getIcon());
        return Result.success(result);
    }

    /**
     * 判断文件类型（保留用于兼容）
     */
    private String getFileType(String fileName) {
        return fileTypeService.getFileTypeInfo(fileName).getType();
    }

    /**
     * 获取文件文本内容（用于Word/Excel等文档预览）
     */
    @GetMapping("/{id}/content")
    @RequirePermission("evidence:view")
    @OperationLog(module = "证据管理", action = "获取文件内容")
    public Result<Map<String, Object>> getFileContent(@PathVariable Long id) {
        EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
        if (evidence.getFileUrl() == null) {
            throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
        }
        
        Map<String, Object> result = new HashMap<>();
        String fileType = getFileType(evidence.getFileName());
        
        try {
            // Word/Excel文档：使用Apache POI提取文本内容
            if ("word".equals(fileType) || "excel".equals(fileType)) {
                // 从MinIO下载文件
                String objectName = minioService.extractObjectName(evidence.getFileUrl());
                if (objectName == null) {
                    result.put("content", "无法解析文件URL");
                    result.put("supported", false);
                    return Result.success(result);
                }
                
                byte[] fileBytes = minioService.downloadFileAsBytes(objectName);
                String content = documentContentExtractor.extractText(fileBytes, evidence.getFileName());
                
                result.put("content", content);
                result.put("supported", true);
            } else if ("pdf".equals(fileType)) {
                // PDF文本提取可以通过OCR服务实现，暂时返回提示
                result.put("content", "PDF文本提取功能需要OCR服务支持，当前版本暂不支持。");
                result.put("supported", false);
            } else {
                result.put("content", "");
                result.put("supported", false);
            }
        } catch (Exception e) {
            log.error("提取文件内容失败", e);
            result.put("content", "文档内容提取失败: " + e.getMessage());
            result.put("supported", false);
        }
        
        return Result.success(result);
    }

    /**
     * 导出证据清单为 Word 文档
     */
    @PostMapping("/matter/{matterId}/export")
    @RequirePermission("evidence:view")
    @OperationLog(module = "证据管理", action = "导出证据清单")
    public void exportEvidenceList(
            @PathVariable Long matterId,
            @RequestParam(defaultValue = "word") String format,
            @RequestBody(required = false) List<EvidenceExportItemDTO> items,
            HttpServletResponse response) {
        try {
            byte[] content;
            String contentType;
            String extension;

            if ("word".equalsIgnoreCase(format) || "docx".equalsIgnoreCase(format)) {
                content = evidenceExportService.exportToWord(matterId, items);
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
            } else {
                throw new com.lawfirm.common.exception.BusinessException("暂不支持的导出格式: " + format);
            }

            String fileName = evidenceExportService.getExportFileName(matterId, extension);
            
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + 
                    java.net.URLEncoder.encode(fileName, "UTF-8") + "\"");
            response.setContentLength(content.length);
            response.getOutputStream().write(content);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出证据清单失败", e);
            try {
                response.sendError(500, "导出失败: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    /**
     * 导出证据清单（GET方式，导出全部）
     */
    @GetMapping("/matter/{matterId}/export")
    @RequirePermission("evidence:view")
    @OperationLog(module = "证据管理", action = "导出证据清单")
    public void exportEvidenceListGet(
            @PathVariable Long matterId,
            @RequestParam(defaultValue = "word") String format,
            HttpServletResponse response) {
        exportEvidenceList(matterId, format, null, response);
    }
}
