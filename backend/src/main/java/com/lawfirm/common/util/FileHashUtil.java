package com.lawfirm.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class FileHashUtil {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    public static String calculateHash(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try (InputStream inputStream = file.getInputStream()) {
            return calculateHash(inputStream);
        } catch (Exception e) {
            log.error("计算文件Hash失败: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    public static String calculateHash(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash算法不存在: {}", HASH_ALGORITHM, e);
            return null;
        } catch (Exception e) {
            log.error("计算Hash失败", e);
            return null;
        }
    }

    public static String calculateHash(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(bytes);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash算法不存在: {}", HASH_ALGORITHM, e);
            return null;
        }
    }

    public static boolean verifyHash(MultipartFile file, String expectedHash) {
        if (file == null || expectedHash == null) {
            return false;
        }
        String actualHash = calculateHash(file);
        return expectedHash.equalsIgnoreCase(actualHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
