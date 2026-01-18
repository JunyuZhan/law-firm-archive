package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.CreateDocumentCommand;
import com.lawfirm.application.document.command.DocumentAuditQueryCommand;
import com.lawfirm.application.document.command.UpdateDocumentCommand;
import com.lawfirm.application.document.command.UploadNewVersionCommand;
import com.lawfirm.application.document.dto.DocumentAuditStatisticsDTO;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.dto.DocumentQueryDTO;
import com.lawfirm.application.document.service.DocAccessLogService;
import com.lawfirm.application.document.service.DocumentAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.DocAccessLog;
import com.lawfirm.infrastructure.external.onlyoffice.OnlyOfficeService;
import com.lawfirm.infrastructure.external.minio.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * 文档管理接口
 */
@Slf4j
@Tag(name = "文档管理", description = "文档管理相关接口")
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentAppService documentAppService;
    private final DocAccessLogService accessLogService;
    private final OnlyOfficeService onlyOfficeService;
    private final MinioService minioService;

    // 用于生成临时访问 token 的密钥
    private static final String TOKEN_SECRET = "law-firm-document-access-2024";

    /**
     * 分页查询文档
     */
    @GetMapping
    @RequirePermission("doc:list")
    public Result<PageResult<DocumentDTO>> list(DocumentQueryDTO query) {
        return Result.success(documentAppService.listDocuments(query));
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    @RequirePermission("doc:detail")
    public Result<DocumentDTO> getById(@PathVariable Long id, HttpServletRequest request) {
        // 记录查看日志
        accessLogService.logAccess(id, DocAccessLog.ACTION_VIEW, request);
        return Result.success(documentAppService.getDocumentById(id));
    }

    /**
     * 下载文档（记录日志）
     */
    @PostMapping("/{id}/download")
    @RequirePermission("doc:download")
    public Result<DocumentDTO> download(@PathVariable Long id, HttpServletRequest request) {
        accessLogService.logAccess(id, DocAccessLog.ACTION_DOWNLOAD, request);
        return Result.success(documentAppService.getDocumentById(id));
    }

    /**
     * 批量下载文档（打包为 ZIP）
     */
    @PostMapping("/batch-download")
    @RequirePermission("doc:download")
    @OperationLog(module = "文档管理", action = "批量下载文档")
    @Operation(summary = "批量下载文档", description = "将多个文档打包为 ZIP 文件下载")
    public void batchDownload(
            @RequestBody List<Long> ids,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (ids == null || ids.isEmpty()) {
            throw new com.lawfirm.common.exception.BusinessException("请选择要下载的文档");
        }
        if (ids.size() > 100) {
            throw new com.lawfirm.common.exception.BusinessException("单次最多下载100个文档");
        }

        try {
            // 收集文件数据
            Map<String, byte[]> filesMap = new java.util.LinkedHashMap<>();
            for (Long id : ids) {
                DocumentDTO doc = documentAppService.getDocumentById(id);
                if (doc != null && doc.getFilePath() != null) {
                    String objectName = minioService.extractObjectName(doc.getFilePath());
                    try (InputStream is = minioService.downloadFile(objectName)) {
                        filesMap.put(doc.getFileName(), is.readAllBytes());
                        // 记录下载日志
                        accessLogService.logAccess(id, DocAccessLog.ACTION_DOWNLOAD, request);
                    }
                }
            }

            if (filesMap.isEmpty()) {
                throw new com.lawfirm.common.exception.BusinessException("没有找到可下载的文档");
            }

            // 压缩并输出
            byte[] zipData = com.lawfirm.common.util.CompressUtils.zipDataToBytes(filesMap);

            String zipFileName = "documents_" + System.currentTimeMillis() + ".zip";
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    URLEncoder.encode(zipFileName, StandardCharsets.UTF_8) + "\"");
            response.setContentLength(zipData.length);
            response.getOutputStream().write(zipData);
            response.getOutputStream().flush();

            log.info("批量下载完成: {} 个文档, 大小: {} bytes", filesMap.size(), zipData.length);
        } catch (Exception e) {
            log.error("批量下载失败", e);
            throw new com.lawfirm.common.exception.BusinessException("批量下载失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档访问日志
     */
    @GetMapping("/{id}/access-logs")
    @RequirePermission("doc:detail")
    public Result<PageResult<DocAccessLog>> getAccessLogs(
            @PathVariable Long id,
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(accessLogService.getAccessLogs(id, null, actionType, pageNum, pageSize));
    }

    /**
     * 创建文档（仅元数据，文件已在其他地方上传）
     */
    @PostMapping
    @RequirePermission("doc:upload")
    @OperationLog(module = "文档管理", action = "创建文档")
    public Result<DocumentDTO> create(@Valid @RequestBody CreateDocumentCommand command) {
        return Result.success(documentAppService.createDocument(command));
    }

    /**
     * 上传文件并创建文档记录
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("doc:upload")
    @OperationLog(module = "文档管理", action = "上传文件")
    @Operation(summary = "上传文件", description = "上传文件到 MinIO 并创建文档记录")
    public Result<DocumentDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "matterId", required = false) Long matterId,
            @RequestParam(value = "folder", required = false, defaultValue = "documents") String folder,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "dossierItemId", required = false) Long dossierItemId) {
        // ✅ 安全验证：使用 FileValidator 验证文件
        FileValidator.ValidationResult validationResult = FileValidator.validate(file);
        if (!validationResult.isValid()) {
            return Result.error(validationResult.getErrorMessage());
        }
        return Result.success(documentAppService.uploadFile(file, matterId, folder, description, dossierItemId));
    }

    /**
     * 批量上传文件
     */
    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("doc:upload")
    @OperationLog(module = "文档管理", action = "批量上传文件")
    @Operation(summary = "批量上传文件", description = "批量上传多个文件")
    public Result<List<DocumentDTO>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "matterId", required = false) Long matterId,
            @RequestParam(value = "folder", required = false, defaultValue = "documents") String folder,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "dossierItemId", required = false) Long dossierItemId,
            @RequestParam(value = "sourceType", required = false) String sourceType) {
        // ✅ 安全验证：批量验证所有文件
        for (MultipartFile file : files) {
            FileValidator.ValidationResult validationResult = FileValidator.validate(file);
            if (!validationResult.isValid()) {
                return Result
                        .error("文件 " + file.getOriginalFilename() + " 验证失败: " + validationResult.getErrorMessage());
            }
        }
        return Result.success(
                documentAppService.uploadFiles(files, matterId, folder, description, dossierItemId, sourceType));
    }

    /**
     * 更新文档信息
     */
    @PutMapping("/{id}")
    @RequirePermission("doc:edit")
    @OperationLog(module = "文档管理", action = "更新文档")
    public Result<DocumentDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentCommand command) {
        command.setId(id);
        return Result.success(documentAppService.updateDocument(command));
    }

    /**
     * 移动文件到指定目录
     */
    @PutMapping("/{id}/move")
    @RequirePermission("doc:edit")
    @OperationLog(module = "文档管理", action = "移动文件")
    @Operation(summary = "移动文件", description = "将文件移动到指定的卷宗目录")
    public Result<DocumentDTO> moveDocument(
            @PathVariable Long id,
            @RequestParam("targetDossierItemId") Long targetDossierItemId) {
        return Result.success(documentAppService.moveDocument(id, targetDossierItemId));
    }

    /**
     * 重新排序文档
     */
    @PutMapping("/reorder")
    @RequirePermission("doc:edit")
    @OperationLog(module = "文档管理", action = "重新排序文档")
    @Operation(summary = "重新排序文档", description = "按指定顺序重新排列文档")
    public Result<Void> reorderDocuments(@RequestBody List<Long> documentIds) {
        documentAppService.reorderDocuments(documentIds);
        return Result.success();
    }

    /**
     * 上传新版本
     */
    @PostMapping("/{id}/versions")
    @RequirePermission("doc:upload")
    @OperationLog(module = "文档管理", action = "上传新版本")
    public Result<DocumentDTO> uploadNewVersion(@PathVariable Long id,
            @Valid @RequestBody UploadNewVersionCommand command) {
        command.setDocumentId(id);
        return Result.success(documentAppService.uploadNewVersion(command));
    }

    /**
     * 获取文档所有版本
     */
    @GetMapping("/{id}/versions")
    @RequirePermission("doc:detail")
    public Result<List<DocumentDTO>> getVersions(@PathVariable Long id) {
        return Result.success(documentAppService.getDocumentVersions(id));
    }

    /**
     * 回退到指定版本
     */
    @PostMapping("/{id}/versions/{version}/rollback")
    @RequirePermission("doc:edit")
    @OperationLog(module = "文档管理", action = "版本回退")
    @Operation(summary = "回退到指定版本", description = "将文档回退到指定的历史版本，会创建一个新版本")
    public Result<DocumentDTO> rollbackToVersion(@PathVariable Long id, @PathVariable Integer version) {
        return Result.success(documentAppService.rollbackToVersion(id, version));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    @RequirePermission("doc:delete")
    @OperationLog(module = "文档管理", action = "删除文档")
    public Result<Void> delete(@PathVariable Long id) {
        documentAppService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 归档文档
     */
    @PostMapping("/{id}/archive")
    @RequirePermission("doc:archive")
    @OperationLog(module = "文档管理", action = "归档文档")
    public Result<Void> archive(@PathVariable Long id) {
        documentAppService.archiveDocument(id);
        return Result.success();
    }

    /**
     * 按案件查询文档
     */
    @GetMapping("/matter/{matterId}")
    @RequirePermission("doc:list")
    public Result<List<DocumentDTO>> getByMatter(@PathVariable Long matterId) {
        return Result.success(documentAppService.getDocumentsByMatter(matterId));
    }

    /**
     * 获取文档审计统计（M5-044）
     */
    @PostMapping("/audit/statistics")
    @RequirePermission("doc:audit:view")
    @Operation(summary = "获取文档审计统计", description = "统计文档访问情况，包括按用户、文档、操作类型、时间等维度")
    public Result<DocumentAuditStatisticsDTO> getAuditStatistics(@RequestBody DocumentAuditQueryCommand command) {
        return Result.success(accessLogService.getAuditStatistics(command));
    }

    /**
     * 查询文档审计报告（M5-045）
     */
    @PostMapping("/audit/report")
    @RequirePermission("doc:audit:view")
    @Operation(summary = "查询文档审计报告", description = "查询详细的文档访问审计报告数据")
    public Result<List<Map<String, Object>>> queryAuditReport(@RequestBody DocumentAuditQueryCommand command) {
        return Result.success(accessLogService.queryAuditReport(command));
    }

    // ==================== OnlyOffice 在线编辑相关接口 ====================

    /**
     * 获取文档预览配置（只读模式）
     */
    @GetMapping("/{id}/preview")
    @RequirePermission("doc:detail")
    @Operation(summary = "获取文档预览配置", description = "获取 OnlyOffice 文档预览配置，用于在线预览文档")
    public Result<Map<String, Object>> getPreviewConfig(@PathVariable Long id, HttpServletRequest request) {
        DocumentDTO doc = documentAppService.getDocumentById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }

        // 检查文件是否支持预览
        if (!onlyOfficeService.isPreviewSupported(doc.getFileName())) {
            Map<String, Object> result = new HashMap<>();
            result.put("supported", false);
            result.put("message", "该文件类型不支持在线预览");
            result.put("fileName", doc.getFileName());
            result.put("fileUrl", doc.getFilePath());
            return Result.success(result);
        }

        // 记录查看日志
        accessLogService.logAccess(id, DocAccessLog.ACTION_VIEW, request);

        // 获取当前用户信息
        Long userId = SecurityUtils.getUserId();
        String userName = SecurityUtils.getUsername();

        // 生成带签名的文件访问 URL
        // 直接使用 MinIO 预签名 URL
        String fileUrl = onlyOfficeService.buildFileUrl(doc.getFilePath());

        // 生成 OnlyOffice 预览配置
        Map<String, Object> config = onlyOfficeService.generateViewConfig(
                fileUrl,
                doc.getFileName(),
                userId,
                userName);
        config.put("supported", true);
        config.put("documentId", id);

        log.info("用户 {} 预览文档: id={}, fileName={}", userName, id, doc.getFileName());
        return Result.success(config);
    }

    /**
     * 获取文档编辑配置（可编辑模式）
     */
    @GetMapping("/{id}/edit")
    @RequirePermission("doc:edit")
    @Operation(summary = "获取文档编辑配置", description = "获取 OnlyOffice 文档编辑配置，用于在线编辑文档")
    public Result<Map<String, Object>> getEditConfig(@PathVariable Long id, HttpServletRequest request) {
        DocumentDTO doc = documentAppService.getDocumentById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }

        // 检查文件是否支持编辑
        if (!onlyOfficeService.isSupported(doc.getFileName())) {
            Map<String, Object> result = new HashMap<>();
            result.put("supported", false);
            result.put("message", "该文件类型不支持在线编辑");
            result.put("fileName", doc.getFileName());
            return Result.success(result);
        }

        // 记录编辑日志
        accessLogService.logAccess(id, "EDIT", request);

        // 获取当前用户信息
        Long userId = SecurityUtils.getUserId();
        String userName = SecurityUtils.getUsername();

        // 生成文档唯一标识（用于协同编辑）
        // 使用 文档ID + 版本号 作为 key，确保不同版本不会冲突
        String documentKey = "doc_" + id + "_v" + (doc.getVersion() != null ? doc.getVersion() : 1);

        // 生成带签名的文件访问 URL
        // 直接使用 MinIO 预签名 URL，确保 OnlyOffice 容器可以下载
        String fileUrl = onlyOfficeService.buildFileUrl(doc.getFilePath());

        // 生成 OnlyOffice 编辑配置
        Map<String, Object> config = onlyOfficeService.generateEditConfig(
                fileUrl,
                doc.getFileName(),
                documentKey,
                userId,
                userName,
                "/document/" + id + "/callback" // 回调路径
        );
        config.put("supported", true);
        config.put("documentId", id);

        log.info("用户 {} 开始编辑文档: id={}, fileName={}, documentKey={}",
                userName, id, doc.getFileName(), documentKey);
        return Result.success(config);
    }

    /**
     * OnlyOffice 回调接口（保存编辑后的文档）
     * OnlyOffice 会在文档关闭或保存时调用此接口
     */
    @PostMapping("/{id}/callback")
    @Operation(summary = "OnlyOffice 回调", description = "OnlyOffice 文档保存回调接口，不需要认证")
    public Map<String, Object> onlyOfficeCallback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {

        Map<String, Object> response = new HashMap<>();

        try {
            // OnlyOffice 回调状态
            // 0 - 无错误
            // 1 - 文档已保存
            // 2 - 文档已准备好保存（用户关闭了编辑器）
            // 3 - 发生错误
            // 4 - 没有变化
            // 6 - 正在编辑（强制保存）
            // 7 - 发生错误（强制保存）
            Integer status = (Integer) payload.get("status");
            String downloadUrl = (String) payload.get("url");

            log.info("OnlyOffice 回调: documentId={}, status={}, url={}", id, status, downloadUrl);

            if (status == 2 || status == 6) {
                // 需要保存文档
                if (downloadUrl != null && !downloadUrl.isEmpty()) {
                    // 下载编辑后的文档并保存到 MinIO
                    documentAppService.saveFromOnlyOffice(id, downloadUrl);
                    log.info("文档保存成功: id={}", id);
                }
            }

            response.put("error", 0);
        } catch (Exception e) {
            log.error("OnlyOffice 回调处理失败: documentId={}", id, e);
            response.put("error", 1);
            response.put("message", e.getMessage());
        }

        return response;
    }

    /**
     * 检查文件是否支持在线编辑
     */
    @GetMapping("/{id}/edit-support")
    @RequirePermission("doc:detail")
    @Operation(summary = "检查编辑支持", description = "检查文件是否支持在线编辑")
    public Result<Map<String, Object>> checkEditSupport(@PathVariable Long id) {
        DocumentDTO doc = documentAppService.getDocumentById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", id);
        result.put("fileName", doc.getFileName());
        result.put("fileType", doc.getFileType());
        result.put("canEdit", onlyOfficeService.isSupported(doc.getFileName()));
        result.put("canPreview", onlyOfficeService.isPreviewSupported(doc.getFileName()));

        return Result.success(result);
    }

    /**
     * 获取文档预览 URL（带签名的临时访问链接）
     */
    @GetMapping("/{id}/preview-url")
    @RequirePermission("doc:detail")
    @Operation(summary = "获取预览URL", description = "获取文档的临时预览URL，支持所有文件类型")
    public Result<Map<String, Object>> getPreviewUrl(@PathVariable Long id) {
        DocumentDTO doc = documentAppService.getDocumentById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }

        // 生成带签名的访问 URL（2小时有效）
        long expires = System.currentTimeMillis() + 7200 * 1000;
        String token = generateAccessToken(id, expires);
        String previewUrl = "/api/document/" + id + "/content?token=" + token + "&expires=" + expires;

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", id);
        result.put("fileName", doc.getFileName());
        result.put("fileType", doc.getFileType());
        result.put("mimeType", doc.getMimeType());
        result.put("previewUrl", previewUrl);
        result.put("expires", expires);

        return Result.success(result);
    }

    /**
     * 获取文档缩略图URL
     */
    @GetMapping("/{id}/thumbnail")
    @RequirePermission("doc:detail")
    @Operation(summary = "获取缩略图URL", description = "获取文档的缩略图URL，支持图片和PDF文件")
    public Result<Map<String, Object>> getThumbnailUrl(@PathVariable Long id) {
        DocumentDTO doc = documentAppService.getDocumentById(id);
        if (doc == null) {
            return Result.error("文档不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", id);
        result.put("fileName", doc.getFileName());
        result.put("fileType", doc.getFileType());

        // 获取或生成缩略图
        String thumbnailUrl = documentAppService.getThumbnailUrl(id);
        if (thumbnailUrl != null) {
            result.put("thumbnailUrl", thumbnailUrl);
            result.put("hasThumbnail", true);
        } else {
            result.put("hasThumbnail", false);
            result.put("message", "该文件类型不支持缩略图");
        }

        return Result.success(result);
    }

    /**
     * 获取文档原始内容（供 OnlyOffice 访问）
     * 使用临时 token 验证，token 包含文档ID、过期时间和签名
     */
    @GetMapping("/{id}/content")
    @Operation(summary = "获取文档内容", description = "获取文档原始内容，供 OnlyOffice 使用，需要有效的访问 token")
    public void getDocumentContent(
            @PathVariable Long id,
            @RequestParam String token,
            @RequestParam long expires,
            HttpServletResponse response) {
        try {
            // 验证 token
            if (!validateAccessToken(id, token, expires)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "无效的访问令牌");
                return;
            }

            // 检查是否过期
            if (System.currentTimeMillis() > expires) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "访问令牌已过期");
                return;
            }

            DocumentDTO doc = documentAppService.getDocumentById(id);
            if (doc == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文档不存在");
                return;
            }

            // 从 MinIO 获取文件
            String objectName = minioService.extractObjectName(doc.getFilePath());
            if (objectName == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
                return;
            }

            // 设置响应头
            String mimeType = doc.getMimeType();
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "application/octet-stream";
            }
            response.setContentType(mimeType);
            response.setHeader("Content-Disposition", "inline; filename=\"" +
                    URLEncoder.encode(doc.getFileName(), StandardCharsets.UTF_8) + "\"");

            // 输出文件内容
            try (InputStream inputStream = minioService.downloadFile(objectName);
                    OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

            log.debug("文档内容请求成功: id={}, fileName={}", id, doc.getFileName());

        } catch (Exception e) {
            log.error("获取文档内容失败: id={}", id, e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取文件失败");
            } catch (Exception sendError) {
                log.debug("发送错误响应失败", sendError);
            }
        }
    }

    /**
     * 生成文档访问 token
     */
    public static String generateAccessToken(Long documentId, long expires) {
        String data = documentId + ":" + expires + ":" + TOKEN_SECRET;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (Exception e) {
            throw new RuntimeException("生成 token 失败", e);
        }
    }

    /**
     * 验证文档访问 token
     */
    private boolean validateAccessToken(Long documentId, String token, long expires) {
        String expectedToken = generateAccessToken(documentId, expires);
        return expectedToken.equals(token);
    }
}
