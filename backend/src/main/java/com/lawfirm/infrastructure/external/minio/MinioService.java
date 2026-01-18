package com.lawfirm.infrastructure.external.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MinIO 文件服务
 * 
 * 线程安全：使用 @PostConstruct 在启动时初始化客户端，避免并发初始化问题
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

    @Value("${minio.external-endpoint:#{null}}")
    private String externalEndpoint;

    private MinioClient minioClient;
    private MinioClient externalMinioClient;

    /**
     * 初始化 MinIO 客户端（启动时执行，线程安全）
     */
    @PostConstruct
    public void init() {
        log.info("初始化 MinIO 客户端: endpoint={}", endpoint);

        try {
            // 创建客户端
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 确保bucket存在
            initializeBucket();

            log.info("MinIO 客户端初始化成功");
        } catch (Exception e) {
            log.error("MinIO 客户端初始化失败", e);
            // 不抛出异常，允许系统启动（MinIO 可能是可选服务）
            // 后续调用时会检查 minioClient 是否为 null
        }
    }

    /**
     * 初始化 bucket（只在启动时调用一次）
     */
    private void initializeBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("创建 MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO bucket 检查/创建失败", e);
        }
    }

    /**
     * 获取 MinIO 客户端
     * 如果客户端未初始化，抛出友好的业务异常而不是返回null导致NPE
     * 
     * @return MinioClient 实例
     * @throws RuntimeException 如果文件服务不可用
     */
    private MinioClient getMinioClient() {
        if (minioClient == null) {
            log.error("MinIO 客户端未初始化，文件操作无法执行");
            throw new RuntimeException("文件服务暂时不可用，请联系管理员检查MinIO服务状态");
        }
        return minioClient;
    }

    /**
     * 检查文件服务是否可用
     * 
     * @return true 如果服务可用
     */
    public boolean isAvailable() {
        return minioClient != null;
    }

    /**
     * 上传文件
     * 
     * @param file   文件
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
                        .build());

        String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
        log.info("文件上传成功: {}", fileUrl);
        return fileUrl;
    }

    /**
     * 上传文件（从InputStream）
     * 
     * @param inputStream 输入流
     * @param fileName    文件名
     * @param folder      文件夹路径
     * @param contentType 内容类型
     * @return 文件URL
     */
    public String uploadFile(InputStream inputStream, String fileName, String folder, String contentType)
            throws Exception {
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
                        .build());

        String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
        log.info("文件上传成功: {}", fileUrl);
        return fileUrl;
    }

    /**
     * 上传文件（使用完整的 objectName）
     * 
     * @param inputStream 输入流
     * @param objectName  完整的对象路径名称
     * @param contentType 内容类型
     * @return 文件URL
     */
    public String uploadFile(InputStream inputStream, String objectName, String contentType) throws Exception {
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
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .build());

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
                        .build());
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
                        .build());
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
        String objectName = fileUrl.substring(index + bucketName.length() + 1);

        // 去除可能存在的查询参数
        if (objectName.contains("?")) {
            objectName = objectName.substring(0, objectName.indexOf("?"));
        }

        // URL 解码，防止双重编码 (例如 %E8 -> 中文)
        try {
            objectName = java.net.URLDecoder.decode(objectName, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.warn("文件名URL解码失败: {}", objectName);
        }

        return objectName;
    }

    /**
     * 获取外部访问的 MinioClient（用于 Docker 容器等外部服务访问）
     */
    private MinioClient getExternalMinioClient() {
        if (externalMinioClient == null) {
            String extEndpoint = externalEndpoint != null ? externalEndpoint : endpoint;
            externalMinioClient = MinioClient.builder()
                    .endpoint(extEndpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        }
        return externalMinioClient;
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
                        .build());
    }

    /**
     * 生成 Docker 容器可访问的预签名URL
     * 先生成标准预签名 URL，然后替换 host 为 Docker 可访问的地址
     * 注意：需要在 OnlyOffice 中设置 ALLOW_PRIVATE_IP_ADDRESS=true
     */
    public String getPresignedUrlForDocker(String objectName, int expirySeconds) throws Exception {
        // 生成标准预签名 URL
        String presignedUrl = getPresignedUrl(objectName, expirySeconds);

        // 替换为 Docker 可访问的地址
        // 由于签名包含 host 信息，简单替换可能导致签名验证失败
        // 但 MinIO 在某些配置下会忽略 host 检查
        if (externalEndpoint != null && !externalEndpoint.isEmpty()) {
            // 提取原始 endpoint 的 host:port
            String originalHost = endpoint.replace("http://", "").replace("https://", "");
            String externalHost = externalEndpoint.replace("http://", "").replace("https://", "");
            presignedUrl = presignedUrl.replace(originalHost, externalHost);
        }

        return presignedUrl;
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
                            .build());

            String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
        }
    }

    /**
     * 列出所有文件对象
     * 
     * @return 文件信息列表（包含 objectName 和 size）
     */
    public List<MinioObjectInfo> listAllObjects() {
        List<MinioObjectInfo> objects = new ArrayList<>();

        if (minioClient == null) {
            log.warn("MinIO 客户端未初始化，无法列出文件");
            return objects;
        }

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true) // 递归列出所有子目录
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) { // 只收集文件，不收集目录
                    objects.add(new MinioObjectInfo(
                            item.objectName(),
                            item.size(),
                            item.lastModified() != null ? item.lastModified().toString() : null));
                }
            }

            log.info("MinIO 文件列表获取成功，共 {} 个文件", objects.size());
        } catch (Exception e) {
            log.error("获取 MinIO 文件列表失败", e);
        }

        return objects;
    }

    /**
     * 列出指定前缀的文件对象
     * 
     * @param prefix 前缀（如 "documents/"）
     * @return 文件信息列表
     */
    public List<MinioObjectInfo> listObjects(String prefix) {
        List<MinioObjectInfo> objects = new ArrayList<>();

        if (minioClient == null) {
            log.warn("MinIO 客户端未初始化，无法列出文件");
            return objects;
        }

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    objects.add(new MinioObjectInfo(
                            item.objectName(),
                            item.size(),
                            item.lastModified() != null ? item.lastModified().toString() : null));
                }
            }
        } catch (Exception e) {
            log.error("获取 MinIO 文件列表失败: prefix={}", prefix, e);
        }

        return objects;
    }

    /**
     * 获取存储桶总大小
     * 
     * @return 总字节数
     */
    public long getTotalSize() {
        return listAllObjects().stream()
                .mapToLong(MinioObjectInfo::getSize)
                .sum();
    }

    /**
     * 获取存储桶名称
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * MinIO 对象信息
     */
    public static class MinioObjectInfo {
        private final String objectName;
        private final long size;
        private final String lastModified;

        public MinioObjectInfo(String objectName, long size, String lastModified) {
            this.objectName = objectName;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getObjectName() {
            return objectName;
        }

        public long getSize() {
            return size;
        }

        public String getLastModified() {
            return lastModified;
        }
    }
}
