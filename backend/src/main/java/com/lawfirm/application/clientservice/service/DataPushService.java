package com.lawfirm.application.clientservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.clientservice.dto.PortalMatterDTO;
import com.lawfirm.application.clientservice.dto.PushConfigDTO;
import com.lawfirm.application.clientservice.dto.PushRecordDTO;
import com.lawfirm.application.clientservice.dto.PushRequest;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.security.AesEncryptionService;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.clientservice.entity.PushConfig;
import com.lawfirm.domain.clientservice.entity.PushRecord;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterParticipantMapper;
import com.lawfirm.infrastructure.persistence.mapper.clientservice.PushConfigMapper;
import com.lawfirm.infrastructure.persistence.mapper.clientservice.PushRecordMapper;
import com.lawfirm.infrastructure.external.minio.MinioService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/** 数据推送服务 负责将项目数据推送到客户服务系统 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPushService {

  /** 默认有效期天数 */
  private static final int DEFAULT_VALID_DAYS = 30;

  /** 推送记录Mapper */
  private final PushRecordMapper pushRecordMapper;

  /** 推送配置Mapper */
  private final PushConfigMapper pushConfigMapper;

  /** 项目Mapper */
  private final MatterMapper matterMapper;

  /** 项目参与者Mapper */
  private final MatterParticipantMapper participantMapper;

  /** 客户Mapper */
  private final ClientMapper clientMapper;

  /** 外部集成Mapper */
  private final ExternalIntegrationMapper integrationMapper;

  /** 文档Mapper */
  private final DocumentMapper documentMapper;

  /** MinIO服务 */
  private final MinioService minioService;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /** 加密服务 */
  private final AesEncryptionService encryptionService;

  /** 客户服务系统的集成类型标识 */
  private static final String CLIENT_SERVICE_TYPE = "CLIENT_SERVICE";

  /** 不允许推送的项目状态 */
  private static final Set<String> DISABLED_STATUSES = Set.of("ARCHIVED", "CANCELLED");

  /**
   * 推送项目数据到客户服务系统.
   *
   * @param request 推送请求
   * @param operatorId 操作人ID
   * @return 推送记录DTO
   */
  @Transactional
  public PushRecordDTO pushMatterData(final PushRequest request, final Long operatorId) {
    // 1. 验证项目
    Matter matter = matterMapper.selectById(request.getMatterId());
    if (matter == null || matter.getDeleted()) {
      throw new BusinessException("项目不存在");
    }

    // 2. 数据权限校验 - 只有项目参与者才能推送
    checkMatterPermission(matter.getId(), operatorId);

    // 3. 项目状态校验
    if (DISABLED_STATUSES.contains(matter.getStatus())) {
      throw new BusinessException("该项目状态不允许推送数据");
    }

    // 4. 验证客户
    Long clientId = request.getClientId() != null ? request.getClientId() : matter.getClientId();
    if (clientId == null) {
      throw new BusinessException("项目未关联客户");
    }

    Client client = clientMapper.selectById(clientId);
    if (client == null) {
      throw new BusinessException("客户不存在");
    }

    // 2. 获取客户服务系统配置
    ExternalIntegration integration = getClientServiceIntegration();

    // 3. 组装推送数据（脱敏）
    PortalMatterDTO matterData =
        buildMatterData(matter, new HashSet<>(request.getScopes()), request.getDocumentIds());

    // 4. 创建推送记录
    PushRecord record =
        PushRecord.builder()
            .matterId(matter.getId())
            .clientId(clientId)
            .pushType(
                request.getPushType() != null ? request.getPushType() : PushRecord.TYPE_MANUAL)
            .scopes(String.join(",", request.getScopes()))
            .status(PushRecord.STATUS_PENDING)
            .retryCount(0)
            .expiresAt(
                LocalDateTime.now()
                    .plusDays(
                        request.getValidDays() != null
                            ? request.getValidDays()
                            : DEFAULT_VALID_DAYS))
            .createdBy(operatorId)
            .build();

    // 保存数据快照
    try {
      record.setDataSnapshot(objectMapper.writeValueAsString(matterData));
    } catch (Exception e) {
      log.warn("序列化数据快照失败", e);
    }

    pushRecordMapper.insert(record);

    // 5. 调用客户服务系统API
    if (integration != null && Boolean.TRUE.equals(integration.getEnabled())) {
      try {
        PushResult result = callClientServiceApi(integration, matterData, client, request);

        // 更新推送结果
        pushRecordMapper.updatePushResult(
            record.getId(),
            PushRecord.STATUS_SUCCESS,
            result.externalId(),
            result.externalUrl(),
            null);

        record.setStatus(PushRecord.STATUS_SUCCESS);
        record.setExternalId(result.externalId());
        record.setExternalUrl(result.externalUrl());

      } catch (Exception e) {
        log.error("推送到客户服务系统失败", e);
        pushRecordMapper.updatePushResult(
            record.getId(), PushRecord.STATUS_FAILED, null, null, e.getMessage());
        record.setStatus(PushRecord.STATUS_FAILED);
        record.setErrorMessage(e.getMessage());
      }
    } else {
      // 客户服务系统未配置，标记为待推送
      log.info("客户服务系统未配置或未启用，推送记录已保存，待后续处理");
      record.setErrorMessage("客户服务系统未配置或未启用");
    }

    return convertToDTO(record, matter, client);
  }

  /**
   * 获取项目的推送记录列表.
   *
   * @param matterId 项目ID
   * @param clientId 客户ID
   * @param status 状态
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 分页结果
   */
  public PageResult<PushRecordDTO> getPushRecords(
      final Long matterId,
      final Long clientId,
      final String status,
      final int pageNum,
      final int pageSize) {
    Page<PushRecord> page = new Page<>(pageNum, pageSize);
    var resultPage = pushRecordMapper.selectPage(page, matterId, clientId, status);

    List<PushRecordDTO> list =
        resultPage.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList());

    return PageResult.of(list, resultPage.getTotal(), pageNum, pageSize);
  }

  /**
   * 获取推送记录详情.
   *
   * @param id 推送记录ID
   * @return 推送记录DTO
   */
  public PushRecordDTO getPushRecordById(final Long id) {
    PushRecord record = pushRecordMapper.selectById(id);
    if (record == null || record.getDeleted()) {
      throw new BusinessException("推送记录不存在");
    }
    return convertToDTO(record);
  }

  /**
   * 获取项目的最近一次成功推送.
   *
   * @param matterId 项目ID
   * @return 推送记录DTO
   */
  public PushRecordDTO getLatestPush(final Long matterId) {
    PushRecord record = pushRecordMapper.selectLatestSuccessByMatterId(matterId);
    if (record == null) {
      return null;
    }
    return convertToDTO(record);
  }

  /**
   * 获取或创建推送配置.
   *
   * @param matterId 项目ID
   * @param clientId 客户ID
   * @return 推送配置DTO
   */
  public PushConfigDTO getOrCreateConfig(final Long matterId, final Long clientId) {
    PushConfig config = pushConfigMapper.selectByMatterId(matterId);
    if (config == null) {
      config =
          PushConfig.builder()
              .matterId(matterId)
              .clientId(clientId)
              .enabled(false)
              .scopes("MATTER_INFO,MATTER_PROGRESS,LAWYER_INFO,DEADLINE_INFO")
              .autoPushOnUpdate(false)
              .validDays(DEFAULT_VALID_DAYS)
              .build();
      pushConfigMapper.insert(config);
    }
    return convertConfigToDTO(config);
  }

  /**
   * 更新推送配置.
   *
   * @param matterId 项目ID
   * @param dto 推送配置DTO
   * @return 推送配置DTO
   */
  @Transactional
  public PushConfigDTO updateConfig(final Long matterId, final PushConfigDTO dto) {
    PushConfig config = pushConfigMapper.selectByMatterId(matterId);
    if (config == null) {
      throw new BusinessException("配置不存在");
    }

    if (dto.getEnabled() != null) {
      config.setEnabled(dto.getEnabled());
    }
    if (dto.getScopes() != null) {
      config.setScopes(String.join(",", dto.getScopes()));
    }
    if (dto.getAutoPushOnUpdate() != null) {
      config.setAutoPushOnUpdate(dto.getAutoPushOnUpdate());
    }
    if (dto.getValidDays() != null) {
      config.setValidDays(dto.getValidDays());
    }

    pushConfigMapper.updateById(config);
    return convertConfigToDTO(config);
  }

  /**
   * 统计推送信息.
   *
   * @param matterId 项目ID
   * @return 统计信息Map
   */
  public Map<String, Object> getStatistics(final Long matterId) {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalPushCount", pushRecordMapper.countSuccessByMatterId(matterId));

    PushRecord latest = pushRecordMapper.selectLatestSuccessByMatterId(matterId);
    if (latest != null) {
      stats.put("lastPushTime", latest.getCreatedAt());
      stats.put("lastPushStatus", latest.getStatus());
      stats.put("externalUrl", latest.getExternalUrl());
    }

    return stats;
  }

  /**
   * 获取客户服务系统集成配置.
   *
   * @return 外部集成配置
   */
  private ExternalIntegration getClientServiceIntegration() {
    return integrationMapper.selectByType(CLIENT_SERVICE_TYPE);
  }

  /**
   * 调用客户服务系统API.
   *
   * @param integration 外部集成配置
   * @param matterData 项目数据
   * @param client 客户信息
   * @param request 推送请求
   * @return 推送结果
   */
  private PushResult callClientServiceApi(
      final ExternalIntegration integration,
      final PortalMatterDTO matterData,
      final Client client,
      final PushRequest request) {
    RestTemplate restTemplate = new RestTemplate();

    // 解密API密钥（API密钥在数据库中加密存储）
    String apiKey = integration.getApiKey();
    if (apiKey != null && !apiKey.isEmpty()) {
      try {
        apiKey = encryptionService.decrypt(apiKey);
      } catch (Exception e) {
        log.warn("API密钥解密失败，尝试使用原值（可能是未加密的历史数据）: {}", e.getMessage());
        // 如果解密失败，可能是未加密的旧数据，继续使用原值
      }
    }

    // 构建请求头
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isEmpty()) {
      headers.set("Authorization", "Bearer " + apiKey);
    }

    // 构建请求体
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("clientId", client.getId());
    requestBody.put("clientName", client.getName());
    requestBody.put("matterData", matterData);
    requestBody.put(
        "validDays", request.getValidDays() != null ? request.getValidDays() : DEFAULT_VALID_DAYS);
    requestBody.put("scopes", request.getScopes());

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    // 构建API URL（正确处理URL拼接，避免双/api）
    String baseUrl = integration.getApiUrl();
    if (baseUrl == null || baseUrl.isEmpty()) {
      throw new BusinessException("客户服务系统API地址未配置");
    }
    // 移除末尾的斜杠，避免双斜杠
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    // 如果baseUrl已经以/api结尾，直接拼接/matter/receive；否则拼接/api/matter/receive
    String apiPath = baseUrl.endsWith("/api") ? "/matter/receive" : "/api/matter/receive";
    String apiUrl = baseUrl + apiPath;

    log.debug("调用客户服务系统API: url={}, clientId={}, matterId={}", 
        apiUrl, client.getId(), matterData.getMatterId());

    try {
      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(
              apiUrl,
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});

      if (!response.getStatusCode().is2xxSuccessful()) {
        log.error("客户服务系统返回错误状态码: status={}", response.getStatusCode());
        throw new BusinessException(
            "客户服务系统返回错误状态码: " + response.getStatusCode());
      }

      Map<String, Object> body = response.getBody();
      if (body == null) {
        log.error("客户服务系统返回空响应体");
        throw new BusinessException("客户服务系统返回空响应体");
      }

      // 支持两种响应格式：
      // 1. 扁平格式：{id: "...", accessUrl: "..."}
      // 2. 标准格式：{success: true, code: "200", data: {id: "...", accessUrl: "..."}}
      //    或 {code: 200, data: {id: "...", accessUrl: "..."}}
      
      String id = null;
      String accessUrl = null;
      
      // 尝试从扁平格式获取（推荐格式）
      if (body.containsKey("id") && body.get("id") != null) {
        id = String.valueOf(body.get("id"));
        accessUrl = body.get("accessUrl") != null ? String.valueOf(body.get("accessUrl")) : null;
        log.debug("解析扁平格式响应: id={}, accessUrl={}", id, accessUrl);
      } 
      // 尝试从标准格式获取（data对象中）
      else if (body.containsKey("data") && body.get("data") instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data.containsKey("id") && data.get("id") != null) {
          id = String.valueOf(data.get("id"));
          accessUrl = data.get("accessUrl") != null ? String.valueOf(data.get("accessUrl")) : null;
          log.debug("解析标准格式响应: id={}, accessUrl={}", id, accessUrl);
        }
      }
      
      if (id == null) {
        log.error("客户服务系统返回格式错误：缺少id字段, body={}", body);
        throw new BusinessException("客户服务系统返回格式错误：缺少id字段");
      }

      log.info("客户服务系统调用成功: id={}, accessUrl={}", id, accessUrl);
      return new PushResult(id, accessUrl);

    } catch (BusinessException e) {
      throw e;
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      log.error("调用客户服务系统API失败: url={}, status={}, body={}", 
          apiUrl, e.getStatusCode(), e.getResponseBodyAsString());
      if (e.getStatusCode().value() == 401) {
        throw new BusinessException("客户服务系统认证失败，请检查API密钥配置");
      } else if (e.getStatusCode().value() == 404) {
        throw new BusinessException("客户服务系统接口不存在，请检查API地址配置: " + apiUrl);
      } else {
        throw new BusinessException("调用客户服务系统失败: " + e.getStatusCode() + " - " + e.getMessage());
      }
    } catch (org.springframework.web.client.ResourceAccessException e) {
      log.error("无法连接到客户服务系统: url={}", apiUrl, e);
      throw new BusinessException("无法连接到客户服务系统，请检查API地址配置和网络连接: " + apiUrl);
    } catch (Exception e) {
      log.error("调用客户服务系统API异常: url={}", apiUrl, e);
      throw new BusinessException("调用客户服务系统异常: " + e.getMessage(), e);
    }
  }

  /**
   * 推送结果.
   *
   * @param externalId 外部系统ID
   * @param externalUrl 外部系统URL
   */
  private record PushResult(String externalId, String externalUrl) {}

  /**
   * 转换为DTO.
   *
   * @param record 推送记录实体
   * @return 推送记录DTO
   */
  private PushRecordDTO convertToDTO(final PushRecord record) {
    Matter matter = matterMapper.selectById(record.getMatterId());
    Client client = clientMapper.selectById(record.getClientId());
    return convertToDTO(record, matter, client);
  }

  /**
   * 转换为DTO.
   *
   * @param record 推送记录实体
   * @param matter 项目实体
   * @param client 客户实体
   * @return 推送记录DTO
   */
  private PushRecordDTO convertToDTO(
      final PushRecord record, final Matter matter, final Client client) {
    return PushRecordDTO.builder()
        .id(record.getId())
        .matterId(record.getMatterId())
        .matterName(matter != null ? matter.getName() : null)
        .clientId(record.getClientId())
        .clientName(client != null ? client.getName() : null)
        .pushType(record.getPushType())
        .scopes(
            record.getScopes() != null ? Arrays.asList(record.getScopes().split(",")) : List.of())
        .status(record.getStatus())
        .externalId(record.getExternalId())
        .externalUrl(record.getExternalUrl())
        .errorMessage(record.getErrorMessage())
        .expiresAt(record.getExpiresAt())
        .createdAt(record.getCreatedAt())
        .build();
  }

  /**
   * 转换配置为DTO.
   *
   * @param config 推送配置实体
   * @return 推送配置DTO
   */
  private PushConfigDTO convertConfigToDTO(final PushConfig config) {
    // 检查客户服务系统是否已连接
    ExternalIntegration integration = getClientServiceIntegration();
    boolean connected = integration != null && Boolean.TRUE.equals(integration.getEnabled());
    String connectionMessage = null;
    if (integration == null) {
      connectionMessage = "客户服务系统尚未配置，请在【系统管理→外部系统集成】中配置";
    } else if (!Boolean.TRUE.equals(integration.getEnabled())) {
      connectionMessage = "客户服务系统已配置但未启用";
    }

    return PushConfigDTO.builder()
        .id(config.getId())
        .matterId(config.getMatterId())
        .clientId(config.getClientId())
        .enabled(config.getEnabled())
        .scopes(
            config.getScopes() != null ? Arrays.asList(config.getScopes().split(",")) : List.of())
        .autoPushOnUpdate(config.getAutoPushOnUpdate())
        .validDays(config.getValidDays())
        .clientServiceConnected(connected)
        .connectionMessage(connectionMessage)
        .build();
  }

  /**
   * 构建推送的项目数据.
   *
   * @param matter 项目实体
   * @param scopes 授权范围
   * @param documentIds 要推送的文档ID列表（当 scopes 包含 DOCUMENT_FILES 时使用）
   * @return 推送数据DTO
   */
  private PortalMatterDTO buildMatterData(
      final Matter matter, final Set<String> scopes, final List<Long> documentIds) {
    PortalMatterDTO.PortalMatterDTOBuilder builder = PortalMatterDTO.builder();

    // 基本信息始终包含
    if (scopes.contains("MATTER_INFO")) {
      builder
          .matterId(matter.getId())
          .matterNo(matter.getMatterNo())
          .matterName(matter.getName())
          .matterType(matter.getMatterType())
          .status(matter.getStatus())
          .createDate(matter.getCreatedAt());
    }

    // 进度信息
    if (scopes.contains("MATTER_PROGRESS")) {
      builder
          .currentStage(matter.getLitigationStage())
          .lastUpdateTime(matter.getUpdatedAt());
    }

    // 可下载文件
    if (scopes.contains("DOCUMENT_FILES") && documentIds != null && !documentIds.isEmpty()) {
      List<PortalMatterDTO.DownloadableFileDTO> downloadableFiles =
          documentIds.stream()
              .map(documentMapper::selectById)
              .filter(doc -> doc != null && !doc.getDeleted())
              .map(this::convertToDownloadableFile)
              .collect(Collectors.toList());
      builder.downloadableFiles(downloadableFiles);
    }

    // 其他信息可以按需扩展，这里简化处理
    // 实际项目中可以从相关表查询更多数据

    return builder.build();
  }

  /** 文件下载URL有效期（秒） - 24小时 */
  private static final int FILE_URL_EXPIRY_SECONDS = 86400;

  /**
   * 转换文档为可下载文件DTO.
   *
   * @param doc 文档实体
   * @return 可下载文件DTO
   */
  private PortalMatterDTO.DownloadableFileDTO convertToDownloadableFile(final Document doc) {
    String sourceUrl = null;
    try {
      // 使用 storagePath 或 physicalName 作为对象名
      String objectName = doc.getStoragePath();
      if (objectName == null || objectName.isEmpty()) {
        objectName = doc.getPhysicalName();
      }
      if (objectName != null && !objectName.isEmpty()) {
        sourceUrl = minioService.getPresignedUrl(objectName, FILE_URL_EXPIRY_SECONDS);
      }
    } catch (Exception e) {
      log.warn("生成文件下载URL失败: docId={}, error={}", doc.getId(), e.getMessage());
    }

    return PortalMatterDTO.DownloadableFileDTO.builder()
        .documentId(doc.getId())
        .fileName(doc.getTitle() != null ? doc.getTitle() : doc.getFileName())
        .fileType(doc.getFileType())
        .fileSize(doc.getFileSize())
        .category(doc.getFileCategory())
        .sourceUrl(sourceUrl)
        .uploadTime(doc.getCreatedAt())
        .build();
  }

  /**
   * 校验用户对项目的数据权限 只有项目参与者才能操作.
   *
   * @param matterId 项目ID
   * @param userId 用户ID
   */
  private void checkMatterPermission(final Long matterId, final Long userId) {
    if (userId == null) {
      throw new BusinessException("未获取到当前用户信息");
    }

    // 检查是否是项目参与者
    boolean isParticipant = participantMapper.existsByMatterIdAndUserId(matterId, userId);
    if (!isParticipant) {
      // 检查是否是管理员（管理员可以操作所有项目）
      // 注：这里简化处理，实际可以通过 SecurityUtils 获取用户角色判断
      log.warn("用户 {} 尝试操作非自己参与的项目 {}", userId, matterId);
      throw new BusinessException("您没有权限操作此项目");
    }
  }
}
