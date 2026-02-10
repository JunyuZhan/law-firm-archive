package com.lawfirm.application.document.service;

import com.lawfirm.common.util.FileHashUtil;
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.infrastructure.external.minio.MinioService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 文件访问服务 统一处理文件上传，设置MinIO四元组字段. */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileAccessService {

  /** MinIO服务. */
  private final MinioService minioService;

  /**
   * 上传文件并返回存储信息
   *
   * @param file 文件
   * @param fileType 文件类型（MinioPathGenerator.FileType常量）
   * @param matterId 项目ID（可选）
   * @param folder 文件夹名称（可选）
   * @return 包含存储信息的Map：bucketName, storagePath, physicalName, fileHash, fileUrl
   */
  public Map<String, String> uploadFile(
      final MultipartFile file, final String fileType, final Long matterId, final String folder) {
    // 1. 验证文件
    FileValidator.ValidationResult validationResult = FileValidator.validate(file);
    if (!validationResult.isValid()) {
      throw new RuntimeException("文件验证失败: " + validationResult.getErrorMessage());
    }

    try {
      // 2. 计算文件Hash
      String fileHash = FileHashUtil.calculateHash(file);
      log.debug("文件Hash计算完成: hash={}, fileName={}", fileHash, file.getOriginalFilename());

      // 3. 生成标准化存储路径
      String standardStoragePath;
      if (MinioPathGenerator.FileType.APPROVAL.equals(fileType)) {
        // 审批附件：folder参数代表businessType
        standardStoragePath = MinioPathGenerator.generateApprovalPath(folder, matterId);
      } else {
        standardStoragePath = MinioPathGenerator.generateStandardPath(fileType, matterId, folder);
      }

      // 4. 验证存储路径格式
      if (matterId != null && !MinioPathGenerator.FileType.APPROVAL.equals(fileType)) {
        MinioPathGenerator.validateStoragePath(standardStoragePath, fileType, matterId);
      }

      // 5. 生成物理文件名
      String originalFilename = file.getOriginalFilename();
      String physicalName = MinioPathGenerator.generatePhysicalName(originalFilename);

      // 6. 构建完整对象名称
      String objectName = MinioPathGenerator.buildObjectName(standardStoragePath, physicalName);

      // 7. 上传文件到 MinIO（使用 try-with-resources 确保 InputStream 关闭）
      String fileUrl;
      try (java.io.InputStream inputStream = file.getInputStream()) {
        fileUrl = minioService.uploadFile(inputStream, objectName, file.getContentType());
      }

      // 8. 构建返回信息
      Map<String, String> storageInfo = new HashMap<>();
      storageInfo.put("bucketName", minioService.getBucketName());
      storageInfo.put("storagePath", standardStoragePath);
      storageInfo.put("physicalName", physicalName);
      storageInfo.put("fileHash", fileHash);
      storageInfo.put("fileUrl", fileUrl);

      log.info(
          "文件上传成功: fileName={}, storagePath={}, physicalName={}, hash={}",
          originalFilename,
          standardStoragePath,
          physicalName,
          fileHash);

      return storageInfo;

    } catch (Exception e) {
      log.error("文件上传失败", e);
      throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
    }
  }

  /**
   * 从MinIO URL解析存储信息 用于向后兼容：当只提供了fileUrl时，尝试解析并设置新字段
   *
   * @param fileUrl MinIO文件URL
   * @return 包含存储信息的Map：bucketName, storagePath, physicalName（如果无法解析则返回null）
   */
  public Map<String, String> parseStorageInfoFromUrl(final String fileUrl) {
    if (fileUrl == null || fileUrl.isEmpty()) {
      return null;
    }

    try {
      // 解析MinIO URL格式：http://endpoint/bucketName/objectName
      // 例如：http://minio:9000/law-firm/contracts/M_123/2026-01/合同文件/20260127_abc123_合同.pdf

      String bucketName = minioService.getBucketName();
      String endpoint = minioService.getEndpoint();

      // 检查是否是MinIO URL
      if (!fileUrl.contains(endpoint) && !fileUrl.contains("/" + bucketName + "/")) {
        // 不是MinIO URL，返回null（向后兼容）
        return null;
      }

      // 提取objectName
      String objectName = null;
      if (fileUrl.contains("/" + bucketName + "/")) {
        int index = fileUrl.indexOf("/" + bucketName + "/");
        objectName = fileUrl.substring(index + bucketName.length() + 2);
        // 去除查询参数
        if (objectName.contains("?")) {
          objectName = objectName.substring(0, objectName.indexOf("?"));
        }
      }

      if (objectName == null || objectName.isEmpty()) {
        return null;
      }

      // 解析storagePath和physicalName
      int lastSlash = objectName.lastIndexOf('/');
      if (lastSlash < 0) {
        return null;
      }

      String storagePath = objectName.substring(0, lastSlash + 1);
      String physicalName = objectName.substring(lastSlash + 1);

      Map<String, String> storageInfo = new HashMap<>();
      storageInfo.put("bucketName", bucketName);
      storageInfo.put("storagePath", storagePath);
      storageInfo.put("physicalName", physicalName);
      // fileHash无法从URL解析，返回null

      return storageInfo;

    } catch (Exception e) {
      log.warn("无法从URL解析存储信息: {}", fileUrl, e);
      return null;
    }
  }

  /**
   * 获取文档访问URL 优先使用新字段（bucketName, storagePath, physicalName），回退到filePath
   *
   * @param document 文档实体
   * @param accessType 访问类型（DOWNLOAD或VIEW，字符串）
   * @return 文件访问URL
   */
  public String getDocumentAccessUrl(
      final com.lawfirm.domain.document.entity.Document document, final String accessType) {
    if (document == null) {
      return null;
    }

    String objectName = null;

    // 优先使用新字段构建URL
    if (document.getStoragePath() != null && document.getPhysicalName() != null) {
      objectName =
          MinioPathGenerator.buildObjectName(document.getStoragePath(), document.getPhysicalName());
    } else if (document.getFilePath() != null && !document.getFilePath().isEmpty()) {
      // 回退到filePath
      objectName = document.getFilePath();
    }

    if (objectName == null || objectName.isEmpty()) {
      return null;
    }

    // 构建文件URL
    return minioService.buildFileUrl(objectName);
  }
}
