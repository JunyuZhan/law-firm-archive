package com.archivesystem.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 文件类型验证工具类.
 * 通过文件魔数（Magic Number）验证文件真实类型，防止文件伪装攻击。
 */
@Slf4j
public class FileTypeValidator {

    // 文件魔数映射表（十六进制）
    private static final Map<String, byte[][]> MAGIC_NUMBERS = new HashMap<>();
    
    // 允许的文件扩展名与MIME类型映射
    private static final Map<String, Set<String>> EXTENSION_MIME_MAP = new HashMap<>();

    static {
        // PDF - 25 50 44 46 (%PDF)
        MAGIC_NUMBERS.put("pdf", new byte[][]{
            {0x25, 0x50, 0x44, 0x46}
        });
        
        // JPEG - FF D8 FF
        MAGIC_NUMBERS.put("jpg", new byte[][]{
            {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        });
        MAGIC_NUMBERS.put("jpeg", new byte[][]{
            {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        });
        
        // PNG - 89 50 4E 47 0D 0A 1A 0A
        MAGIC_NUMBERS.put("png", new byte[][]{
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        });
        
        // GIF - 47 49 46 38 (GIF8)
        MAGIC_NUMBERS.put("gif", new byte[][]{
            {0x47, 0x49, 0x46, 0x38}
        });
        
        // BMP - 42 4D (BM)
        MAGIC_NUMBERS.put("bmp", new byte[][]{
            {0x42, 0x4D}
        });
        
        // WebP - 52 49 46 46 ... 57 45 42 50 (RIFF...WEBP)
        MAGIC_NUMBERS.put("webp", new byte[][]{
            {0x52, 0x49, 0x46, 0x46}
        });
        
        // TIFF - 49 49 2A 00 (little endian) 或 4D 4D 00 2A (big endian)
        MAGIC_NUMBERS.put("tif", new byte[][]{
            {0x49, 0x49, 0x2A, 0x00},
            {0x4D, 0x4D, 0x00, 0x2A}
        });
        MAGIC_NUMBERS.put("tiff", new byte[][]{
            {0x49, 0x49, 0x2A, 0x00},
            {0x4D, 0x4D, 0x00, 0x2A}
        });
        
        // DOC (旧版Word) - D0 CF 11 E0 A1 B1 1A E1 (OLE)
        MAGIC_NUMBERS.put("doc", new byte[][]{
            {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1}
        });
        
        // DOCX/XLSX/PPTX (ZIP-based Office) - 50 4B 03 04 (PK..)
        MAGIC_NUMBERS.put("docx", new byte[][]{
            {0x50, 0x4B, 0x03, 0x04}
        });
        MAGIC_NUMBERS.put("xlsx", new byte[][]{
            {0x50, 0x4B, 0x03, 0x04}
        });
        MAGIC_NUMBERS.put("pptx", new byte[][]{
            {0x50, 0x4B, 0x03, 0x04}
        });
        
        // XLS (旧版Excel) - D0 CF 11 E0 (OLE)
        MAGIC_NUMBERS.put("xls", new byte[][]{
            {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1}
        });
        
        // PPT (旧版PowerPoint) - D0 CF 11 E0 (OLE)
        MAGIC_NUMBERS.put("ppt", new byte[][]{
            {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1}
        });
        
        // ZIP - 50 4B 03 04
        MAGIC_NUMBERS.put("zip", new byte[][]{
            {0x50, 0x4B, 0x03, 0x04},
            {0x50, 0x4B, 0x05, 0x06},
            {0x50, 0x4B, 0x07, 0x08}
        });
        
        // RAR - 52 61 72 21 1A 07 (Rar!..)
        MAGIC_NUMBERS.put("rar", new byte[][]{
            {0x52, 0x61, 0x72, 0x21, 0x1A, 0x07}
        });
        
        // 7Z - 37 7A BC AF 27 1C
        MAGIC_NUMBERS.put("7z", new byte[][]{
            {0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C}
        });
        
        // OFD (国产版式文档) - 基于ZIP格式
        MAGIC_NUMBERS.put("ofd", new byte[][]{
            {0x50, 0x4B, 0x03, 0x04}
        });
        
        // TXT/文本文件 - 不做魔数验证，仅依赖扩展名
        
        // MIME类型映射
        EXTENSION_MIME_MAP.put("pdf", Set.of("application/pdf"));
        EXTENSION_MIME_MAP.put("jpg", Set.of("image/jpeg"));
        EXTENSION_MIME_MAP.put("jpeg", Set.of("image/jpeg"));
        EXTENSION_MIME_MAP.put("png", Set.of("image/png"));
        EXTENSION_MIME_MAP.put("gif", Set.of("image/gif"));
        EXTENSION_MIME_MAP.put("bmp", Set.of("image/bmp"));
        EXTENSION_MIME_MAP.put("webp", Set.of("image/webp"));
        EXTENSION_MIME_MAP.put("tif", Set.of("image/tiff"));
        EXTENSION_MIME_MAP.put("tiff", Set.of("image/tiff"));
        EXTENSION_MIME_MAP.put("doc", Set.of("application/msword"));
        EXTENSION_MIME_MAP.put("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        EXTENSION_MIME_MAP.put("xls", Set.of("application/vnd.ms-excel"));
        EXTENSION_MIME_MAP.put("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        EXTENSION_MIME_MAP.put("ppt", Set.of("application/vnd.ms-powerpoint"));
        EXTENSION_MIME_MAP.put("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        EXTENSION_MIME_MAP.put("zip", Set.of("application/zip", "application/x-zip-compressed"));
        EXTENSION_MIME_MAP.put("rar", Set.of("application/x-rar-compressed", "application/vnd.rar"));
        EXTENSION_MIME_MAP.put("7z", Set.of("application/x-7z-compressed"));
        EXTENSION_MIME_MAP.put("txt", Set.of("text/plain"));
        EXTENSION_MIME_MAP.put("ofd", Set.of("application/ofd"));
    }

    /**
     * 验证文件类型是否匹配扩展名.
     *
     * @param file      上传的文件
     * @param extension 文件扩展名
     * @return 验证结果
     */
    public static ValidationResult validate(MultipartFile file, String extension) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.fail("文件为空");
        }
        
        if (extension == null || extension.isBlank()) {
            return ValidationResult.fail("文件扩展名为空");
        }
        
        String ext = extension.toLowerCase();
        
        // 文本文件进行二进制内容检查（防止伪装攻击）
        if ("txt".equals(ext) || "csv".equals(ext) || "xml".equals(ext) || "json".equals(ext)) {
            try {
                byte[] header = readFileHeader(file, 512);
                if (header != null && containsBinaryContent(header)) {
                    log.warn("文本文件包含二进制内容，可能是伪装文件: extension={}", ext);
                    return ValidationResult.fail("文件内容与文本格式不匹配，可能是伪装文件");
                }
            } catch (IOException e) {
                log.warn("无法验证文本文件: {}", e.getMessage());
            }
            return ValidationResult.success();
        }
        
        // 获取该扩展名对应的魔数
        byte[][] magicNumbers = MAGIC_NUMBERS.get(ext);
        if (magicNumbers == null) {
            // 没有配置魔数的类型，仅通过扩展名验证
            log.debug("未配置魔数验证: {}", ext);
            return ValidationResult.success();
        }
        
        try {
            // 读取文件头部
            byte[] header = readFileHeader(file, 16);
            if (header == null || header.length == 0) {
                return ValidationResult.fail("无法读取文件内容");
            }
            
            // 验证魔数
            boolean matched = false;
            for (byte[] magic : magicNumbers) {
                if (startsWith(header, magic)) {
                    matched = true;
                    break;
                }
            }
            
            if (!matched) {
                log.warn("文件魔数验证失败: extension={}, header={}", ext, bytesToHex(header));
                return ValidationResult.fail("文件类型与扩展名不匹配，可能是伪装文件");
            }
            
            return ValidationResult.success();
            
        } catch (IOException e) {
            log.error("文件验证异常", e);
            return ValidationResult.fail("文件验证异常: " + e.getMessage());
        }
    }

    /**
     * 验证字节数组内容类型是否匹配扩展名.
     *
     * @param content   文件字节内容
     * @param extension 文件扩展名
     * @return 验证结果
     */
    public static ValidationResult validate(byte[] content, String extension) {
        if (content == null || content.length == 0) {
            return ValidationResult.fail("文件内容为空");
        }
        
        if (extension == null || extension.isBlank()) {
            return ValidationResult.fail("文件扩展名为空");
        }
        
        String ext = extension.toLowerCase();
        
        // 文本文件进行二进制内容检查（防止伪装攻击）
        if ("txt".equals(ext) || "csv".equals(ext) || "xml".equals(ext) || "json".equals(ext)) {
            byte[] header = new byte[Math.min(content.length, 512)];
            System.arraycopy(content, 0, header, 0, header.length);
            if (containsBinaryContent(header)) {
                log.warn("文本文件包含二进制内容，可能是伪装文件: extension={}", ext);
                return ValidationResult.fail("文件内容与文本格式不匹配，可能是伪装文件");
            }
            return ValidationResult.success();
        }
        
        // 获取该扩展名对应的魔数
        byte[][] magicNumbers = MAGIC_NUMBERS.get(ext);
        if (magicNumbers == null) {
            // 没有配置魔数的类型，仅通过扩展名验证
            log.debug("未配置魔数验证: {}", ext);
            return ValidationResult.success();
        }
        
        // 读取文件头部（最多16字节）
        byte[] header = new byte[Math.min(content.length, 16)];
        System.arraycopy(content, 0, header, 0, header.length);
        
        // 验证魔数
        boolean matched = false;
        for (byte[] magic : magicNumbers) {
            if (startsWith(header, magic)) {
                matched = true;
                break;
            }
        }
        
        if (!matched) {
            log.warn("文件魔数验证失败: extension={}, header={}", ext, bytesToHex(header));
            return ValidationResult.fail("文件类型与扩展名不匹配，可能是伪装文件");
        }
        
        return ValidationResult.success();
    }

    /**
     * 验证MIME类型是否与扩展名匹配.
     */
    public static boolean validateMimeType(String extension, String mimeType) {
        if (extension == null || mimeType == null) {
            return false;
        }
        
        Set<String> allowedMimes = EXTENSION_MIME_MAP.get(extension.toLowerCase());
        if (allowedMimes == null) {
            // 未配置的类型，允许通过
            return true;
        }
        
        // 某些浏览器可能发送带参数的MIME类型
        String baseMime = mimeType.split(";")[0].trim().toLowerCase();
        
        // 通用Office类型也允许
        if ("application/octet-stream".equals(baseMime)) {
            return true;
        }
        
        return allowedMimes.contains(baseMime);
    }

    /**
     * 读取文件头部字节.
     */
    private static byte[] readFileHeader(MultipartFile file, int length) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[length];
            int read = is.read(header);
            if (read < length) {
                byte[] result = new byte[read];
                System.arraycopy(header, 0, result, 0, read);
                return result;
            }
            return header;
        }
    }

    /**
     * 检查内容是否包含二进制数据（非文本内容）.
     * 用于验证文本文件是否被伪装。
     */
    private static boolean containsBinaryContent(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        
        int binaryCount = 0;
        for (byte b : data) {
            // 允许的字符：可打印ASCII (0x20-0x7E)、换行(0x0A)、回车(0x0D)、制表符(0x09)
            // UTF-8 高位字节 (0x80-0xFF) 也允许
            if (b < 0x09 || (b > 0x0D && b < 0x20) || b == 0x7F) {
                binaryCount++;
            }
        }
        
        // 如果二进制字符超过 5%，认为是二进制文件
        double binaryRatio = (double) binaryCount / data.length;
        return binaryRatio > 0.05;
    }
    
    /**
     * 检查字节数组是否以指定前缀开头.
     */
    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 字节数组转十六进制字符串（用于日志）.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 16); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    /**
     * 验证结果.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
