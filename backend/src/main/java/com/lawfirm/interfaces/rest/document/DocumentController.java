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
import org.springframework.web.bind.annotation.RequestMethod;
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
        // 优先使用后端代理接口，确保 OnlyOffice 容器能够访问
        // 使用文档ID构建代理URL，OnlyOffice 通过 Docker 网络访问 backend 服务
        String fileUrl = onlyOfficeService.buildFileUrlForDocument(id);

        // 根据 MIME 类型修正文件名扩展名（如果文件名扩展名与实际内容不匹配）
        String correctedFileName = correctFileNameByMimeType(doc.getFileName(), doc.getMimeType());

        // 生成 OnlyOffice 预览配置
        Map<String, Object> config = onlyOfficeService.generateViewConfig(
                fileUrl,
                correctedFileName,
                userId,
                userName);
        config.put("supported", true);
        config.put("documentId", id);

        log.info("用户 {} 预览文档: id={}, originalFileName={}, correctedFileName={}, mimeType={}", 
            userName, id, doc.getFileName(), correctedFileName, doc.getMimeType());
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
        // 使用后端代理接口，确保 OnlyOffice 容器能够访问
        String fileUrl = onlyOfficeService.buildFileUrlForDocument(id);

        // 根据 MIME 类型修正文件名扩展名（如果文件名扩展名与实际内容不匹配）
        String correctedFileName = correctFileNameByMimeType(doc.getFileName(), doc.getMimeType());

        // 生成 OnlyOffice 编辑配置
        Map<String, Object> config = onlyOfficeService.generateEditConfig(
                fileUrl,
                correctedFileName,
                documentKey,
                userId,
                userName,
                "/document/" + id + "/callback" // 回调路径
        );
        config.put("supported", true);
        config.put("documentId", id);

        log.info("用户 {} 开始编辑文档: id={}, originalFileName={}, correctedFileName={}, documentKey={}, mimeType={}",
                userName, id, doc.getFileName(), correctedFileName, documentKey, doc.getMimeType());
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
     * 处理 OPTIONS 预检请求（CORS）
     */
    @RequestMapping(value = "/{id}/file-proxy", method = RequestMethod.OPTIONS)
    public void fileProxyOptions(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * 文档文件代理接口（供 OnlyOffice 容器下载文件）
     * 安全措施：
     * 1. 使用临时 token 验证（防止未授权访问）
     * 2. 验证请求来源（只允许 Docker 内部网络或 OnlyOffice 容器）
     * 3. Token 有时效性（2小时）
     */
    @GetMapping("/{id}/file-proxy")
    @Operation(summary = "文档文件代理", description = "供 OnlyOffice 容器下载文档文件，需要有效的访问 token")
    public void fileProxy(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) Long expires,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // 安全验证：检查 token（如果提供）
            // 注意：OnlyOffice 容器可能无法传递 token，所以这里允许无 token 访问
            // 但会验证请求来源（通过 IP 或 User-Agent）
            if (token != null && expires != null) {
                // URL 解码 token（如果被编码了）
                String decodedToken = token;
                try {
                    decodedToken = java.net.URLDecoder.decode(token, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // 如果解码失败，使用原始 token
                    log.debug("Token URL 解码失败，使用原始 token: {}", e.getMessage());
                }
                
                log.info("OnlyOffice 文件代理请求（带token）: documentId={}, token={}, expires={}", 
                    id, decodedToken.substring(0, Math.min(8, decodedToken.length())) + "...", expires);
                
                if (!validateAccessToken(id, decodedToken, expires)) {
                    log.warn("Token 验证失败: documentId={}", id);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "无效的访问令牌");
                    return;
                }
                if (System.currentTimeMillis() > expires) {
                    log.warn("Token 已过期: documentId={}, expires={}", id, expires);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "访问令牌已过期");
                    return;
                }
            } else {
                // 无 token 时，验证请求来源（只允许 Docker 内部网络）
                // OnlyOffice 容器在 Docker 网络中，可以通过服务名访问
                String clientIp = getClientIp(request);
                String userAgent = request.getHeader("User-Agent") != null 
                    ? request.getHeader("User-Agent").toLowerCase() 
                    : "";
                
                // 允许 Docker 内部网络访问（172.x.x.x, 10.x.x.x, 192.168.x.x）
                // 或者请求来自 OnlyOffice 容器（通过服务名 onlyoffice）
                boolean isInternalNetwork = clientIp != null && (
                    clientIp.startsWith("172.") || 
                    clientIp.startsWith("10.") || 
                    clientIp.startsWith("192.168.") ||
                    clientIp.equals("127.0.0.1") ||
                    clientIp.equals("::1")
                );
                
                // 记录安全日志
                log.info("OnlyOffice 文件代理请求（无token）: documentId={}, clientIp={}, userAgent={}, isInternal={}", 
                    id, clientIp, userAgent, isInternalNetwork);
                
                // 如果没有 token 且不是内部网络，拒绝访问
                // 注意：在生产环境中，建议始终使用 token
                if (!isInternalNetwork) {
                    log.warn("拒绝未授权访问: documentId={}, clientIp={}", id, clientIp);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "访问被拒绝：需要有效的访问令牌或内部网络访问");
                    return;
                }
            }

            DocumentDTO doc = documentAppService.getDocumentById(id);
            if (doc == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文档不存在");
                return;
            }

            // 从 MinIO 获取文件
            String objectName = minioService.extractObjectName(doc.getFilePath());
            log.info("OnlyOffice 文件代理: documentId={}, filePath={}, objectName={}, bucketName={}",
                id, doc.getFilePath(), objectName, minioService.getBucketName());
            if (objectName == null) {
                log.error("无法解析 objectName: documentId={}, filePath={}, bucketName={}",
                    id, doc.getFilePath(), minioService.getBucketName());
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
                return;
            }

            // 设置响应头
            // 优先根据实际文件内容检测文件类型，而不是依赖数据库中的 MIME 类型
            String detectedMimeType = detectMimeTypeFromContent(doc, minioService, objectName);
            String mimeType = detectedMimeType != null ? detectedMimeType : doc.getMimeType();
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "application/octet-stream";
            }
            
            // 根据检测到的 MIME 类型修正文件名
            String correctedFileName = correctFileNameByMimeType(doc.getFileName(), mimeType);
            
            // 设置 CORS 头（允许 OnlyOffice 容器访问）
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            
            response.setContentType(mimeType);
            response.setHeader("Content-Disposition", "inline; filename=\"" +
                    URLEncoder.encode(correctedFileName, StandardCharsets.UTF_8) + "\"");
            
            // 设置缓存控制（避免 OnlyOffice 缓存错误内容）
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 输出文件内容
            long totalBytes = 0;
            try (InputStream inputStream = minioService.downloadFile(objectName);
                    OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                outputStream.flush();
            }

            log.info("OnlyOffice 文件代理请求成功: id={}, fileName={}, fileSize={}, mimeType={}, bytesSent={}", 
                id, doc.getFileName(), doc.getFileSize(), mimeType, totalBytes);
        } catch (Exception e) {
            log.error("OnlyOffice 文件代理失败: id={}, error={}", id, e.getMessage(), e);
            // 确保在错误时也设置 CORS 头
            response.setHeader("Access-Control-Allow-Origin", "*");
            try {
                // 如果响应还没有提交，发送错误响应
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"获取文档内容失败: " + e.getMessage() + "\"}");
                    response.getWriter().flush();
                }
            } catch (Exception sendError) {
                log.error("发送错误响应失败", sendError);
            }
        }
    }

    /**
     * 根据文件内容检测实际的 MIME 类型
     * 通过读取文件头（魔数）来判断文件类型，而不是依赖数据库中的 MIME 类型
     */
    private String detectMimeTypeFromContent(DocumentDTO doc, MinioService minioService, String objectName) {
        try {
            // 读取文件的前几个字节来检测文件类型
            byte[] header = new byte[8];
            try (InputStream inputStream = minioService.downloadFile(objectName)) {
                int bytesRead = inputStream.read(header);
                if (bytesRead < 4) {
                    log.warn("文件太小，无法检测类型: documentId={}", doc.getId());
                    return null;
                }
            }
            
            // 检测 ZIP 格式（docx, xlsx, pptx 都是 ZIP 格式）
            // ZIP 文件头：50 4B 03 04
            if (header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04) {
                // 这是 ZIP 格式，可能是 docx, xlsx, pptx
                // 根据文件名扩展名判断
                String fileExt = doc.getFileType() != null ? doc.getFileType().toLowerCase() : "";
                String fileName = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";
                if (fileExt.equals("doc") || fileExt.equals("docx") || fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                    log.info("检测到文件实际为 docx 格式（ZIP）: documentId={}, fileName={}", doc.getId(), doc.getFileName());
                    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                } else if (fileExt.equals("xls") || fileExt.equals("xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                    log.info("检测到文件实际为 xlsx 格式（ZIP）: documentId={}, fileName={}", doc.getId(), doc.getFileName());
                    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                } else if (fileExt.equals("ppt") || fileExt.equals("pptx") || fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                    log.info("检测到文件实际为 pptx 格式（ZIP）: documentId={}, fileName={}", doc.getId(), doc.getFileName());
                    return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                }
            }
            
            // 检测旧的 Office 格式（doc, xls, ppt）
            // MS Office 97-2003 文件头：D0 CF 11 E0 A1 B1 1A E1
            if (header[0] == (byte)0xD0 && header[1] == (byte)0xCF && header[2] == 0x11 && header[3] == (byte)0xE0) {
                String fileExt = doc.getFileType() != null ? doc.getFileType().toLowerCase() : "";
                String fileName = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";
                if (fileExt.equals("doc") || fileName.endsWith(".doc")) {
                    log.info("检测到文件实际为 doc 格式（MS Office 97-2003）: documentId={}, fileName={}", doc.getId(), doc.getFileName());
                    return "application/msword";
                } else if (fileExt.equals("xls") || fileName.endsWith(".xls")) {
                    return "application/vnd.ms-excel";
                } else if (fileExt.equals("ppt") || fileName.endsWith(".ppt")) {
                    return "application/vnd.ms-powerpoint";
                }
            }
            
            log.debug("无法从文件内容检测 MIME 类型，使用数据库中的值: documentId={}", doc.getId());
            return null;
        } catch (Exception e) {
            log.warn("检测文件 MIME 类型失败: documentId={}, error={}", doc.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * 根据 MIME 类型修正文件名扩展名
     * 如果文件名扩展名与实际 MIME 类型不匹配，修正为正确的扩展名
     */
    private String correctFileNameByMimeType(String fileName, String mimeType) {
        if (fileName == null || mimeType == null) {
            return fileName;
        }
        
        // MIME 类型到扩展名的映射
        String correctExt = null;
        if (mimeType.contains("wordprocessingml.document")) {
            // docx 格式
            correctExt = "docx";
        } else if (mimeType.contains("spreadsheetml.sheet")) {
            // xlsx 格式
            correctExt = "xlsx";
        } else if (mimeType.contains("presentationml.presentation")) {
            // pptx 格式
            correctExt = "pptx";
        } else if (mimeType.contains("msword")) {
            // 旧的 doc 格式
            correctExt = "doc";
        } else if (mimeType.contains("ms-excel")) {
            // 旧的 xls 格式
            correctExt = "xls";
        } else if (mimeType.contains("ms-powerpoint")) {
            // 旧的 ppt 格式
            correctExt = "ppt";
        }
        
        if (correctExt != null) {
            // 检查当前文件名扩展名
            String currentExt = "";
            if (fileName.contains(".")) {
                currentExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            }
            
            // 如果扩展名不匹配，修正文件名
            if (!currentExt.equals(correctExt)) {
                String baseName = fileName.contains(".") 
                    ? fileName.substring(0, fileName.lastIndexOf(".")) 
                    : fileName;
                String corrected = baseName + "." + correctExt;
                log.info("修正文件名扩展名: original={}, mimeType={}, corrected={}", 
                    fileName, mimeType, corrected);
                return corrected;
            }
        }
        
        return fileName;
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个 IP 的情况（X-Forwarded-For 可能包含多个 IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
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
