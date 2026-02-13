package com.archivesystem.service;

import com.archivesystem.entity.DigitalFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口.
 */
public interface FileStorageService {

    /**
     * 上传文件.
     */
    DigitalFile upload(MultipartFile file, Long archiveId, String fileCategory);

    /**
     * 从URL下载文件并存储.
     */
    DigitalFile downloadAndStore(Long archiveId, String downloadUrl, String fileName, String fileCategory, int sortOrder);

    /**
     * 获取文件下载URL（预签名）.
     */
    String getDownloadUrl(Long fileId);

    /**
     * 获取文件预览URL.
     */
    String getPreviewUrl(Long fileId);

    /**
     * 删除文件.
     */
    void delete(Long fileId);
}
