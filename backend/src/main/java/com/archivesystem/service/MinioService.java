package com.archivesystem.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * MinIO对象存储服务.
 */
@Slf4j
@Service
public class MinioService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.proxy-prefix:}")
    private String proxyPrefix;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        
        // 确保bucket存在
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("初始化MinIO失败（MinIO服务可能未启动）: {}", e.getMessage());
        }
    }

    /**
     * 获取bucket名称.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * 上传文件.
     */
    public void upload(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
            log.info("文件上传成功: {}", objectName);
        } catch (Exception e) {
            log.error("文件上传失败: {}", objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传MultipartFile.
     */
    public String uploadFile(MultipartFile file, String objectName) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("文件上传成功: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("文件上传失败: {}", objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传字节数组.
     */
    public String uploadBytes(byte[] data, String objectName, String contentType) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, data.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("字节数据上传成功: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("字节数据上传失败: {}", objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 从URL下载并存储到MinIO.
     */
    public String downloadAndStore(String sourceUrl, String objectName) {
        try {
            URL url = new URL(sourceUrl);
            try (InputStream inputStream = url.openStream()) {
                byte[] data = inputStream.readAllBytes();
                return uploadBytes(data, objectName, "application/octet-stream");
            }
        } catch (Exception e) {
            log.error("从URL下载并存储失败: sourceUrl={}, objectName={}", sourceUrl, objectName, e);
            throw new RuntimeException("文件下载存储失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件输入流.
     */
    public InputStream getFile(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("获取文件失败: {}", objectName, e);
            throw new RuntimeException("获取文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取预签名URL（用于下载）.
     */
    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(objectName, 3600);
    }

    /**
     * 获取预签名URL（指定有效期）.
     * 返回的URL已转换为相对路径 /storage/...，通过前端Nginx代理访问MinIO，
     * 避免暴露MinIO内网地址，同时兼容HTTPS外网访问。
     */
    public String getPresignedUrl(String objectName, int expirySeconds) {
        // 验证 objectName 防止路径穿越攻击
        validateObjectName(objectName);
        
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build());
            // 如果配置了代理前缀，将 MinIO 内网地址替换为 Nginx 代理的相对路径
            // 例如: http://minio:9000/bucket/file → /storage/bucket/file
            if (proxyPrefix != null && !proxyPrefix.isBlank()) {
                url = url.replace(endpoint, proxyPrefix);
            }
            return url;
        } catch (Exception e) {
            log.error("获取预签名URL失败: {}", objectName, e);
            throw new RuntimeException("获取下载链接失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证 objectName 防止路径穿越攻击
     */
    private void validateObjectName(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            throw new IllegalArgumentException("对象名称不能为空");
        }
        // 检查路径穿越字符
        if (objectName.contains("..") || objectName.contains("//") || objectName.startsWith("/")) {
            log.warn("检测到可疑的对象名称: {}", objectName);
            throw new IllegalArgumentException("非法的对象名称");
        }
        // 检查空字节注入
        if (objectName.contains("\0")) {
            log.warn("检测到空字节注入: {}", objectName);
            throw new IllegalArgumentException("非法的对象名称");
        }
    }

    /**
     * 删除文件.
     */
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("文件删除成功: {}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectName, e);
        }
    }

    /**
     * 删除文件（别名）.
     */
    public void deleteFile(String objectName) {
        delete(objectName);
    }

    /**
     * 检查文件是否存在.
     */
    public boolean exists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
