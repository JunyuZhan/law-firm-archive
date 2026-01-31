package com.lawfirm.infrastructure.external.minio;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 文件服务
 *
 * <p>线程安全：使用 @PostConstruct 在启动时初始化客户端，避免并发初始化问题
 *
 * @author system
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

  /** 预签名URL默认过期时间（秒）：2小时. */
  private static final int DEFAULT_PRESIGNED_URL_EXPIRY_SECONDS = 7200;

  /** MinIO服务端点地址. */
  @Value("${minio.endpoint}")
  private String endpoint;

  /** MinIO访问密钥. */
  @Value("${minio.access-key}")
  private String accessKey;

  /** MinIO秘密密钥. */
  @Value("${minio.secret-key}")
  private String secretKey;

  /** MinIO存储桶名称. */
  @Value("${minio.bucket-name}")
  private String bucketName;

  /** MinIO外部访问端点（可选）. */
  @Value("${minio.external-endpoint:#{null}}")
  private String externalEndpoint;

  /**
   * 浏览器访问端点（用于生成浏览器可访问的预签名 URL） 如果配置了此端点，预签名 URL 将使用此端点，浏览器可以直接访问 格式：http://你的域名或IP:9000 或通过 Nginx
   * 代理的地址
   */
  @Value("${minio.browser-endpoint:#{null}}")
  private String browserEndpoint;

  /** MinIO客户端实例. */
  private MinioClient minioClient;

  /** 外部访问的MinIO客户端实例. */
  private MinioClient externalMinioClient;

  /** 浏览器访问的MinIO客户端实例. */
  @SuppressWarnings("unused")
  private MinioClient browserMinioClient;

  /** 初始化 MinIO 客户端（启动时执行，线程安全） */
  @PostConstruct
  public void init() {
    log.info("初始化 MinIO 客户端: endpoint={}", endpoint);

    try {
      // 创建客户端
      minioClient =
          MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();

      // 确保bucket存在
      initializeBucket();

      log.info("MinIO 客户端初始化成功");
    } catch (Exception e) {
      log.error("MinIO 客户端初始化失败", e);
      // 不抛出异常，允许系统启动（MinIO 可能是可选服务）
      // 后续调用时会检查 minioClient 是否为 null
    }
  }

  /** 初始化 bucket（只在启动时调用一次） */
  private void initializeBucket() {
    try {
      boolean found =
          minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
      if (!found) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        log.info("创建 MinIO bucket: {}", bucketName);
      }
    } catch (Exception e) {
      log.error("MinIO bucket 检查/创建失败", e);
    }
  }

  /**
   * 获取 MinIO 客户端 如果客户端未初始化，抛出友好的业务异常而不是返回null导致NPE
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
   * @param file 文件对象
   * @return 文件的存储路径（Object Name）
   */
  public String uploadFile(final MultipartFile file) {
    return uploadFile(file, "");
  }

  /**
   * 上传文件到指定目录
   *
   * @param file 文件对象
   * @param directory 目录路径（例如 "contracts/2024/"）
   * @return 文件的存储路径（Object Name）
   */
  public String uploadFile(final MultipartFile file, final String directory) {
    try {
      // 生成唯一文件名
      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }

      // 确保目录以 / 结尾（如果非空）
      String dirPath = directory;
      if (dirPath != null && !dirPath.isEmpty() && !dirPath.endsWith("/")) {
        dirPath += "/";
      } else if (dirPath == null) {
        dirPath = "";
      }

      String fileName = dirPath + UUID.randomUUID().toString() + extension;

      // 上传
      InputStream inputStream = file.getInputStream();
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                  inputStream, file.getSize(), -1)
              .contentType(file.getContentType())
              .build());

      log.info("文件上传成功: {}", fileName);
      return fileName;
    } catch (Exception e) {
      log.error("文件上传失败", e);
      throw new RuntimeException("文件上传失败: " + e.getMessage());
    }
  }

  /**
   * 将 MinIO URL 转换为浏览器可访问的 URL 如果 URL 包含 Docker 内部地址（如 minio:9000），则生成预签名 URL
   *
   * <p>实现策略： 
   * 1. 使用内部 minioClient（minio:9000）生成预签名 URL
   * 2. 将绝对 URL 转换为相对路径 /minio/...，通过 nginx 代理访问
   * 3. 这样可以自动适配 HTTP/HTTPS，避免 Mixed Content 问题
   *
   * @param fileUrl 原始文件 URL
   * @return 浏览器可访问的 URL（相对路径或绝对路径）
   */
  public String getBrowserAccessibleUrl(final String fileUrl) {
    if (fileUrl == null || fileUrl.isEmpty()) {
      return fileUrl;
    }

    // 检查是否是 Docker 内部地址或 IP 地址（需要转换为相对路径）
    // 匹配模式：http://host:port/ 或 https://host:port/
    boolean needsConversion = fileUrl.contains("minio:9000")
        || fileUrl.contains("localhost:9000")
        || fileUrl.contains("127.0.0.1:9000")
        || fileUrl.contains("backend:8080")
        || fileUrl.matches("https?://\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+/"); // IP 地址模式
    
    if (needsConversion) {
      try {
        // 提取对象名称（去除查询参数）
        String objectName = extractObjectName(fileUrl);
        if (objectName != null) {
          // 使用内部 minioClient 生成预签名 URL（容器内只能访问内部地址）
          String presignedUrl =
              minioClient.getPresignedObjectUrl(
                  io.minio.GetPresignedObjectUrlArgs.builder()
                      .method(io.minio.http.Method.GET)
                      .bucket(bucketName)
                      .object(objectName)
                      .expiry(DEFAULT_PRESIGNED_URL_EXPIRY_SECONDS) // 2小时有效
                      .build());

          // 优先使用相对路径 /minio/，通过 nginx 代理，自动适配 HTTP/HTTPS
          // 这样可以避免 Mixed Content 问题（HTTPS 页面加载 HTTP 资源被阻止）
          if (browserEndpoint != null && browserEndpoint.equals("/minio")) {
            // 使用相对路径模式：将任何 host（包括 IP 地址）替换为相对路径 /minio/
            // 处理各种可能的 URL 格式：
            // - http://minio:9000/bucket/object?query
            // - http://192.168.50.10:9000/bucket/object?query
            // - http://localhost:9000/bucket/object?query
            try {
              // 使用 URI 解析 URL（避免 Java 20+ 的 URL(String) 弃用警告）
              java.net.URI uri = new java.net.URI(presignedUrl);
              String path = uri.getPath(); // 例如：/law-firm/thumbnails/file.jpg
              String query = uri.getQuery(); // 例如：X-Amz-Algorithm=...
              
              // 构建相对路径 URL：/minio/path?query
              if (query != null && !query.isEmpty()) {
                presignedUrl = "/minio" + path + "?" + query;
              } else {
                presignedUrl = "/minio" + path;
              }
              
              log.debug("使用相对路径模式: {} -> {}", presignedUrl, presignedUrl);
            } catch (Exception e) {
              // 如果 URI 解析失败，回退到简单的字符串替换
              log.warn("URI 解析失败，使用简单替换: {}", presignedUrl, e);
              // 替换常见的 host 模式
              presignedUrl = presignedUrl.replaceAll("https?://[^/]+/", "/minio/");
              log.debug("使用简单替换模式: -> {}", presignedUrl);
            }
          } else if (browserEndpoint != null && !browserEndpoint.isEmpty()) {
            // 使用绝对路径模式（向后兼容）
            // 同样需要处理 IP 地址的情况
            try {
              // 使用 URI 解析 URL（避免 Java 20+ 的 URL(String) 弃用警告）
              java.net.URI uri = new java.net.URI(presignedUrl);
              String path = uri.getPath();
              String query = uri.getQuery();
              
              // 构建新的 URL
              if (query != null && !query.isEmpty()) {
                presignedUrl = browserEndpoint + path + "?" + query;
              } else {
                presignedUrl = browserEndpoint + path;
              }
              
              log.debug(
                  "将内部预签名 URL 替换为浏览器可访问地址: {} -> browserEndpoint={}",
                  presignedUrl,
                  browserEndpoint);
            } catch (Exception e) {
              // 回退到简单替换
              log.warn("URI 解析失败，使用简单替换: {}", presignedUrl, e);
              presignedUrl = presignedUrl.replaceAll("https?://[^/]+", browserEndpoint);
            }
          }

          log.debug("生成浏览器可访问的预签名 URL: {} -> {}", fileUrl, presignedUrl);
          return presignedUrl;
        }
      } catch (Exception e) {
        log.warn("无法生成预签名 URL，使用原始 URL: {}", fileUrl, e);
      }
    }

    // 如果已经是外部可访问的 URL，直接返回
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
   * @throws Exception 上传失败时抛出异常
   */
  public String uploadFile(
      final InputStream inputStream,
      final String fileName,
      final String folder,
      final String contentType)
      throws Exception {
    String objectName = folder + fileName;

    // 获取文件大小（如果可能）
    long fileSize = inputStream.available();
    if (fileSize <= 0) {
      fileSize = -1; // 未知大小
    }

    getMinioClient()
        .putObject(
            PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                    inputStream, fileSize, -1)
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
   * @param objectName 完整的对象路径名称
   * @param contentType 内容类型
   * @return 文件URL
   * @throws Exception 上传失败时抛出异常
   */
  public String uploadFile(
      final InputStream inputStream, final String objectName, final String contentType)
      throws Exception {
    // 获取文件大小（如果可能）
    long fileSize = inputStream.available();
    if (fileSize <= 0) {
      fileSize = -1; // 未知大小
    }

    getMinioClient()
        .putObject(
            PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                    inputStream, fileSize, -1)
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
   * @throws Exception 下载失败时抛出异常
   */
  public InputStream downloadFile(final String objectName) throws Exception {
    return getMinioClient()
        .getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
  }

  /**
   * 删除文件
   *
   * @param objectName 对象名称（完整路径）
   * @throws Exception 删除失败时抛出异常
   */
  public void deleteFile(final String objectName) throws Exception {
    getMinioClient()
        .removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    log.info("文件删除成功: {}", objectName);
  }

  /**
   * 从URL提取对象名称
   *
   * @param fileUrl 文件URL
   * @return 对象名称
   */
  public String extractObjectName(final String fileUrl) {
    if (fileUrl == null || !fileUrl.contains(bucketName + "/")) {
      return null;
    }
    int index = fileUrl.indexOf(bucketName + "/");
    String objectName = fileUrl.substring(index + bucketName.length() + 1);

    // 1. 先进行 URL 解码 (处理 %3F, %25E8 等)
    try {
      objectName =
          java.net.URLDecoder.decode(objectName, java.nio.charset.StandardCharsets.UTF_8.name());
    } catch (Exception e) {
      log.warn("文件名URL解码失败: {}", objectName);
    }

    // 2. 解码后再去除查询参数 (此时 %3F 已变成 ?)
    if (objectName.contains("?")) {
      objectName = objectName.substring(0, objectName.indexOf("?"));
    }

    return objectName;
  }

  /**
   * 获取外部访问的 MinioClient（用于 Docker 容器等外部服务访问）.
   *
   * @return 外部访问的MinioClient实例
   */
  private MinioClient getExternalMinioClient() {
    if (externalMinioClient == null) {
      String extEndpoint = externalEndpoint != null ? externalEndpoint : endpoint;
      externalMinioClient =
          MinioClient.builder().endpoint(extEndpoint).credentials(accessKey, secretKey).build();
    }
    return externalMinioClient;
  }

  /**
   * 生成预签名URL（用于临时访问）
   *
   * @param objectName 对象名称
   * @param expirySeconds 过期时间（秒）
   * @return 预签名URL
   * @throws Exception 生成失败时抛出异常
   */
  public String getPresignedUrl(final String objectName, final int expirySeconds) throws Exception {
    // MinIO Java SDK 8.x 使用 getPresignedObjectUrl
    return getMinioClient()
        .getPresignedObjectUrl(
            io.minio.GetPresignedObjectUrlArgs.builder()
                .method(io.minio.http.Method.GET)
                .bucket(bucketName)
                .object(objectName)
                .expiry(expirySeconds)
                .build());
  }

  /**
   * 生成 Docker 容器可访问的预签名URL 使用独立的 MinioClient（endpoint 为 Docker 内部地址）生成 URL 这样生成的签名与访问时使用的 host
   * 一致，不会出现签名验证失败 注意：需要在 OnlyOffice 中设置 ALLOW_PRIVATE_IP_ADDRESS=true
   *
   * @param objectName 对象名称
   * @param expirySeconds 过期时间（秒）
   * @return 预签名URL
   * @throws Exception 生成失败时抛出异常
   */
  public String getPresignedUrlForDocker(final String objectName, final int expirySeconds)
      throws Exception {
    // 如果配置了外部端点，使用专门的客户端生成 URL
    // 这样签名会使用正确的 host（如 minio:9000），避免签名不匹配
    if (externalEndpoint != null && !externalEndpoint.isEmpty()) {
      log.info("使用外部端点生成预签名URL: externalEndpoint={}, objectName={}", externalEndpoint, objectName);
      return getExternalMinioClient()
          .getPresignedObjectUrl(
              io.minio.GetPresignedObjectUrlArgs.builder()
                  .method(io.minio.http.Method.GET)
                  .bucket(bucketName)
                  .object(objectName)
                  .expiry(expirySeconds)
                  .build());
    }

    // 没有配置外部端点时，回退到标准 URL
    log.info("使用标准端点生成预签名URL: endpoint={}, objectName={}", endpoint, objectName);
    return getPresignedUrl(objectName, expirySeconds);
  }

  /**
   * 下载文件为字节数组
   *
   * @param objectName 对象名称
   * @return 文件字节数组
   * @throws Exception 下载失败时抛出异常
   */
  public byte[] downloadFileAsBytes(final String objectName) throws Exception {
    try (InputStream inputStream = downloadFile(objectName)) {
      return inputStream.readAllBytes();
    }
  }

  /**
   * 上传字节数组
   *
   * @param bytes 字节数组
   * @param objectName 对象名称
   * @param contentType 内容类型
   * @return 文件URL
   * @throws Exception 上传失败时抛出异常
   */
  public String uploadBytes(final byte[] bytes, final String objectName, final String contentType)
      throws Exception {
    try (InputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
      getMinioClient()
          .putObject(
              PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                      inputStream, bytes.length, -1)
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
      Iterable<Result<Item>> results =
          minioClient.listObjects(
              ListObjectsArgs.builder()
                  .bucket(bucketName)
                  .recursive(true) // 递归列出所有子目录
                  .build());

      for (Result<Item> result : results) {
        Item item = result.get();
        if (!item.isDir()) { // 只收集文件，不收集目录
          objects.add(
              new MinioObjectInfo(
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
  public List<MinioObjectInfo> listObjects(final String prefix) {
    List<MinioObjectInfo> objects = new ArrayList<>();

    if (minioClient == null) {
      log.warn("MinIO 客户端未初始化，无法列出文件");
      return objects;
    }

    try {
      Iterable<Result<Item>> results =
          minioClient.listObjects(
              ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(true).build());

      for (Result<Item> result : results) {
        Item item = result.get();
        if (!item.isDir()) {
          objects.add(
              new MinioObjectInfo(
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
    return listAllObjects().stream().mapToLong(MinioObjectInfo::getSize).sum();
  }

  /**
   * 获取存储桶名称.
   *
   * @return 存储桶名称
   */
  public String getBucketName() {
    return bucketName;
  }

  /**
   * 获取端点地址.
   *
   * @return 端点地址
   */
  public String getEndpoint() {
    return endpoint;
  }

  /**
   * 构建完整文件URL
   *
   * @param objectName 对象名称
   * @return 完整文件URL
   */
  public String buildFileUrl(final String objectName) {
    return endpoint + "/" + bucketName + "/" + objectName;
  }

  /** MinIO 对象信息 */
  public static class MinioObjectInfo {
    /** 对象名称 */
    private final String objectName;

    /** 文件大小 */
    private final long size;

    /** 最后修改时间 */
    private final String lastModified;

    /**
     * 构造函数
     *
     * @param objectName 对象名称
     * @param size 文件大小
     * @param lastModified 最后修改时间
     */
    public MinioObjectInfo(final String objectName, final long size, final String lastModified) {
      this.objectName = objectName;
      this.size = size;
      this.lastModified = lastModified;
    }

    /**
     * 获取对象名称
     *
     * @return 对象名称
     */
    public String getObjectName() {
      return objectName;
    }

    /**
     * 获取文件大小
     *
     * @return 文件大小
     */
    public long getSize() {
      return size;
    }

    /**
     * 获取最后修改时间
     *
     * @return 最后修改时间
     */
    public String getLastModified() {
      return lastModified;
    }
  }
}
