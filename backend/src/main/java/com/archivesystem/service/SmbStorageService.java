package com.archivesystem.service;

import com.archivesystem.entity.BackupTarget;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SMB 备份目标访问服务。
 * @author junyuzhan
 */
public interface SmbStorageService {

    void verifyWritable(BackupTarget target) throws Exception;

    List<RemoteDirectoryEntry> listDirectories(BackupTarget target) throws Exception;

    boolean exists(BackupTarget target, String relativePath) throws Exception;

    InputStream openInputStream(BackupTarget target, String relativePath) throws Exception;

    void uploadDirectory(BackupTarget target, String remoteDirectoryName, Path localDirectory) throws Exception;

    Path downloadDirectoryToTemp(BackupTarget target, String remoteDirectoryName) throws Exception;

    void deleteDirectory(BackupTarget target, String remoteDirectoryName) throws Exception;

    String buildDisplayPath(BackupTarget target, String backupSetName);

    record RemoteDirectoryEntry(String name, LocalDateTime modifiedAt) {
    }
}
