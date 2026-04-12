package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.entity.BackupTarget;
import com.archivesystem.security.SecretCryptoService;
import com.archivesystem.service.SmbStorageService;
import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * SMB/NAS 存储访问实现。
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmbStorageServiceImpl implements SmbStorageService {

    private final SecretCryptoService secretCryptoService;

    @Override
    public void verifyWritable(BackupTarget target) throws Exception {
        try (SmbFile root = resolveRoot(target)) {
            ensureDirectory(root);
            String probeName = ".probe-" + UUID.randomUUID() + ".tmp";
            try (SmbFile probe = new SmbFile(root, probeName);
                 OutputStream outputStream = new SmbFileOutputStream(probe)) {
                outputStream.write(("probe:" + probeName).getBytes(StandardCharsets.UTF_8));
            }
            try (SmbFile probe = new SmbFile(root, probeName)) {
                if (!probe.exists()) {
                    throw new BusinessException("SMB 探针文件写入失败");
                }
                probe.delete();
            }
        }
    }

    @Override
    public List<RemoteDirectoryEntry> listDirectories(BackupTarget target) throws Exception {
        try (SmbFile root = resolveRoot(target)) {
            ensureDirectory(root);
            SmbFile[] children = root.listFiles();
            List<RemoteDirectoryEntry> entries = new ArrayList<>();
            for (SmbFile child : children) {
                try (child) {
                    if (!child.isDirectory()) {
                        continue;
                    }
                    String name = trimTrailingSlash(child.getName());
                    if (!StringUtils.hasText(name)) {
                        continue;
                    }
                    entries.add(new RemoteDirectoryEntry(name, toLocalDateTime(child.lastModified())));
                }
            }
            return entries;
        }
    }

    @Override
    public boolean exists(BackupTarget target, String relativePath) throws Exception {
        try (SmbFile smbFile = resolveFile(target, relativePath, false)) {
            return smbFile.exists();
        }
    }

    @Override
    public InputStream openInputStream(BackupTarget target, String relativePath) throws Exception {
        SmbFile smbFile = resolveFile(target, relativePath, false);
        try {
            return new SmbFileInputStream(smbFile) {
                @Override
                public void close() throws java.io.IOException {
                    try {
                        super.close();
                    } finally {
                        smbFile.close();
                    }
                }
            };
        } catch (Exception e) {
            smbFile.close();
            throw e;
        }
    }

    @Override
    public void uploadDirectory(BackupTarget target, String remoteDirectoryName, Path localDirectory) throws Exception {
        try (SmbFile remoteRoot = resolveFile(target, remoteDirectoryName, true)) {
            ensureDirectory(remoteRoot);
            uploadRecursively(localDirectory, remoteRoot);
        }
    }

    @Override
    public Path downloadDirectoryToTemp(BackupTarget target, String remoteDirectoryName) throws Exception {
        Path tempRoot = Files.createTempDirectory("archive-smb-restore-");
        Path localDirectory = tempRoot.resolve(remoteDirectoryName);
        try (SmbFile remoteRoot = resolveFile(target, remoteDirectoryName, true)) {
            if (!remoteRoot.exists() || !remoteRoot.isDirectory()) {
                throw new BusinessException("SMB 备份集不存在: " + remoteDirectoryName);
            }
            downloadRecursively(remoteRoot, localDirectory);
        } catch (Exception e) {
            deleteLocalQuietly(tempRoot);
            throw e;
        }
        return localDirectory;
    }

    @Override
    public void deleteDirectory(BackupTarget target, String remoteDirectoryName) throws Exception {
        try (SmbFile remoteRoot = resolveFile(target, remoteDirectoryName, true)) {
            if (!remoteRoot.exists()) {
                return;
            }
            deleteRecursively(remoteRoot);
        }
    }

    @Override
    public String buildDisplayPath(BackupTarget target, String backupSetName) {
        StringBuilder builder = new StringBuilder("smb://")
                .append(target.getSmbHost());
        if (target.getSmbPort() != null && target.getSmbPort() != 445) {
            builder.append(':').append(target.getSmbPort());
        }
        builder.append('/')
                .append(trimSlashes(target.getSmbShare()));
        if (StringUtils.hasText(target.getSmbSubPath())) {
            builder.append('/').append(trimSlashes(target.getSmbSubPath()));
        }
        if (StringUtils.hasText(backupSetName)) {
            builder.append('/').append(trimSlashes(backupSetName));
        }
        return builder.toString();
    }

    private void uploadRecursively(Path localDirectory, SmbFile remoteDirectory) throws Exception {
        Files.createDirectories(localDirectory);
        try (var stream = Files.list(localDirectory)) {
            for (Path item : stream.toList()) {
                if (Files.isDirectory(item)) {
                    try (SmbFile childDirectory = new SmbFile(remoteDirectory, item.getFileName().toString() + "/")) {
                        ensureDirectory(childDirectory);
                        uploadRecursively(item, childDirectory);
                    }
                    continue;
                }
                try (SmbFile remoteFile = new SmbFile(remoteDirectory, item.getFileName().toString());
                     InputStream inputStream = Files.newInputStream(item);
                     OutputStream outputStream = new SmbFileOutputStream(remoteFile)) {
                    inputStream.transferTo(outputStream);
                }
            }
        }
    }

    private void downloadRecursively(SmbFile remoteDirectory, Path localDirectory) throws Exception {
        Files.createDirectories(localDirectory);
        for (SmbFile child : remoteDirectory.listFiles()) {
            try (child) {
                String childName = trimTrailingSlash(child.getName());
                if (!StringUtils.hasText(childName)) {
                    continue;
                }
                Path localTarget = localDirectory.resolve(childName);
                if (child.isDirectory()) {
                    downloadRecursively(child, localTarget);
                    continue;
                }
                Files.createDirectories(localTarget.getParent());
                try (InputStream inputStream = new SmbFileInputStream(child);
                     OutputStream outputStream = Files.newOutputStream(localTarget)) {
                    inputStream.transferTo(outputStream);
                }
            }
        }
    }

    private void deleteRecursively(SmbFile directory) throws Exception {
        if (directory.isDirectory()) {
            for (SmbFile child : directory.listFiles()) {
                try (child) {
                    deleteRecursively(child);
                }
            }
        }
        directory.delete();
    }

    private SmbFile resolveRoot(BackupTarget target) throws Exception {
        return new SmbFile(buildDisplayPath(target, null) + "/", buildContext(target));
    }

    private SmbFile resolveFile(BackupTarget target, String relativePath, boolean directory) throws Exception {
        String normalized = normalizeRelativePath(relativePath);
        if (!StringUtils.hasText(normalized)) {
            return resolveRoot(target);
        }
        String suffix = directory && !normalized.endsWith("/") ? "/" : "";
        return new SmbFile(buildDisplayPath(target, normalized) + suffix, buildContext(target));
    }

    private CIFSContext buildContext(BackupTarget target) throws Exception {
        if (!StringUtils.hasText(target.getSmbUsername()) || !StringUtils.hasText(target.getSmbPasswordEncrypted())) {
            throw new BusinessException("SMB 凭证未完整配置");
        }
        Properties properties = new Properties();
        properties.setProperty("jcifs.smb.client.connTimeout", "5000");
        properties.setProperty("jcifs.smb.client.responseTimeout", "5000");
        properties.setProperty("jcifs.smb.client.soTimeout", "5000");
        String password = secretCryptoService.decrypt(target.getSmbPasswordEncrypted());
        return new BaseContext(new PropertyConfiguration(properties))
                .withCredentials(new NtlmPasswordAuthenticator(null, target.getSmbUsername(), password));
    }

    private void ensureDirectory(SmbFile smbFile) throws Exception {
        if (!smbFile.exists()) {
            smbFile.mkdirs();
        }
        if (!smbFile.isDirectory()) {
            throw new BusinessException("SMB 路径不是目录: " + smbFile.getPath());
        }
    }

    private LocalDateTime toLocalDateTime(long lastModified) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneId.systemDefault());
    }

    private String normalizeRelativePath(String relativePath) {
        String normalized = trimSlashes(relativePath);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.replace('\\', '/');
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String trimSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        int start = 0;
        int end = value.length();
        while (start < end && (value.charAt(start) == '/' || value.charAt(start) == '\\')) {
            start++;
        }
        while (end > start && (value.charAt(end - 1) == '/' || value.charAt(end - 1) == '\\')) {
            end--;
        }
        return value.substring(start, end);
    }

    private void deleteLocalQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted((left, right) -> right.compareTo(left)).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (Exception e) {
                    log.warn("清理 SMB 临时目录失败: {}", item, e);
                }
            });
        } catch (Exception e) {
            log.warn("清理 SMB 临时目录失败: {}", path, e);
        }
    }
}
