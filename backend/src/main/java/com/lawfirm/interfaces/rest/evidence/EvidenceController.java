package com.lawfirm.interfaces.rest.evidence;

import com.lawfirm.application.document.service.FileAccessService;
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
import com.lawfirm.common.util.FileHashUtil;
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.infrastructure.config.OnlyOfficeConfig;
import com.lawfirm.infrastructure.external.document.DocumentContentExtractor;
import com.lawfirm.infrastructure.external.file.FileTypeService;
import com.lawfirm.infrastructure.external.file.ThumbnailService;
import com.lawfirm.infrastructure.external.minio.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 证据管理接口 */
@Slf4j
@RestController
@RequestMapping("/evidence")
@RequiredArgsConstructor
public class EvidenceController {

  /** 预签名URL有效期（秒）：1小时 */
  private static final int PRESIGNED_URL_EXPIRY_SECONDS = 3600;

  /** HTTP状态码：404 Not Found */
  private static final int HTTP_STATUS_NOT_FOUND = 404;

  /** HTTP状态码：500 Internal Server Error */
  private static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

  /** 证据应用服务 */
  private final EvidenceAppService evidenceAppService;

  /** 证据导出服务 */
  private final EvidenceExportService evidenceExportService;

  /** MinIO服务 */
  private final MinioService minioService;

  /** 文档内容提取器 */
  private final DocumentContentExtractor documentContentExtractor;

  /** 文件类型服务 */
  private final FileTypeService fileTypeService;

  /** 缩略图服务 */
  private final ThumbnailService thumbnailService;

  /** 文件访问服务 */
  @SuppressWarnings("unused")
  private final FileAccessService fileAccessService;

  /** OnlyOffice 配置 */
  private final OnlyOfficeConfig onlyOfficeConfig;

  /**
   * 分页查询证据
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("evidence:view")
  public Result<PageResult<EvidenceDTO>> list(final EvidenceQueryDTO query) {
    return Result.success(evidenceAppService.listEvidence(query));
  }

  /**
   * 获取证据详情
   *
   * @param id 证据ID
   * @return 证据信息
   */
  @GetMapping("/{id}")
  @RequirePermission("evidence:view")
  public Result<EvidenceDTO> getById(@PathVariable final Long id) {
    return Result.success(evidenceAppService.getEvidenceById(id));
  }

  /**
   * 创建证据
   *
   * @param command 创建证据命令
   * @return 证据信息
   */
  @PostMapping
  @RequirePermission("evidence:create")
  @OperationLog(module = "证据管理", action = "添加证据")
  public Result<EvidenceDTO> create(@Valid @RequestBody final CreateEvidenceCommand command) {
    return Result.success(evidenceAppService.createEvidence(command));
  }

  /**
   * 更新证据
   *
   * @param id 证据ID
   * @param command 更新证据命令
   * @return 证据信息
   */
  @PutMapping("/{id}")
  @RequirePermission("evidence:update")
  @OperationLog(module = "证据管理", action = "更新证据")
  public Result<EvidenceDTO> update(
      @PathVariable final Long id, @Valid @RequestBody final UpdateEvidenceCommand command) {
    return Result.success(evidenceAppService.updateEvidence(id, command));
  }

  /**
   * 删除证据
   *
   * @param id 证据ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("evidence:delete")
  @OperationLog(module = "证据管理", action = "删除证据")
  public Result<Void> delete(@PathVariable final Long id) {
    evidenceAppService.deleteEvidence(id);
    return Result.success();
  }

  /**
   * 调整排序
   *
   * @param id 证据ID
   * @param sortOrder 排序顺序
   * @return 空结果
   */
  @PutMapping("/{id}/sort")
  @RequirePermission("evidence:update")
  public Result<Void> updateSort(
      @PathVariable final Long id, @RequestParam final Integer sortOrder) {
    evidenceAppService.updateSortOrder(id, sortOrder);
    return Result.success();
  }

  /**
   * 批量调整分组
   *
   * @param ids 证据ID列表
   * @param groupName 分组名称
   * @return 空结果
   */
  @PostMapping("/batch-group")
  @RequirePermission("evidence:update")
  @OperationLog(module = "证据管理", action = "批量调整分组")
  public Result<Void> batchUpdateGroup(
      @RequestBody @jakarta.validation.constraints.Size(min = 1, max = 100, message = "批量操作数量需在1-100之间")
          final List<Long> ids,
      @RequestParam @jakarta.validation.constraints.Size(max = 100, message = "分组名称不能超过100个字符")
          final String groupName) {
    evidenceAppService.batchUpdateGroup(ids, groupName);
    return Result.success();
  }

  /**
   * 添加质证记录
   *
   * @param id 证据ID
   * @param command 创建质证记录命令
   * @return 质证记录信息
   */
  @PostMapping("/{id}/cross-exam")
  @RequirePermission("evidence:crossExam")
  @OperationLog(module = "证据管理", action = "添加质证记录")
  public Result<EvidenceCrossExamDTO> addCrossExam(
      @PathVariable final Long id, @Valid @RequestBody final CreateCrossExamCommand command) {
    command.setEvidenceId(id);
    return Result.success(evidenceAppService.addCrossExam(command));
  }

  /**
   * 完成质证
   *
   * @param id 证据ID
   * @return 空结果
   */
  @PostMapping("/{id}/complete-cross-exam")
  @RequirePermission("evidence:crossExam")
  @OperationLog(module = "证据管理", action = "完成质证")
  public Result<Void> completeCrossExam(@PathVariable final Long id) {
    evidenceAppService.completeCrossExam(id);
    return Result.success();
  }

  /**
   * 按案件获取证据列表
   *
   * @param matterId 案件ID
   * @return 证据列表
   */
  @GetMapping("/matter/{matterId}")
  @RequirePermission("evidence:view")
  public Result<List<EvidenceDTO>> getByMatter(@PathVariable final Long matterId) {
    return Result.success(evidenceAppService.getEvidenceByMatter(matterId));
  }

  /**
   * 获取案件的证据分组
   *
   * @param matterId 案件ID
   * @return 分组列表
   */
  @GetMapping("/matter/{matterId}/groups")
  @RequirePermission("evidence:view")
  public Result<List<String>> getGroups(@PathVariable final Long matterId) {
    return Result.success(evidenceAppService.getEvidenceGroups(matterId));
  }

  /**
   * 上传证据文件
   *
   * <p>注意：证据文件上传时需要matterId，因为文件需要按项目组织
   *
   * @param file 文件
   * @param matterId 案件ID
   * @return 上传结果
   */
  @PostMapping("/upload")
  @RequirePermission("evidence:create")
  @OperationLog(module = "证据管理", action = "上传证据文件")
  public Result<Map<String, Object>> uploadFile(
      @RequestParam("file") final MultipartFile file,
      @RequestParam(value = "matterId", required = false) final Long matterId) {
    try {
      // ✅ 安全验证：使用 FileValidator 验证文件（防止恶意文件）
      FileValidator.ValidationResult securityResult = FileValidator.validate(file);
      if (!securityResult.isValid()) {
        log.warn(
            "证据文件安全验证失败: {}, 原因: {}", file.getOriginalFilename(), securityResult.getErrorMessage());
        throw new com.lawfirm.common.exception.BusinessException(securityResult.getErrorMessage());
      }

      // 业务验证（文件类型）
      String validationError = fileTypeService.validateFile(file);
      if (validationError != null) {
        throw new com.lawfirm.common.exception.BusinessException(validationError);
      }

      // 1. 计算文件Hash
      String fileHash = FileHashUtil.calculateHash(file);
      log.debug("证据文件Hash计算完成: hash={}, fileName={}", fileHash, file.getOriginalFilename());

      // 2. 生成标准化存储路径（新规则）
      // 注意：如果matterId为空，使用临时路径（后续创建证据时会更新）
      String standardStoragePath;
      if (matterId != null) {
        standardStoragePath =
            MinioPathGenerator.generateStandardPath(
                MinioPathGenerator.FileType.EVIDENCE, matterId, "证据材料");
        // 2.1 验证存储路径格式（确保包含项目ID）
        MinioPathGenerator.validateStoragePath(
            standardStoragePath, MinioPathGenerator.FileType.EVIDENCE, matterId);
      } else {
        // matterId为空时，使用临时路径（后续需要迁移）
        // 注意：临时路径不进行校验，但会在创建证据时要求matterId
        standardStoragePath = "evidence/temp/";
      }

      // 3. 生成物理文件名
      String originalFilename = file.getOriginalFilename();
      String physicalName = MinioPathGenerator.generatePhysicalName(originalFilename);

      // 4. 构建完整对象名称
      String objectName = MinioPathGenerator.buildObjectName(standardStoragePath, physicalName);

      // 5. 上传文件到 MinIO（使用新路径）
      String newFileUrl;
      try (java.io.InputStream inputStream = file.getInputStream()) {
        newFileUrl = minioService.uploadFile(inputStream, objectName, file.getContentType());
      }

      // 6. 构建完整URL（用于file_url字段，双写策略）
      String fileUrl = minioService.buildFileUrl(objectName);

      // 7. 获取文件类型信息
      FileTypeService.FileTypeInfo typeInfo = fileTypeService.getFileTypeInfo(originalFilename);

      // 8. 生成缩略图（仅图片文件）
      String thumbnailUrl = null;
      if (typeInfo.isCanGenerateThumbnail()) {
        thumbnailUrl = thumbnailService.generateThumbnail(file, newFileUrl);
      }

      Map<String, Object> result = new HashMap<>();
      result.put("fileUrl", fileUrl); // 返回完整URL（兼容前端）
      result.put("fileName", originalFilename);
      result.put("fileSize", file.getSize());
      result.put("fileType", typeInfo.getType());
      result.put("thumbnailUrl", thumbnailUrl);
      result.put("canPreview", typeInfo.isCanPreview());
      // 新增字段（供前端创建证据时使用）
      result.put("bucketName", minioService.getBucketName());
      result.put("storagePath", standardStoragePath);
      result.put("physicalName", physicalName);
      result.put("fileHash", fileHash);

      log.info(
          "证据文件上传成功（新路径）: fileName={}, storagePath={}, physicalName={}, hash={}",
          originalFilename,
          standardStoragePath,
          physicalName,
          fileHash);
      return Result.success(result);
    } catch (com.lawfirm.common.exception.BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("证据文件上传失败", e);
      throw new com.lawfirm.common.exception.BusinessException("文件上传失败，请稍后重试");
    }
  }

  /**
   * 获取文件预览URL（预签名URL，有效期1小时）
   *
   * @param id 证据ID
   * @return 预览URL信息
   */
  @GetMapping("/{id}/preview")
  @RequirePermission("evidence:view")
  public Result<Map<String, Object>> getPreviewUrl(@PathVariable final Long id) {
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
        String presignedUrl =
            minioService.getPresignedUrl(objectName, PRESIGNED_URL_EXPIRY_SECONDS);
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
   *
   * @param id 证据ID
   * @return 下载URL信息
   */
  @GetMapping("/{id}/download-url")
  @RequirePermission("evidence:view")
  public Result<Map<String, String>> getDownloadUrl(@PathVariable final Long id) {
    EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
    if (evidence.getFileUrl() == null) {
      throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
    }

    Map<String, String> result = new HashMap<>();
    try {
      String objectName = minioService.extractObjectName(evidence.getFileUrl());
      if (objectName != null) {
        String presignedUrl =
            minioService.getPresignedUrl(objectName, PRESIGNED_URL_EXPIRY_SECONDS);
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
   *
   * @param ids 证据ID列表
   * @param response HTTP响应
   */
  @PostMapping("/batch-download")
  @RequirePermission("evidence:download")
  @OperationLog(module = "证据管理", action = "批量下载证据")
  @Operation(summary = "批量下载证据", description = "将多个证据文件打包为 ZIP 文件下载")
  public void batchDownload(@RequestBody final List<Long> ids, final HttpServletResponse response) {
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
      response.setHeader(
          "Content-Disposition",
          "attachment; filename=\""
              + java.net.URLEncoder.encode(zipFileName, java.nio.charset.StandardCharsets.UTF_8)
              + "\"");
      response.setContentLength(zipData.length);
      response.getOutputStream().write(zipData);
      response.getOutputStream().flush();

      log.info("证据批量下载完成: {} 个文件, 大小: {} bytes", filesMap.size(), zipData.length);
    } catch (Exception e) {
      log.error("证据批量下载失败", e);
      throw new com.lawfirm.common.exception.BusinessException("批量下载失败，请稍后重试");
    }
  }

  /**
   * 获取 OnlyOffice 预览URL（Docker 容器可访问的预签名URL）
   *
   * @param id 证据ID
   * @return OnlyOffice URL信息
   */
  @GetMapping("/{id}/onlyoffice-url")
  @RequirePermission("evidence:view")
  public Result<Map<String, Object>> getOnlyOfficeUrl(@PathVariable final Long id) {
    EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
    if (evidence.getFileUrl() == null) {
      throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
    }

    FileTypeService.FileTypeInfo typeInfo = fileTypeService.getFileTypeInfo(evidence.getFileName());

    Map<String, Object> result = new HashMap<>();
    // 使用后端代理 URL，OnlyOffice 通过 Docker 内部网络下载文件
    // 使用配置的 callbackUrl 作为基础（默认为 http://backend:8080/api）
    String baseUrl = onlyOfficeConfig.getCallbackUrl();
    // 移除末尾的 /api（如果有）
    if (baseUrl.endsWith("/api")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
    }
    String proxyUrl = baseUrl + "/api/evidence/" + id + "/file-proxy";
    result.put("fileUrl", proxyUrl);
    result.put("fileName", evidence.getFileName());
    result.put("fileType", typeInfo.getType());
    return Result.success(result);
  }

  /**
   * 文件代理接口（供 OnlyOffice 容器下载文件）
   *
   * @param id 证据ID
   * @param response HTTP响应
   */
  @GetMapping("/{id}/file-proxy")
  @RequirePermission("evidence:view")
  public void fileProxy(@PathVariable final Long id, final HttpServletResponse response) {
    try {
      // 获取证据实体（用于FileAccessService）
      com.lawfirm.domain.evidence.entity.Evidence evidence =
          evidenceAppService.getEvidenceEntityById(id);
      EvidenceDTO evidenceDTO = evidenceAppService.getEvidenceById(id);

      if (evidenceDTO.getFileUrl() == null) {
        response.sendError(HTTP_STATUS_NOT_FOUND, "文件不存在");
        return;
      }

      // 优先使用新字段构建对象名，回退到file_url
      String objectName = null;
      if (evidence.getStoragePath() != null && evidence.getPhysicalName() != null) {
        // 使用新字段构建对象名
        objectName =
            MinioPathGenerator.buildObjectName(
                evidence.getStoragePath(), evidence.getPhysicalName());
        log.debug(
            "证据文件代理（使用新字段）: evidenceId={}, storagePath={}, physicalName={}, objectName={}",
            id,
            evidence.getStoragePath(),
            evidence.getPhysicalName(),
            objectName);
      } else {
        // 回退到file_url
        objectName = minioService.extractObjectName(evidenceDTO.getFileUrl());
        log.debug(
            "证据文件代理（使用file_url）: evidenceId={}, fileUrl={}, objectName={}",
            id,
            evidenceDTO.getFileUrl(),
            objectName);
      }

      if (objectName == null) {
        response.sendError(HTTP_STATUS_NOT_FOUND, "文件路径无效");
        return;
      }

      byte[] fileBytes = minioService.downloadFileAsBytes(objectName);

      // 设置响应头
      String contentType = getContentType(evidenceDTO.getFileName());
      response.setContentType(contentType);
      response.setContentLength(fileBytes.length);
      // 安全处理文件名，防止 HTTP 响应拆分攻击
      String safeFileName = evidenceDTO.getFileName()
          .replaceAll("[\\r\\n\"\\\\]", "_");  // 移除危险字符
      String encodedFileName = java.net.URLEncoder.encode(safeFileName, 
          java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
      response.setHeader(
          "Content-Disposition", 
          "inline; filename=\"" + safeFileName + "\"; filename*=UTF-8''" + encodedFileName);

      // 写入文件内容
      response.getOutputStream().write(fileBytes);
      response.getOutputStream().flush();
    } catch (Exception e) {
      log.error("文件代理失败", e);
      try {
        response.sendError(HTTP_STATUS_INTERNAL_SERVER_ERROR, "文件下载失败");
      } catch (Exception sendError) {
        log.debug("发送错误响应失败", sendError);
      }
    }
  }

  private String getContentType(final String fileName) {
    if (fileName == null) {
      return "application/octet-stream";
    }
    String ext = fileName.toLowerCase();
    if (ext.endsWith(".docx")) {
      return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }
    if (ext.endsWith(".doc")) {
      return "application/msword";
    }
    if (ext.endsWith(".xlsx")) {
      return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
    if (ext.endsWith(".xls")) {
      return "application/vnd.ms-excel";
    }
    if (ext.endsWith(".pptx")) {
      return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    }
    if (ext.endsWith(".ppt")) {
      return "application/vnd.ms-powerpoint";
    }
    if (ext.endsWith(".pdf")) {
      return "application/pdf";
    }
    return "application/octet-stream";
  }

  /**
   * 获取文件缩略图URL
   *
   * @param id 证据ID
   * @return 缩略图URL信息
   */
  @GetMapping("/{id}/thumbnail")
  @RequirePermission("evidence:view")
  public Result<Map<String, Object>> getThumbnailUrl(@PathVariable final Long id) {
    EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
    if (evidence.getFileUrl() == null) {
      throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
    }

    FileTypeService.FileTypeInfo typeInfo = fileTypeService.getFileTypeInfo(evidence.getFileName());

    Map<String, Object> result = new HashMap<>();
    if (fileTypeService.isImageFile(evidence.getFileName())) {
      // 图片文件：返回缩略图URL或原文件URL，转换为浏览器可访问的 URL
      String thumbnailUrl =
          evidence.getThumbnailUrl() != null ? evidence.getThumbnailUrl() : evidence.getFileUrl();
      if (thumbnailUrl != null) {
        thumbnailUrl = minioService.getBrowserAccessibleUrl(thumbnailUrl);
      }
      result.put("thumbnailUrl", thumbnailUrl);
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
   *
   * @param fileName 文件名
   * @return 文件类型
   */
  private String getFileType(final String fileName) {
    return fileTypeService.getFileTypeInfo(fileName).getType();
  }

  /**
   * 获取文件文本内容（用于Word/Excel等文档预览）
   *
   * @param id 证据ID
   * @return 文件内容信息
   */
  @GetMapping("/{id}/content")
  @RequirePermission("evidence:view")
  @OperationLog(module = "证据管理", action = "获取文件内容")
  public Result<Map<String, Object>> getFileContent(@PathVariable final Long id) {
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
      result.put("content", "文档内容提取失败，请稍后重试");
      result.put("supported", false);
    }

    return Result.success(result);
  }

  /**
   * 导出证据清单为 Word 或 PDF 文档
   *
   * @param matterId 案件ID
   * @param format 导出格式
   * @param items 导出项列表
   * @param response HTTP响应
   */
  @PostMapping("/matter/{matterId}/export")
  @RequirePermission("evidence:view")
  @OperationLog(module = "证据管理", action = "导出证据清单")
  public void exportEvidenceList(
      @PathVariable final Long matterId,
      @RequestParam(defaultValue = "word") final String format,
      @RequestBody(required = false) final List<EvidenceExportItemDTO> items,
      final HttpServletResponse response) {
    byte[] content;
    String contentType;
    String extension;

    try {
      // 先生成文档内容（可能抛出异常）
      if ("word".equalsIgnoreCase(format) || "docx".equalsIgnoreCase(format)) {
        content = evidenceExportService.exportToWord(matterId, items);
        contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        extension = "docx";
      } else if ("pdf".equalsIgnoreCase(format)) {
        content = evidenceExportService.exportToPdf(matterId, items);
        contentType = "application/pdf";
        extension = "pdf";
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"暂不支持的导出格式: " + format + "\"}");
        return;
      }
    } catch (Exception e) {
      log.error("导出证据清单失败", e);
      try {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"导出失败，请稍后重试\"}");
      } catch (Exception writeError) {
        log.debug("写入错误响应失败", writeError);
      }
      return;
    }

    // 写入响应（此时不会再抛出业务异常）
    try {
      String fileName = evidenceExportService.getExportFileName(matterId, extension);

      response.setContentType(contentType);
      response.setHeader(
          "Content-Disposition",
          "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8") + "\"");
      response.setContentLength(content.length);
      response.getOutputStream().write(content);
      response.getOutputStream().flush();
    } catch (Exception e) {
      log.error("写入导出文件响应失败", e);
    }
  }

  /**
   * 导出证据清单（GET方式，导出全部）
   *
   * @param matterId 案件ID
   * @param format 导出格式
   * @param response HTTP响应
   */
  @GetMapping("/matter/{matterId}/export")
  @RequirePermission("evidence:view")
  @OperationLog(module = "证据管理", action = "导出证据清单")
  public void exportEvidenceListGet(
      @PathVariable final Long matterId,
      @RequestParam(defaultValue = "word") final String format,
      final HttpServletResponse response) {
    exportEvidenceList(matterId, format, null, response);
  }
}
