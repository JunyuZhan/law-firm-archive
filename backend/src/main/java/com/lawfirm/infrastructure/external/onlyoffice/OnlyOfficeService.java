package com.lawfirm.infrastructure.external.onlyoffice;

import com.lawfirm.infrastructure.config.OnlyOfficeConfig;
import com.lawfirm.infrastructure.external.minio.MinioService;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OnlyOffice 文档服务.
 *
 * <p>提供在线文档编辑和预览功能
 *
 * <p>安全增强： - 支持 JWT 签名验证，防止配置篡改 - 回调请求 JWT 验证
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlyOfficeService {

  /** OnlyOffice配置. */
  private final OnlyOfficeConfig config;

  /** MinIO文件服务. */
  private final MinioService minioService;

  /** 文档访问 Token 密钥（从配置注入，与 DocumentController 保持一致）. */
  @Value("${law-firm.document.token-secret}")
  private String documentTokenSecret;

  /** 预签名URL默认过期时间（秒）：2小时. */
  private static final int DEFAULT_PRESIGNED_URL_EXPIRY_SECONDS = 7200;

  /**
   * 生成文档编辑器配置.
   *
   * @param fileUrl 文件URL（MinIO 中的路径）
   * @param fileName 文件名
   * @param documentKey 文档唯一标识（用于协同编辑，同一个 key 的文档共享编辑状态）
   * @param userId 当前用户ID
   * @param userName 当前用户名
   * @param mode 模式：view（只读）、edit（编辑）
   * @param callbackPath 回调路径（相对路径）
   * @return OnlyOffice 编辑器配置
   */
  public Map<String, Object> generateEditorConfig(
      final String fileUrl,
      final String fileName,
      final String documentKey,
      final Long userId,
      final String userName,
      final String mode,
      final String callbackPath) {

    Map<String, Object> editorConfig = new HashMap<>();

    // 文档配置
    Map<String, Object> document = new HashMap<>();
    // 根据文件 URL 或 MIME 类型推断正确的文件扩展名
    String fileType = inferFileTypeFromUrl(fileUrl, fileName);
    document.put("fileType", fileType);
    String docKey = documentKey != null ? documentKey : generateDocumentKey();
    document.put("key", docKey);
    document.put("title", fileName);
    // 构建文件 URL（如果已经是代理 URL，直接使用；否则尝试转换）
    String finalFileUrl = buildFileUrl(fileUrl);
    log.info(
        "OnlyOffice 配置生成 - 文件信息: fileName={}, fileType={}, documentKey={}, originalUrl={}, finalUrl={}",
        fileName,
        fileType,
        docKey,
        fileUrl,
        finalFileUrl);
    document.put("url", finalFileUrl);

    // 权限配置
    Map<String, Object> permissions = new HashMap<>();
    permissions.put("comment", true);
    permissions.put("download", true);
    permissions.put("edit", "edit".equals(mode));
    permissions.put("print", true);
    permissions.put("review", true);
    permissions.put("chat", false); // chat 参数已从 customization 移到 permissions
    document.put("permissions", permissions);

    editorConfig.put("document", document);

    // 文档类型（word/cell/slide）
    editorConfig.put("documentType", getDocumentType(fileName));

    // 编辑器配置
    Map<String, Object> editorSettings = new HashMap<>();
    editorSettings.put("mode", mode);
    editorSettings.put("lang", "zh-CN");

    // 回调URL（保存时调用）
    if ("edit".equals(mode) && callbackPath != null) {
      editorSettings.put("callbackUrl", this.config.getCallbackUrl() + callbackPath);
    }

    // 用户信息
    Map<String, Object> user = new HashMap<>();
    user.put("id", userId != null ? userId.toString() : "anonymous");
    user.put("name", userName != null ? userName : "匿名用户");
    editorSettings.put("user", user);

    // 自定义配置
    Map<String, Object> customization = new HashMap<>();
    customization.put("autosave", true);
    // chat 参数已移到 permissions 中，不再在 customization 中设置
    customization.put("comments", true);
    customization.put("compactHeader", false);
    customization.put("compactToolbar", false);
    customization.put("help", false);
    customization.put("hideRightMenu", false);
    customization.put("hideRulers", false);
    customization.put("plugins", false);
    customization.put("toolbarNoTabs", false);
    customization.put("forcesave", true); // 强制保存

    // Logo 配置
    Map<String, Object> logo = new HashMap<>();
    logo.put("image", "");
    logo.put("imageEmbedded", "");
    logo.put("url", "");
    customization.put("logo", logo);

    editorSettings.put("customization", customization);

    editorConfig.put("editorConfig", editorSettings);

    // 尺寸
    editorConfig.put("height", "100%");
    editorConfig.put("width", "100%");
    editorConfig.put("type", "desktop");

    // OnlyOffice Document Server URL（供前端参考，但不会放入 JWT）
    // 前端会自动检测当前域名来替换这些值
    // 如果配置的是 localhost、127.0.0.1 或 IP 地址，返回相对路径让前端自动处理
    // 这样可以避免 Mixed Content 问题（HTTPS 页面加载 HTTP 资源）
    String documentServerUrl = this.config.getDocumentServerUrl();
    if (documentServerUrl != null) {
      // 检测是否是无效地址（localhost、127.0.0.1、IP 地址）
      boolean isInvalidUrl = documentServerUrl.contains("localhost")
          || documentServerUrl.contains("127.0.0.1")
          || documentServerUrl.matches("https?://\\d+\\.\\d+\\.\\d+\\.\\d+"); // IP 地址模式
      
      if (isInvalidUrl) {
        // 返回相对路径，前端会自动转换为当前域名 + /onlyoffice
        documentServerUrl = "/onlyoffice";
        log.debug("OnlyOffice URL 包含无效地址，已转换为相对路径: /onlyoffice");
      } else if (documentServerUrl.startsWith("/")) {
        // 如果已经是相对路径，直接使用
        log.debug("OnlyOffice URL 已是相对路径: {}", documentServerUrl);
      }
    }
    // Ensure documentServerUrl is not null to prevent NPE
    if (documentServerUrl == null) {
      documentServerUrl = "/onlyoffice";
    }
    editorConfig.put("documentServerUrl", documentServerUrl);
    editorConfig.put("apiJsUrl", documentServerUrl + "/web-apps/apps/api/documents/api.js");

    // JWT 签名（如果启用）
    // 注意：生成 JWT 时排除 documentServerUrl 和 apiJsUrl，让前端自动检测域名
    if (this.config.isJwtEnabled()
        && this.config.getJwtSecret() != null
        && !this.config.getJwtSecret().isEmpty()) {
      // 创建用于签名的配置副本（排除 URL 相关字段）
      Map<String, Object> jwtPayload = new HashMap<>(editorConfig);
      jwtPayload.remove("documentServerUrl");
      jwtPayload.remove("apiJsUrl");

      String token = generateJwtToken(jwtPayload);
      editorConfig.put("token", token);
      log.debug("OnlyOffice JWT 签名已添加（URL 由前端自动检测）");
    }

    log.info(
        "生成 OnlyOffice 配置: fileName={}, mode={}, documentKey={}, url={}, callbackUrl={}, jwtEnabled={}",
        fileName,
        mode,
        document.get("key"),
        document.get("url"),
        editorSettings.get("callbackUrl"),
        this.config.isJwtEnabled());

    return editorConfig;
  }

  /**
   * 生成 OnlyOffice JWT Token 用于验证配置未被篡改.
   *
   * <p>重要：OnlyOffice 使用标准的 HMAC-SHA256 算法验证 JWT。 必须使用 javax.crypto.spec.SecretKeySpec 直接创建密钥， 避免
   * JJWT 的 Keys.hmacShaKeyFor() 方法根据密钥长度自动选择错误的算法。
   *
   * @param payload JWT载荷数据
   * @return JWT令牌字符串
   */
  private String generateJwtToken(final Map<String, Object> payload) {
    try {
      // 直接使用 SecretKeySpec 创建 HMAC-SHA256 密钥
      // 这样可以确保无论密钥长度如何，都使用 HS256 算法
      byte[] keyBytes = this.config.getJwtSecret().getBytes(StandardCharsets.UTF_8);
      SecretKey key = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");

      return Jwts.builder().claims(payload).signWith(key, Jwts.SIG.HS256).compact();
    } catch (Exception e) {
      log.error("生成 OnlyOffice JWT Token 失败: {}", e.getMessage());
      throw new RuntimeException("生成 JWT Token 失败", e);
    }
  }

  /**
   * 验证 OnlyOffice 回调请求的 JWT Token
   *
   * @param token JWT Token
   * @return 解析后的 payload
   */
  public Map<String, Object> verifyCallbackToken(final String token) {
    if (!this.config.isJwtEnabled()
        || this.config.getJwtSecret() == null
        || this.config.getJwtSecret().isEmpty()) {
      log.warn("OnlyOffice JWT 未启用，跳过验证");
      return null;
    }

    try {
      // 使用 SecretKeySpec 创建密钥，与生成方法保持一致
      byte[] keyBytes = this.config.getJwtSecret().getBytes(StandardCharsets.UTF_8);
      SecretKey key = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");

      var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

      return new HashMap<>(claims);
    } catch (Exception e) {
      log.error("OnlyOffice 回调 JWT 验证失败: {}", e.getMessage());
      throw new RuntimeException("JWT 验证失败", e);
    }
  }

  /**
   * 检查 JWT 是否已启用.
   *
   * @return 如果JWT已启用返回true，否则返回false
   */
  public boolean isJwtEnabled() {
    return this.config.isJwtEnabled()
        && this.config.getJwtSecret() != null
        && !this.config.getJwtSecret().isEmpty();
  }

  /**
   * 生成预览配置（只读模式）.
   *
   * @param fileUrl 文件URL
   * @param fileName 文件名
   * @param userId 用户ID
   * @param userName 用户名
   * @return OnlyOffice预览配置
   */
  public Map<String, Object> generateViewConfig(
      final String fileUrl, final String fileName, final Long userId, final String userName) {
    return generateEditorConfig(fileUrl, fileName, null, userId, userName, "view", null);
  }

  /**
   * 生成编辑配置.
   *
   * @param fileUrl 文件URL
   * @param fileName 文件名
   * @param documentKey 文档唯一标识
   * @param userId 用户ID
   * @param userName 用户名
   * @param callbackPath 回调路径
   * @return OnlyOffice编辑配置
   */
  public Map<String, Object> generateEditConfig(
      final String fileUrl,
      final String fileName,
      final String documentKey,
      final Long userId,
      final String userName,
      final String callbackPath) {
    return generateEditorConfig(
        fileUrl, fileName, documentKey, userId, userName, "edit", callbackPath);
  }

  /**
   * 构建文件访问URL（供 OnlyOffice 使用）.
   *
   * <p>优先使用后端代理接口，确保 OnlyOffice 容器能够访问 OnlyOffice 运行在 Docker 容器中，使用 MinIO 预签名 URL 需要设置 OnlyOffice
   * 环境变量 ALLOW_PRIVATE_IP_ADDRESS=true
   *
   * @param fileUrl 原始文件URL
   * @return 构建后的文件访问URL
   */
  public String buildFileUrl(final String fileUrl) {
    // 如果 fileUrl 已经是代理 URL，直接返回
    // 优先检查 /file-proxy（新的代理接口）
    if (fileUrl != null && fileUrl.contains("/file-proxy")) {
      log.debug("文件 URL 已经是代理 URL（file-proxy）: {}", fileUrl);
      return fileUrl;
    }
    // 兼容旧的 /content 接口（但应该迁移到 /file-proxy）
    if (fileUrl != null && fileUrl.contains("/document/") && fileUrl.contains("/content")) {
      log.warn("检测到旧的 /content 接口 URL，建议迁移到 /file-proxy: {}", fileUrl);
      return fileUrl;
    }

    try {
      // 从 MinIO 文件路径生成预签名 URL
      String objectName = minioService.extractObjectName(fileUrl);
      if (objectName != null) {
        // 使用 Docker 可访问的预签名 URL（2小时有效）
        // 在 Linux Docker 环境中，host.docker.internal 可能不可用
        // 必须强制将主机名替换为 Docker 网络内部的服务名 "minio"
        String url =
            minioService.getPresignedUrlForDocker(objectName, DEFAULT_PRESIGNED_URL_EXPIRY_SECONDS);
        if (url != null) {
          String originalUrl = url;
          // 更全面地替换 URL，确保 OnlyOffice 容器能够访问
          // 替换各种可能的地址格式
          url =
              url.replace("host.docker.internal:9000", "minio:9000")
                  .replace("host.docker.internal", "minio")
                  .replace("localhost:9000", "minio:9000")
                  .replace("127.0.0.1:9000", "minio:9000")
                  .replace("localhost", "minio")
                  .replace("127.0.0.1", "minio");

          // 使用正则表达式替换可能包含端口的 URL（但保留 minio:9000）
          // 例如：http://192.168.x.x:9000 -> http://minio:9000
          if (!url.contains("minio:9000")) {
            url = url.replaceAll("http://([^/:]+):9000", "http://minio:9000");
          }

          if (!originalUrl.equals(url)) {
            log.info("OnlyOffice 文件 URL 已替换: {} -> {}", originalUrl, url);
          } else {
            log.debug("OnlyOffice 文件 URL（无需替换）: {}", url);
          }
          return url;
        }
      }
      log.warn("无法从文件路径提取对象名，使用原始 URL: {}", fileUrl);
      return fileUrl;
    } catch (Exception e) {
      log.error("生成预签名URL失败，使用原始URL: {}", e.getMessage(), e);
      return fileUrl;
    }
  }

  /**
   * 构建文件访问URL（使用文档ID，供 OnlyOffice 使用）.
   *
   * <p>使用后端代理接口，确保 OnlyOffice 容器能够访问 包含临时 token 以提高安全性
   *
   * <p>重要：OnlyOffice 容器在 Docker 网络中运行，必须使用 Docker 内部地址（backend:8080） 不能使用外部 IP 地址，因为 OnlyOffice
   * 容器可能无法访问外部网络
   *
   * @param documentId 文档ID
   * @return 文档访问URL
   */
  public String buildFileUrlForDocument(final Long documentId) {
    // 始终使用 Docker 内部地址（backend:8080）
    // OnlyOffice 容器通过 Docker 网络直接访问 backend 容器
    String baseUrl = this.config.getCallbackUrl();
    // 移除末尾的 /api（如果有）
    if (baseUrl.endsWith("/api")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
    }
    log.info(
        "生成 OnlyOffice 文件 URL（Docker 内部地址）: callbackUrl={}, baseUrl={}",
        this.config.getCallbackUrl(),
        baseUrl);

    // 生成临时 token（2小时有效）
    long expires = System.currentTimeMillis() + DEFAULT_PRESIGNED_URL_EXPIRY_SECONDS * 1000;
    String token = generateDocumentAccessToken(documentId, expires);

    // URL 编码 token，避免特殊字符导致 URL 解析错误
    String encodedToken =
        java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);

    // 确保 baseUrl 格式正确（移除末尾的斜杠）
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }

    String proxyUrl =
        baseUrl
            + "/api/document/"
            + documentId
            + "/file-proxy?token="
            + encodedToken
            + "&expires="
            + expires;
    log.info(
        "生成 OnlyOffice 文档代理 URL（带token）: documentId={}, baseUrl={}, url={}",
        documentId,
        baseUrl,
        proxyUrl);
    return proxyUrl;
  }

  /**
   * 生成文档访问 token.
   *
   * <p>使用配置中的密钥，与 DocumentController 保持一致
   *
   * @param documentId 文档ID
   * @param expires 过期时间戳
   * @return 文档访问令牌
   */
  private String generateDocumentAccessToken(final Long documentId, final long expires) {
    // 使用从配置注入的密钥（与 DocumentController 共享同一配置）
    String data = documentId + ":" + expires + ":" + documentTokenSecret;
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(hash).substring(0, 32);
    } catch (Exception e) {
      log.error("生成文档访问 token 失败", e);
      throw new RuntimeException("生成 token 失败", e);
    }
  }

  /**
   * 生成文档唯一标识.
   *
   * @return 文档唯一标识字符串
   */
  private String generateDocumentKey() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * 获取文件扩展名.
   *
   * @param fileName 文件名
   * @return 文件扩展名
   */
  private String getFileExtension(final String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "docx";
    }
    return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
  }

  /**
   * 根据文件 URL 和文件名推断正确的文件类型.
   *
   * <p>如果文件名扩展名与实际内容不匹配，根据 URL 中的信息或 MIME 类型推断
   *
   * @param fileUrl 文件URL
   * @param fileName 文件名
   * @return 推断的文件类型
   */
  private String inferFileTypeFromUrl(final String fileUrl, final String fileName) {
    String ext = getFileExtension(fileName);

    // 如果 URL 包含 /file-proxy，说明是代理接口，需要根据实际文件内容判断
    // 这里我们根据常见的 MIME 类型映射来推断
    // 如果文件名是 .doc 但实际可能是 .docx，需要特殊处理

    // 检查 URL 中是否有文件扩展名信息
    if (fileUrl != null && fileUrl.contains("/file-proxy")) {
      // 代理接口，无法从 URL 判断，使用文件名扩展名
      // 但如果文件名是 .doc，OnlyOffice 可能无法正确处理，需要根据实际内容判断
      // 这里先返回文件名扩展名，如果 OnlyOffice 报错，再根据 MIME 类型修正
      return ext;
    }

    // 尝试从 URL 中提取扩展名
    if (fileUrl != null) {
      int lastDot = fileUrl.lastIndexOf(".");
      int lastSlash = fileUrl.lastIndexOf("/");
      if (lastDot > lastSlash && lastDot < fileUrl.length() - 1) {
        String urlExt = fileUrl.substring(lastDot + 1).toLowerCase();
        // 移除查询参数
        if (urlExt.contains("?")) {
          urlExt = urlExt.substring(0, urlExt.indexOf("?"));
        }
        if (urlExt.contains("&")) {
          urlExt = urlExt.substring(0, urlExt.indexOf("&"));
        }
        // 如果 URL 中的扩展名与文件名不同，优先使用 URL 中的
        if (!urlExt.isEmpty() && !urlExt.equals(ext)) {
          log.info(
              "文件扩展名不一致: fileName={}, fileNameExt={}, urlExt={}, 使用URL扩展名", fileName, ext, urlExt);
          return urlExt;
        }
      }
    }

    return ext;
  }

  /**
   * 获取文档类型.
   *
   * <p>word: 文档 cell: 表格 slide: 演示文稿
   *
   * @param fileName 文件名
   * @return 文档类型
   */
  private String getDocumentType(final String fileName) {
    String ext = getFileExtension(fileName);
    return switch (ext) {
      case "xls", "xlsx", "csv", "ods" -> "cell";
      case "ppt", "pptx", "odp" -> "slide";
      default -> "word";
    };
  }

  /**
   * 判断文件是否支持 OnlyOffice 编辑.
   *
   * @param fileName 文件名
   * @return 如果支持编辑返回true，否则返回false
   */
  public boolean isSupported(final String fileName) {
    String ext = getFileExtension(fileName);
    return switch (ext) {
      case "doc",
          "docx",
          "odt",
          "rtf",
          "txt",
          "xls",
          "xlsx",
          "ods",
          "csv",
          "ppt",
          "pptx",
          "odp" -> true;
      default -> false;
    };
  }

  /**
   * 判断文件是否支持预览（包括 PDF）.
   *
   * @param fileName 文件名
   * @return 如果支持预览返回true，否则返回false
   */
  public boolean isPreviewSupported(final String fileName) {
    String ext = getFileExtension(fileName);
    return switch (ext) {
      case "doc",
          "docx",
          "odt",
          "rtf",
          "txt",
          "xls",
          "xlsx",
          "ods",
          "csv",
          "ppt",
          "pptx",
          "odp",
          "pdf" -> true;
      default -> false;
    };
  }
}
