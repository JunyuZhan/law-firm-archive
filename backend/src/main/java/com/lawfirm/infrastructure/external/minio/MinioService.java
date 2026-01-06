package com.lawfirm.infrastructure.external.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * MinIO 文件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private MinioClient minioClient;

    /**
     * 获取MinIO客户端（懒加载）
     */
    private MinioClient getMinioClient() {
        if (minioClient == null) {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            
            // 确保bucket存在
            try {
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
                    log.info("创建MinIO bucket: {}", bucketName);
                }
            } catch (Exception e) {
                log.error("MinIO bucket检查/创建失败", e);
            }
        }
        return minioClient;
    }

    /**
     * 上传文件
     * 
     * @param file 文件
     * @param folder 文件夹路径（如：reports/）
     * @return 文件URL
     */
    public String uploadFile(MultipartFile file, String folder) throws Exception {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String objectName = folder + fileName;
        
        getMinioClient().putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        
        String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
        log.info("文件上传成功: {}", fileUrl);
        return fileUrl;
    }

    /**
     * 上传文件（从InputStream）
     * 
     * @param inputStream 输入流
     * @param fileName 文件名
     * @param folder 文件夹路径
     * @param contentType 内容类型
     * @return 文件URL
     */
    public String uploadFile(InputStream inputStream, String fileName, String folder, String contentType) throws Exception {
        String objectName = folder + fileName;
        
        // 获取文件大小（如果可能）
        long fileSize = inputStream.available();
        if (fileSize <= 0) {
            fileSize = -1; // 未知大小
        }
        
        getMinioClient().putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, fileSize, -1)
                        .contentType(contentType)
                        .build()
        );
        
        String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
        log.info("文件上传成功: {}", fileUrl);
        return fileUrl;
    }

    /**
     * 下载文件
     * 
     * @param objectName 对象名称（完整路径）
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectName) throws Exception {
        return getMinioClient().getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 删除文件
     * 
     * @param objectName 对象名称（完整路径）
     */
    public void deleteFile(String objectName) throws Exception {
        getMinioClient().removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        log.info("文件删除成功: {}", objectName);
    }

    /**
     * 从URL提取对象名称
     */
    public String extractObjectName(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains(bucketName + "/")) {
            return null;
        }
        int index = fileUrl.indexOf(bucketName + "/");
        return fileUrl.substring(index + bucketName.length() + 1);
    }

    /**
     * 生成预签名URL（用于临时访问）
     */
    public String getPresignedUrl(String objectName, int expirySeconds) throws Exception {
        // MinIO Java SDK 8.x 使用 getPresignedObjectUrl
        return getMinioClient().getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                        .method(io.minio.http.Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expirySeconds)
                        .build()
        );
    }

    /**
     * 生成 Docker 容器可访问的预签名URL
     * 将 localhost:9000 替换为 minio:9000（Docker 内部网络）
     */
    public String getPresignedUrlForDocker(String objectName, int expirySeconds) throws Exception {
        String presignedUrl = getPresignedUrl(objectName, expirySeconds);
        // 替换为 Docker 内部网络地址
        return presignedUrl.replace("localhost:9000", "minio:9000")
                          .replace("127.0.0.1:9000", "minio:9000");
    }

    /**
     * 下载文件为字节数组
     */
    public byte[] downloadFileAsBytes(String objectName) throws Exception {
        try (InputStream inputStream = downloadFile(objectName)) {
            return inputStream.readAllBytes();
        }
    }

    /**
     * 上传字节数组
     */
    public String uploadBytes(byte[] bytes, String objectName, String contentType) throws Exception {
        try (InputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
            getMinioClient().putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, bytes.length, -1)
                            .contentType(contentType)
                            .build()
            );
            
            String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
        }
    }
}

