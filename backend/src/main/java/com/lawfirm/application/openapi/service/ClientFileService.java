package com.lawfirm.application.openapi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.openapi.dto.ClientFileDTO;
import com.lawfirm.application.openapi.dto.ClientFileReceiveRequest;
import com.lawfirm.application.openapi.dto.ClientFileSyncRequest;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.openapi.entity.ClientFile;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.openapi.ClientFileMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 客户文件服务
 * 处理客服系统推送的客户上传文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientFileService {

    private final ClientFileMapper clientFileMapper;
    private final MatterMapper matterMapper;
    private final ExternalIntegrationMapper integrationMapper;
    private final UserMapper userMapper;
    private final MinioService minioService;
    private final DocumentRepository documentRepository;

    private static final String CLIENT_SERVICE_TYPE = "CLIENT_SERVICE";

    // ========== 文件类别名称映射 ==========
    private static final Map<String, String> CATEGORY_NAMES = Map.of(
        ClientFile.CATEGORY_EVIDENCE, "证据材料",
        ClientFile.CATEGORY_CONTRACT, "合同文件",
        ClientFile.CATEGORY_ID_CARD, "身份证件",
        ClientFile.CATEGORY_OTHER, "其他"
    );

    // ========== 状态名称映射 ==========
    private static final Map<String, String> STATUS_NAMES = Map.of(
        ClientFile.STATUS_PENDING, "待同步",
        ClientFile.STATUS_SYNCED, "已同步",
        ClientFile.STATUS_DELETED, "已删除",
        ClientFile.STATUS_FAILED, "同步失败"
    );

    /**
     * 接收客服系统推送的客户文件
     */
    @Transactional
    public ClientFileDTO receiveFile(ClientFileReceiveRequest request) {
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
        ClientFile clientFile = ClientFile.builder()
                .matterId(request.getMatterId())
                .clientId(request.getClientId())
                .clientName(request.getClientName())
                .fileName(request.getFileName())
                .originalFileName(request.getFileName())
                .fileSize(request.getFileSize())
                .fileType(request.getFileType())
                .fileCategory(request.getFileCategory() != null ? request.getFileCategory() : ClientFile.CATEGORY_OTHER)
                .description(request.getDescription())
                .externalFileId(request.getExternalFileId())
                .externalFileUrl(request.getExternalFileUrl())
                .uploadedBy(request.getUploadedBy())
                .uploadedAt(request.getUploadedAt() != null ? request.getUploadedAt() : LocalDateTime.now())
                .status(ClientFile.STATUS_PENDING)
                .build();

        clientFileMapper.insert(clientFile);
        log.info("接收客户文件成功: matterId={}, fileName={}", request.getMatterId(), request.getFileName());

        return convertToDTO(clientFile, matter);
    }

    /**
     * 获取项目的客户文件列表
     */
    public PageResult<ClientFileDTO> getClientFiles(Long matterId, String status, int pageNum, int pageSize) {
        Matter matter = matterMapper.selectById(matterId);
        
        Page<ClientFile> page = new Page<>(pageNum, pageSize);
        var resultPage = clientFileMapper.selectPage(page, matterId, status);

        List<ClientFileDTO> list = resultPage.getRecords().stream()
                .map(f -> convertToDTO(f, matter))
                .collect(Collectors.toList());

        return PageResult.of(list, resultPage.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取项目待同步的文件列表
     */
    public List<ClientFileDTO> getPendingFiles(Long matterId) {
        Matter matter = matterMapper.selectById(matterId);
        List<ClientFile> files = clientFileMapper.selectPendingByMatterId(matterId);
        return files.stream()
                .map(f -> convertToDTO(f, matter))
                .collect(Collectors.toList());
    }

    /**
     * 统计项目待同步文件数量
     */
    public int countPendingFiles(Long matterId) {
        return clientFileMapper.countPendingByMatterId(matterId);
    }

    /**
     * 同步文件到卷宗
     * 从外部URL下载文件，上传到MinIO，并创建文档记录关联到卷宗目录项
     */
    @Transactional
    public ClientFileDTO syncToFolder(ClientFileSyncRequest request, Long operatorId) {
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
            
            // 3. 构建存储路径并上传到MinIO
            String storagePath = String.format("matters/%d/client_uploads/", clientFile.getMatterId());
            String fileName = UUID.randomUUID().toString() + "_" + clientFile.getFileName();
            String objectName = storagePath + fileName;
            
            String fileUrl = minioService.uploadFile(
                    new ByteArrayInputStream(fileContent),
                    objectName,
                    clientFile.getFileType()
            );
            
            // 4. 创建文档记录并关联到卷宗目录项
            Document document = Document.builder()
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
                    null
            );

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
            clientFileMapper.updateSyncStatus(
                    clientFile.getId(),
                    ClientFile.STATUS_FAILED,
                    null,
                    null,
                    operatorId,
                    e.getMessage()
            );
            throw e;
        } catch (Exception e) {
            log.error("文件同步失败: {}", e.getMessage(), e);
            clientFileMapper.updateSyncStatus(
                    clientFile.getId(),
                    ClientFile.STATUS_FAILED,
                    null,
                    null,
                    operatorId,
                    e.getMessage()
            );
            throw new BusinessException("文件同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 批量同步文件
     */
    @Transactional
    public List<ClientFileDTO> batchSync(List<ClientFileSyncRequest> requests, Long operatorId) {
        return requests.stream()
                .map(req -> {
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
     * 忽略/删除待同步文件
     */
    @Transactional
    public void ignoreFile(Long fileId, Long operatorId) {
        ClientFile clientFile = clientFileMapper.selectById(fileId);
        if (clientFile == null) {
            throw new BusinessException("文件不存在");
        }

        // 标记为已删除
        clientFileMapper.updateSyncStatus(
                fileId,
                ClientFile.STATUS_DELETED,
                null,
                null,
                operatorId,
                "用户忽略"
        );

        // 通知客服系统删除
        notifyClientServiceToDelete(clientFile.getExternalFileId());
    }

    /**
     * 下载文件
     */
    private byte[] downloadFile(String fileUrl) throws Exception {
        URL url = new URL(fileUrl);
        try (InputStream is = url.openStream()) {
            return is.readAllBytes();
        }
    }

    /**
     * 通知客服系统删除文件
     */
    private void notifyClientServiceToDelete(String externalFileId) {
        try {
            ExternalIntegration integration = integrationMapper.selectByType(CLIENT_SERVICE_TYPE);
            if (integration == null || !Boolean.TRUE.equals(integration.getEnabled())) {
                log.warn("客服系统未配置或未启用，跳过删除通知");
                return;
            }

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (integration.getApiKey() != null) {
                headers.set("Authorization", "Bearer " + integration.getApiKey());
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("fileId", externalFileId);
            requestBody.put("action", "DELETE");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String apiUrl = integration.getApiUrl() + "/files/delete";
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("通知客服系统删除文件成功: {}", externalFileId);
            } else {
                log.warn("通知客服系统删除文件失败: {}", externalFileId);
            }
        } catch (Exception e) {
            log.error("通知客服系统删除文件异常: {}", e.getMessage());
            // 不抛出异常，删除通知失败不影响主流程
        }
    }

    /**
     * 转换为 DTO
     */
    private ClientFileDTO convertToDTO(ClientFile file, Matter matter) {
        ClientFileDTO dto = ClientFileDTO.builder()
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
