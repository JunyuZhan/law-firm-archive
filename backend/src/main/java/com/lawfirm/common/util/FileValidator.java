package com.lawfirm.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 文件验证工具类
 * 
 * 安全功能：防止恶意文件上传
 * - 验证MIME类型
 * - 验证文件签名(魔数)
 * - 检测可执行文件
 * - 防止文件类型伪装
 * 
 * @author system
 * @since 2026-01-10
 */
@Slf4j
public class FileValidator {

    /**
     * 允许的文档文件扩展名（白名单）
     */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
        // 文档
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf", "txt",
        // 图片
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp",
        // 压缩文件
        "zip", "rar", "7z", "tar", "gz",
        // 其他
        "csv", "xml", "json"
    ));

    /**
     * 禁止的可执行文件扩展名（黑名单）
     */
    private static final Set<String> FORBIDDEN_EXTENSIONS = new HashSet<>(Arrays.asList(
        "exe", "dll", "so", "dylib", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar",
        "app", "deb", "rpm", "msi", "dmg", "iso", "bin", "com", "scr", "pif",
        "php", "jsp", "asp", "aspx", "cgi", "pl", "py", "rb"
    ));

    /**
     * 文件签名（魔数）映射
     * Key: 扩展名, Value: 可能的魔数前缀
     */
    private static final Map<String, List<byte[]>> FILE_SIGNATURES = new HashMap<>();

    /**
     * 默认最大文件大小：20MB
     */
    private static final long DEFAULT_MAX_SIZE = 20 * 1024 * 1024;

    static {
        // PDF: %PDF
        FILE_SIGNATURES.put("pdf", Collections.singletonList(
            new byte[]{0x25, 0x50, 0x44, 0x46}
        ));

        // JPEG: FFD8FF
        FILE_SIGNATURES.put("jpg", Arrays.asList(
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0},
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1},
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE2},
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE8}
        ));
        FILE_SIGNATURES.put("jpeg", FILE_SIGNATURES.get("jpg"));

        // PNG: 89504E47
        FILE_SIGNATURES.put("png", Collections.singletonList(
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        ));

        // GIF: GIF87a 或 GIF89a
        FILE_SIGNATURES.put("gif", Arrays.asList(
            new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61},
            new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}
        ));

        // ZIP (包括docx, xlsx, pptx等Office文档)
        FILE_SIGNATURES.put("zip", Collections.singletonList(
            new byte[]{0x50, 0x4B, 0x03, 0x04}
        ));
        FILE_SIGNATURES.put("docx", FILE_SIGNATURES.get("zip"));
        FILE_SIGNATURES.put("xlsx", FILE_SIGNATURES.get("zip"));
        FILE_SIGNATURES.put("pptx", FILE_SIGNATURES.get("zip"));

        // DOC/XLS/PPT (MS Office 97-2003): D0CF11E0A1B11AE1
        FILE_SIGNATURES.put("doc", Collections.singletonList(
            new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1}
        ));
        FILE_SIGNATURES.put("xls", FILE_SIGNATURES.get("doc"));
        FILE_SIGNATURES.put("ppt", FILE_SIGNATURES.get("doc"));

        // RAR: Rar!
        FILE_SIGNATURES.put("rar", Collections.singletonList(
            new byte[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07}
        ));

        // 7Z: 7z
        FILE_SIGNATURES.put("7z", Collections.singletonList(
            new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C}
        ));

        // BMP: BM
        FILE_SIGNATURES.put("bmp", Collections.singletonList(
            new byte[]{0x42, 0x4D}
        ));

        // WebP: RIFF....WEBP
        FILE_SIGNATURES.put("webp", Collections.singletonList(
            new byte[]{0x52, 0x49, 0x46, 0x46}
        ));
    }

    private FileValidator() {
        // 工具类，禁止实例化
    }

    /**
     * 验证文件是否安全
     *
     * @param file 上传的文件
     * @return 验证结果
     */
    public static ValidationResult validate(MultipartFile file) {
        return validate(file, DEFAULT_MAX_SIZE);
    }

    /**
     * 验证文件是否安全（自定义最大大小）
     *
     * @param file    上传的文件
     * @param maxSize 最大文件大小（字节）
     * @return 验证结果
     */
    public static ValidationResult validate(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return ValidationResult.error("文件名为空");
        }

        // 1. 验证文件大小
        if (file.getSize() > maxSize) {
            return ValidationResult.error("文件大小超过限制，最大支持" + (maxSize / 1024 / 1024) + "MB");
        }

        // 2. 获取文件扩展名
        String extension = getFileExtension(originalFilename);
        if (extension == null || extension.isEmpty()) {
            return ValidationResult.error("无法识别的文件类型");
        }

        extension = extension.toLowerCase();

        // 3. 检查是否在禁止列表中（黑名单）
        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            log.warn("检测到禁止的文件类型: {}, 文件名: {}", extension, originalFilename);
            return ValidationResult.error("禁止上传可执行文件: " + extension);
        }

        // 4. 检查是否在允许列表中（白名单）
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("检测到不允许的文件类型: {}, 文件名: {}", extension, originalFilename);
            return ValidationResult.error("不支持的文件类型: " + extension);
        }

        // 5. 验证MIME类型
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            return ValidationResult.error("无法识别的MIME类型");
        }

        if (!isMimeTypeAllowed(contentType)) {
            log.warn("检测到不允许的MIME类型: {}, 文件名: {}", contentType, originalFilename);
            return ValidationResult.error("不允许的MIME类型: " + contentType);
        }

        // 6. 验证文件签名（魔数），防止文件伪装
        try (InputStream inputStream = file.getInputStream()) {
            if (!validateFileSignature(inputStream, extension)) {
                log.warn("文件签名验证失败，可能是伪装文件: extension={}, filename={}", extension, originalFilename);
                return ValidationResult.error("文件内容与扩展名不匹配，可能是伪装文件");
            }
        } catch (IOException e) {
            log.error("读取文件内容失败", e);
            return ValidationResult.error("无法读取文件内容");
        }

        return ValidationResult.success();
    }

    /**
     * 快速检查文件扩展名是否允许（不检查文件内容）
     *
     * @param filename 文件名
     * @return true-允许，false-不允许
     */
    public static boolean isExtensionAllowed(String filename) {
        String extension = getFileExtension(filename);
        if (extension == null) {
            return false;
        }
        extension = extension.toLowerCase();
        return !FORBIDDEN_EXTENSIONS.contains(extension) && ALLOWED_EXTENSIONS.contains(extension);
    }

    /**
     * 检查文件扩展名是否被禁止
     *
     * @param filename 文件名
     * @return true-禁止，false-允许
     */
    public static boolean isExtensionForbidden(String filename) {
        String extension = getFileExtension(filename);
        if (extension == null) {
            return false;
        }
        return FORBIDDEN_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return null;
    }

    /**
     * 验证MIME类型是否允许
     */
    private static boolean isMimeTypeAllowed(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        // 允许的MIME类型前缀
        String[] allowedPrefixes = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "application/vnd.oasis.opendocument",
            "text/plain",
            "text/csv",
            "text/xml",
            "application/json",
            "application/xml",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/svg+xml",
            "image/webp",
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            "application/gzip",
            "application/x-tar",
            "application/octet-stream" // 通用二进制，需要结合其他验证
        };

        for (String prefix : allowedPrefixes) {
            if (mimeType.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 验证文件签名（魔数）
     */
    private static boolean validateFileSignature(InputStream inputStream, String extension) throws IOException {
        List<byte[]> signatures = FILE_SIGNATURES.get(extension);
        if (signatures == null || signatures.isEmpty()) {
            // 如果没有配置签名，跳过验证（但已经通过扩展名和MIME类型验证）
            return true;
        }

        // 读取文件头部字节
        byte[] header = new byte[16];
        int bytesRead = inputStream.read(header);

        if (bytesRead < 4) {
            // 文件太小，无法验证
            return false;
        }

        // 检查是否匹配任一签名
        for (byte[] signature : signatures) {
            if (matchesSignature(header, signature)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查文件头是否匹配签名
     */
    private static boolean matchesSignature(byte[] header, byte[] signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return valid ? "ValidationResult{valid=true}" 
                        : "ValidationResult{valid=false, error='" + errorMessage + "'}";
        }
    }
}

