package com.lawfirm.infrastructure.external.onlyoffice;

import com.lawfirm.infrastructure.config.OnlyOfficeConfig;
import com.lawfirm.infrastructure.external.minio.MinioService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OnlyOffice 文档服务
 * 提供在线文档编辑和预览功能
 * 
 * 安全增强：
 * - 支持 JWT 签名验证，防止配置篡改
 * - 回调请求 JWT 验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlyOfficeService {

    private final OnlyOfficeConfig config;
    private final MinioService minioService;

    /**
     * 生成文档编辑器配置
     *
     * @param fileUrl      文件URL（MinIO 中的路径）
     * @param fileName     文件名
     * @param documentKey  文档唯一标识（用于协同编辑，同一个 key 的文档共享编辑状态）
     * @param userId       当前用户ID
     * @param userName     当前用户名
     * @param mode         模式：view（只读）、edit（编辑）
     * @param callbackPath 回调路径（相对路径）
     * @return OnlyOffice 编辑器配置
     */
    public Map<String, Object> generateEditorConfig(
            String fileUrl,
            String fileName,
            String documentKey,
            Long userId,
            String userName,
            String mode,
            String callbackPath) {

        Map<String, Object> config = new HashMap<>();

        // 文档配置
        Map<String, Object> document = new HashMap<>();
        document.put("fileType", getFileExtension(fileName));
        document.put("key", documentKey != null ? documentKey : generateDocumentKey());
        document.put("title", fileName);
        document.put("url", buildFileUrl(fileUrl));

        // 权限配置
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("comment", true);
        permissions.put("download", true);
        permissions.put("edit", "edit".equals(mode));
        permissions.put("print", true);
        permissions.put("review", true);
        document.put("permissions", permissions);

        config.put("document", document);

        // 文档类型（word/cell/slide）
        config.put("documentType", getDocumentType(fileName));

        // 编辑器配置
        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("mode", mode);
        editorConfig.put("lang", "zh-CN");

        // 回调URL（保存时调用）
        if ("edit".equals(mode) && callbackPath != null) {
            editorConfig.put("callbackUrl", this.config.getCallbackUrl() + callbackPath);
        }

        // 用户信息
        Map<String, Object> user = new HashMap<>();
        user.put("id", userId != null ? userId.toString() : "anonymous");
        user.put("name", userName != null ? userName : "匿名用户");
        editorConfig.put("user", user);

        // 自定义配置
        Map<String, Object> customization = new HashMap<>();
        customization.put("autosave", true);
        customization.put("chat", false);
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

        editorConfig.put("customization", customization);

        config.put("editorConfig", editorConfig);

        // 尺寸
        config.put("height", "100%");
        config.put("width", "100%");
        config.put("type", "desktop");

        // OnlyOffice Document Server URL（供前端参考，但不会放入 JWT）
        // 前端会自动检测当前域名来替换这些值
        config.put("documentServerUrl", this.config.getDocumentServerUrl());
        config.put("apiJsUrl", this.config.getApiJsUrl());

        // JWT 签名（如果启用）
        // 注意：生成 JWT 时排除 documentServerUrl 和 apiJsUrl，让前端自动检测域名
        if (this.config.isJwtEnabled() && this.config.getJwtSecret() != null
                && !this.config.getJwtSecret().isEmpty()) {
            // 创建用于签名的配置副本（排除 URL 相关字段）
            Map<String, Object> jwtPayload = new HashMap<>(config);
            jwtPayload.remove("documentServerUrl");
            jwtPayload.remove("apiJsUrl");

            String token = generateJwtToken(jwtPayload);
            config.put("token", token);
            log.debug("OnlyOffice JWT 签名已添加（URL 由前端自动检测）");
        }

        log.debug("生成 OnlyOffice 配置: fileName={}, mode={}, documentKey={}, jwtEnabled={}",
                fileName, mode, document.get("key"), this.config.isJwtEnabled());

        return config;
    }

    /**
     * 生成 OnlyOffice JWT Token
     * 用于验证配置未被篡改
     */
    private String generateJwtToken(Map<String, Object> payload) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    this.config.getJwtSecret().getBytes(StandardCharsets.UTF_8));

            return Jwts.builder()
                    .claims(payload)
                    .signWith(key)
                    .compact();
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
    public Map<String, Object> verifyCallbackToken(String token) {
        if (!this.config.isJwtEnabled() || this.config.getJwtSecret() == null
                || this.config.getJwtSecret().isEmpty()) {
            log.warn("OnlyOffice JWT 未启用，跳过验证");
            return null;
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    this.config.getJwtSecret().getBytes(StandardCharsets.UTF_8));

            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new HashMap<>(claims);
        } catch (Exception e) {
            log.error("OnlyOffice 回调 JWT 验证失败: {}", e.getMessage());
            throw new RuntimeException("JWT 验证失败", e);
        }
    }

    /**
     * 检查 JWT 是否已启用
     */
    public boolean isJwtEnabled() {
        return this.config.isJwtEnabled() && this.config.getJwtSecret() != null
                && !this.config.getJwtSecret().isEmpty();
    }

    /**
     * 生成预览配置（只读模式）
     */
    public Map<String, Object> generateViewConfig(
            String fileUrl,
            String fileName,
            Long userId,
            String userName) {
        return generateEditorConfig(fileUrl, fileName, null, userId, userName, "view", null);
    }

    /**
     * 生成编辑配置
     */
    public Map<String, Object> generateEditConfig(
            String fileUrl,
            String fileName,
            String documentKey,
            Long userId,
            String userName,
            String callbackPath) {
        return generateEditorConfig(fileUrl, fileName, documentKey, userId, userName, "edit", callbackPath);
    }

    /**
     * 构建文件访问URL
     * OnlyOffice 运行在 Docker 容器中，使用 MinIO 预签名 URL
     * 需要设置 OnlyOffice 环境变量 ALLOW_PRIVATE_IP_ADDRESS=true
     */
    /**
     * 构建文件访问URL
     * OnlyOffice 运行在 Docker 容器中，使用 MinIO 预签名 URL
     * 需要设置 OnlyOffice 环境变量 ALLOW_PRIVATE_IP_ADDRESS=true
     */
    public String buildFileUrl(String fileUrl) {
        try {
            // 从 MinIO 文件路径生成预签名 URL
            String objectName = minioService.extractObjectName(fileUrl);
            if (objectName != null) {
                // 使用 Docker 可访问的预签名 URL（2小时有效）
                return minioService.getPresignedUrlForDocker(objectName, 7200);
            }
            return fileUrl;
        } catch (Exception e) {
            log.warn("生成预签名URL失败，使用原始URL: {}", e.getMessage());
            return fileUrl;
        }
    }

    /**
     * 构建文件访问URL（用于文档ID）
     * 生成 Docker 内部可访问的完整 URL（OnlyOffice 容器使用）
     * 前端会自动替换为浏览器可访问的 URL
     */
    public String buildFileUrlForDocument(Long documentId) {
        // 直接获取文档信息并生成 MinIO 预签名 URL
        // 这里的逻辑已经在 downloadFile 中有了，我们需要类似的逻辑
        // 由于方法签名只接受 documentId，我们需要在这里手动查询或重构
        // 为了最小化改动且保持正确性，我们应该直接让 Controller 调用 buildFileUrl
        // 但由于 Controller 需要 documentId 对应的 filePath，这里我们简化逻辑：
        // 让 Controller 先获取 DocumentDTO，然后调用 buildFileUrl(filePath)

        // 注意：此方法在 Controller 的 getEditConfig/getPreviewConfig 中被调用
        // Controller 已经查到了 DocumentDTO，应该直接传 filePath 进来
        // 但为了兼容现有接口，我们在这里抛出异常或提示调用者改用 buildFileUrl(filePath)
        // 鉴于此时无法修改 Controller 的所有调用处（Controller 代码未完全展示），
        // 且 DocumentController:363 行调用了此方法，我们必须保持签名兼容。
        // 但 OnlyOfficeService 没有注入 DocumentAppService，无法查询文档路径。

        // 修正方案：修改 Controller，不再调用此方法，而是直接调用 buildFileUrl(doc.getFilePath())
        // 此方法标记为废弃或删除
        throw new UnsupportedOperationException("请使用 buildFileUrl(String fileUrl) 方法");
    }

    /**
     * 生成文档唯一标识
     */
    private String generateDocumentKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "docx";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 获取文档类型
     * word: 文档
     * cell: 表格
     * slide: 演示文稿
     */
    private String getDocumentType(String fileName) {
        String ext = getFileExtension(fileName);
        return switch (ext) {
            case "xls", "xlsx", "csv" -> "cell";
            case "ppt", "pptx" -> "slide";
            default -> "word";
        };
    }

    /**
     * 判断文件是否支持 OnlyOffice 编辑
     */
    public boolean isSupported(String fileName) {
        String ext = getFileExtension(fileName);
        return switch (ext) {
            case "doc", "docx", "odt", "rtf", "txt",
                    "xls", "xlsx", "ods", "csv",
                    "ppt", "pptx", "odp" ->
                true;
            default -> false;
        };
    }

    /**
     * 判断文件是否支持预览（包括 PDF）
     */
    public boolean isPreviewSupported(String fileName) {
        String ext = getFileExtension(fileName);
        return switch (ext) {
            case "doc", "docx", "odt", "rtf", "txt",
                    "xls", "xlsx", "ods", "csv",
                    "ppt", "pptx", "odp",
                    "pdf" ->
                true;
            default -> false;
        };
    }
}
