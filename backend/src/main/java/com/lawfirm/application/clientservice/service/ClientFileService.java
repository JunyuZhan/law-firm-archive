package com.lawfirm.application.clientservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.clientservice.dto.ClientFileDTO;
import com.lawfirm.application.clientservice.dto.ClientFileReceiveRequest;
import com.lawfirm.application.clientservice.dto.ClientFileSyncRequest;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.domain.clientservice.entity.ClientFile;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.repository.DocumentRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import com.lawfirm.infrastructure.persistence.mapper.clientservice.ClientFileMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/** 客户文件服务 处理客服系统推送的客户上传文件. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientFileService {

  /** 客户文件Mapper. */
  private final ClientFileMapper clientFileMapper;

  /** 案件Mapper. */
  private final MatterMapper matterMapper;

  /** 外部集成Mapper. */
  private final ExternalIntegrationMapper integrationMapper;

  /** 用户Mapper. */
  private final UserMapper userMapper;

  /** MinIO服务. */
  private final MinioService minioService;

  /** 文档仓储. */
  private final DocumentRepository documentRepository;

  /** 客户服务类型. */
  private static final String CLIENT_SERVICE_TYPE = "CLIENT_SERVICE";

  /** 文件类别名称映射. */
  private static final Map<String, String> CATEGORY_NAMES =
      Map.of(
          ClientFile.CATEGORY_EVIDENCE, "证据材料",
          ClientFile.CATEGORY_CONTRACT, "合同文件",
          ClientFile.CATEGORY_ID_CARD, "身份证件",
          ClientFile.CATEGORY_OTHER, "其他");

  /** 状态名称映射. */
  private static final Map<String, String> STATUS_NAMES =
      Map.of(
          ClientFile.STATUS_PENDING, "待同步",
          ClientFile.STATUS_SYNCED, "已同步",
          ClientFile.STATUS_DELETED, "已删除",
          ClientFile.STATUS_FAILED, "同步失败");

  /**
   * 接收客服系统推送的客户文件.
   *
   * @param request 客户文件接收请求
   * @return 客户文件DTO
   */
  @Transactional
  public ClientFileDTO receiveFile(final ClientFileReceiveRequest request) {
    // 1. 验证项目存在
    Matter matter = matterMapper.selectById(request.getMatterId());
    if (matter == null || matter.getDeleted()) {
      throw new BusinessException("项目不存在");
    }

    // 2. 检查是否已存在（防止重复推送）
    ClientFile existing = clientFileMapper.selectByExternalFileId(request.getExternalFileId());
    if (existing != null) {
      log.info("文件已存在，跳过: {}", request.getExternalFileId());
      return convertToDTO(existing, matter);
    }

    // 3. 创建文件记录
    ClientFile clientFile =
        ClientFile.builder()
            .matterId(request.getMatterId())
            .clientId(request.getClientId())
            .clientName(request.getClientName())
            .fileName(request.getFileName())
            .originalFileName(request.getFileName())
            .fileSize(request.getFileSize())
            .fileType(request.getFileType())
            .fileCategory(
                request.getFileCategory() != null
                    ? request.getFileCategory()
                    : ClientFile.CATEGORY_OTHER)
            .description(request.getDescription())
            .externalFileId(request.getExternalFileId())
            .externalFileUrl(request.getExternalFileUrl())
            .uploadedBy(request.getUploadedBy())
            .uploadedAt(
                request.getUploadedAt() != null ? request.getUploadedAt() : LocalDateTime.now())
            .status(ClientFile.STATUS_PENDING)
            .build();

    clientFileMapper.insert(clientFile);
    log.info("接收客户文件成功: matterId={}, fileName={}", request.getMatterId(), request.getFileName());

    return convertToDTO(clientFile, matter);
  }

  /**
   * 获取项目的客户文件列表.
   *
   * @param matterId 项目ID
   * @param status 状态
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 分页结果
   */
  public PageResult<ClientFileDTO> getClientFiles(
      final Long matterId, final String status, final int pageNum, final int pageSize) {
    Matter matter = matterMapper.selectById(matterId);

    Page<ClientFile> page = new Page<>(pageNum, pageSize);
    var resultPage = clientFileMapper.selectPage(page, matterId, status);

    List<ClientFileDTO> list =
        resultPage.getRecords().stream()
            .map(f -> convertToDTO(f, matter))
            .collect(Collectors.toList());

    return PageResult.of(list, resultPage.getTotal(), pageNum, pageSize);
  }

  /**
   * 获取项目待同步的文件列表.
   *
   * @param matterId 项目ID
   * @return 客户文件DTO列表
   */
  public List<ClientFileDTO> getPendingFiles(final Long matterId) {
    Matter matter = matterMapper.selectById(matterId);
    List<ClientFile> files = clientFileMapper.selectPendingByMatterId(matterId);
    return files.stream().map(f -> convertToDTO(f, matter)).collect(Collectors.toList());
  }

  /**
   * 统计项目待同步文件数量.
   *
   * @param matterId 项目ID
   * @return 待同步文件数量
   */
  public int countPendingFiles(final Long matterId) {
    return clientFileMapper.countPendingByMatterId(matterId);
  }

  /**
   * 同步文件到卷宗 从外部URL下载文件，上传到MinIO，并创建文档记录关联到卷宗目录项.
   *
   * @param request 客户文件同步请求
   * @param operatorId 操作人ID
   * @return 客户文件DTO
   */
  @Transactional
  public ClientFileDTO syncToFolder(final ClientFileSyncRequest request, final Long operatorId) {
    // 1. 获取客户文件记录
    ClientFile clientFile = clientFileMapper.selectById(request.getFileId());
    if (clientFile == null || clientFile.getDeleted()) {
      throw new BusinessException("文件不存在");
    }

    if (!ClientFile.STATUS_PENDING.equals(clientFile.getStatus())) {
      throw new BusinessException("该文件已同步或已删除");
    }

    try {
      // 2. 从外部URL下载文件
      byte[] fileContent = downloadFile(clientFile.getExternalFileUrl());
      if (fileContent == null || fileContent.length == 0) {
        throw new BusinessException("下载文件失败：文件内容为空");
      }

      // 3. 构建存储路径并上传到MinIO（防止路径遍历攻击）
      String storagePath = String.format("matters/%d/client_uploads/", clientFile.getMatterId());
      String safeFileName = MinioPathGenerator.sanitizeFilename(clientFile.getFileName());
      String fileName = UUID.randomUUID().toString() + "_" + safeFileName;
      String objectName = storagePath + fileName;

      String fileUrl =
          minioService.uploadFile(
              new ByteArrayInputStream(fileContent), objectName, clientFile.getFileType());

      // 4. 创建文档记录并关联到卷宗目录项
      Document document =
          Document.builder()
              .docNo("DOC" + System.currentTimeMillis())
              .title(clientFile.getOriginalFileName())
              .matterId(clientFile.getMatterId())
              .fileName(clientFile.getOriginalFileName())
              .filePath(fileUrl)
              .fileSize(clientFile.getFileSize())
              .fileType(getFileExtension(clientFile.getFileName()))
              .mimeType(clientFile.getFileType())
              .version(1)
              .isLatest(true)
              .securityLevel("INTERNAL")
              .status("ACTIVE")
              .description(clientFile.getDescription())
              .dossierItemId(request.getTargetDossierId())
              .folderPath("client_uploads")
              .sourceType("CLIENT_PORTAL") // 标记来源为客户门户
              .createdBy(operatorId)
              .build();

      documentRepository.save(document);
      log.info("文档记录已创建: documentId={}, fileName={}", document.getId(), document.getFileName());

      // 5. 更新客户文件状态为已同步
      clientFileMapper.updateSyncStatus(
          clientFile.getId(),
          ClientFile.STATUS_SYNCED,
          document.getId(), // 关联本地文档ID
          request.getTargetDossierId(),
          operatorId,
          null);

      // 6. 通知客服系统删除文件
      notifyClientServiceToDelete(clientFile.getExternalFileId());

      log.info("文件同步成功: clientFileId={}, documentId={}", clientFile.getId(), document.getId());

      clientFile.setStatus(ClientFile.STATUS_SYNCED);
      clientFile.setLocalDocumentId(document.getId());
      clientFile.setTargetDossierId(request.getTargetDossierId());
      clientFile.setSyncedAt(LocalDateTime.now());
      clientFile.setSyncedBy(operatorId);

      Matter matter = matterMapper.selectById(clientFile.getMatterId());
      return convertToDTO(clientFile, matter);

    } catch (BusinessException e) {
      log.error("文件同步失败: {}", e.getMessage());
      // 业务异常消息可以存储（已脱敏）
      clientFileMapper.updateSyncStatus(
          clientFile.getId(), ClientFile.STATUS_FAILED, null, null, operatorId, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("文件同步失败: {}", e.getMessage(), e);
      // 不存储原始异常消息，使用通用错误文案
      clientFileMapper.updateSyncStatus(
          clientFile.getId(), ClientFile.STATUS_FAILED, null, null, operatorId, "文件同步失败");
      throw new BusinessException("文件同步失败，请稍后重试");
    }
  }

  /**
   * 获取文件扩展名.
   *
   * @param filename 文件名
   * @return 文件扩展名
   */
  private String getFileExtension(final String filename) {
    if (filename == null || !filename.contains(".")) {
      return "";
    }
    return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
  }

  /**
   * 批量同步文件.
   *
   * @param requests 客户文件同步请求列表
   * @param operatorId 操作人ID
   * @return 客户文件DTO列表
   */
  @Transactional
  public List<ClientFileDTO> batchSync(
      final List<ClientFileSyncRequest> requests, final Long operatorId) {
    return requests.stream()
        .map(
            req -> {
              try {
                return syncToFolder(req, operatorId);
              } catch (Exception e) {
                log.error("批量同步文件失败: fileId={}, error={}", req.getFileId(), e.getMessage());
                return null;
              }
            })
        .filter(dto -> dto != null)
        .collect(Collectors.toList());
  }

  /**
   * 忽略/删除待同步文件.
   *
   * @param fileId 文件ID
   * @param operatorId 操作人ID
   */
  @Transactional
  public void ignoreFile(final Long fileId, final Long operatorId) {
    ClientFile clientFile = clientFileMapper.selectById(fileId);
    if (clientFile == null) {
      throw new BusinessException("文件不存在");
    }

    // 标记为已删除
    clientFileMapper.updateSyncStatus(
        fileId, ClientFile.STATUS_DELETED, null, null, operatorId, "用户忽略");

    // 通知客服系统删除
    notifyClientServiceToDelete(clientFile.getExternalFileId());
  }

  /**
   * 代理文件（解决跨域问题）.
   *
   * @param fileId 文件ID
   * @param response HTTP响应
   * @throws IOException IO异常
   */
  public void proxyFile(final Long fileId, final HttpServletResponse response) throws IOException {
    // 1. 获取文件信息
    ClientFile clientFile = clientFileMapper.selectById(fileId);
    if (clientFile == null || clientFile.getDeleted()) {
      response.setHeader("Access-Control-Allow-Origin", "*");
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
      return;
    }

    // 2. 验证外部URL
    if (clientFile.getExternalFileUrl() == null
        || clientFile.getExternalFileUrl().trim().isEmpty()) {
      log.warn("文件外部URL为空: fileId={}", fileId);
      response.setHeader("Access-Control-Allow-Origin", "*");
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "文件URL无效");
      return;
    }

    // 3. 从外部URL下载文件
    byte[] fileContent;
    try {
      fileContent = downloadFile(clientFile.getExternalFileUrl());
      if (fileContent == null || fileContent.length == 0) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件内容为空");
        return;
      }
    } catch (Exception e) {
      log.error(
          "代理文件失败: fileId={}, url={}, error={}",
          fileId,
          clientFile.getExternalFileUrl(),
          e.getMessage(),
          e);
      response.setHeader("Access-Control-Allow-Origin", "*");
      // 不暴露内部异常消息，使用通用错误文案
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "下载文件失败，请稍后重试");
      return;
    }

    // 3. 设置响应头（包括CORS）
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
    response.setHeader("Access-Control-Allow-Headers", "*");
    response.setContentType(
        clientFile.getFileType() != null ? clientFile.getFileType() : "application/octet-stream");
    response.setContentLength(fileContent.length);

    // 文件名编码处理（支持中文文件名）
    String fileName = clientFile.getFileName() != null ? clientFile.getFileName() : "file";
    String encodedFileName =
        java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8);
    response.setHeader(
        "Content-Disposition",
        "inline; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

    // 4. 写入文件内容
    response.getOutputStream().write(fileContent);
    response.getOutputStream().flush();
  }

  /**
   * 下载文件.
   *
   * @param fileUrl 文件URL
   * @return 文件字节数组
   * @throws Exception 下载异常
   */
  private byte[] downloadFile(final String fileUrl) throws Exception {
    java.net.URI uri = java.net.URI.create(fileUrl);
    URL url = uri.toURL();
    try (InputStream is = url.openStream()) {
      return is.readAllBytes();
    }
  }

  /**
   * 通知客服系统删除文件.
   *
   * @param externalFileId 外部文件ID
   */
  private void notifyClientServiceToDelete(final String externalFileId) {
    try {
      ExternalIntegration integration = integrationMapper.selectByType(CLIENT_SERVICE_TYPE);
      if (integration == null || !Boolean.TRUE.equals(integration.getEnabled())) {
        log.warn("客服系统未配置或未启用，跳过删除通知");
        return;
      }

      RestTemplate restTemplate = new RestTemplate();

      // 构建请求头
      HttpHeaders headers = new HttpHeaders();
      if (integration.getApiKey() != null) {
        headers.set("Authorization", "Bearer " + integration.getApiKey());
      }

      // 构建API URL（使用Query参数）
      String baseUrl =
          integration.getApiUrl().endsWith("/")
              ? integration.getApiUrl().substring(0, integration.getApiUrl().length() - 1)
              : integration.getApiUrl();
      String apiUrl =
          baseUrl
              + "/api/matter/files/delete?fileId="
              + java.net.URLEncoder.encode(externalFileId, java.nio.charset.StandardCharsets.UTF_8);

      HttpEntity<Void> entity = new HttpEntity<>(headers);

      log.debug("通知客服系统删除文件: url={}, fileId={}", apiUrl, externalFileId);

      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(
              apiUrl,
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("通知客服系统删除文件成功: fileId={}", externalFileId);
      } else {
        log.warn("通知客服系统删除文件失败: fileId={}, status={}", externalFileId, response.getStatusCode());
      }
    } catch (Exception e) {
      // 包含堆栈信息以便排查问题
      log.error("通知客服系统删除文件异常: {}", e.getMessage(), e);
      // 不抛出异常，删除通知失败不影响主流程
    }
  }

  /**
   * 转换为 DTO.
   *
   * @param file 客户文件实体
   * @param matter 项目实体
   * @return 客户文件DTO
   */
  private ClientFileDTO convertToDTO(final ClientFile file, final Matter matter) {
    ClientFileDTO dto =
        ClientFileDTO.builder()
            .id(file.getId())
            .matterId(file.getMatterId())
            .matterName(matter != null ? matter.getName() : null)
            .clientId(file.getClientId())
            .clientName(file.getClientName())
            .fileName(file.getFileName())
            .originalFileName(file.getOriginalFileName())
            .fileSize(file.getFileSize())
            .fileType(file.getFileType())
            .fileCategory(file.getFileCategory())
            .fileCategoryName(CATEGORY_NAMES.getOrDefault(file.getFileCategory(), "其他"))
            .description(file.getDescription())
            .externalFileId(file.getExternalFileId())
            .externalFileUrl(file.getExternalFileUrl())
            .uploadedBy(file.getUploadedBy())
            .uploadedAt(file.getUploadedAt())
            .status(file.getStatus())
            .statusName(STATUS_NAMES.getOrDefault(file.getStatus(), file.getStatus()))
            .localDocumentId(file.getLocalDocumentId())
            .targetDossierId(file.getTargetDossierId())
            .syncedAt(file.getSyncedAt())
            .syncedBy(file.getSyncedBy())
            .errorMessage(file.getErrorMessage())
            .createdAt(file.getCreatedAt())
            .build();

    // 获取同步操作人名称
    if (file.getSyncedBy() != null) {
      User user = userMapper.selectById(file.getSyncedBy());
      if (user != null) {
        dto.setSyncedByName(user.getRealName());
      }
    }

    return dto;
  }
}
