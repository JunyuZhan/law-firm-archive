package com.archivesystem.service;

import com.archivesystem.dto.file.FilePreviewInfo;
import com.archivesystem.entity.DigitalFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 文件存储服务接口.
 * @author junyuzhan
 */
public interface FileStorageService {

    /**
     * 上传文件.
     */
    DigitalFile upload(MultipartFile file, Long archiveId, String fileCategory);

    /**
     * 上传文件并附带扫描留痕元数据.
     */
    default DigitalFile upload(MultipartFile file, Long archiveId, String fileCategory,
                               Integer volumeNo, String sectionType, String documentNo,
                               Integer pageStart, Integer pageEnd, String versionLabel,
                               String fileSourceType, String scanBatchNo, String scanOperator,
                               LocalDateTime scanTime, String scanCheckStatus,
                               String scanCheckBy, LocalDateTime scanCheckTime) {
        return upload(file, archiveId, fileCategory);
    }

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
     * 校验当前用户是否有权预览指定文件.
     */
    void assertPreviewAccess(Long fileId);

    /**
     * 获取文件预览信息（包含URL和预览类型）.
     */
    FilePreviewInfo getPreviewInfo(Long fileId);

    /**
     * 删除文件.
     */
    void delete(Long fileId);

    /**
     * 获取公开借阅场景下的短时预览链接.
     */
    String getBorrowPreviewUrl(Long archiveId, Long fileId, int expirySeconds);

    /**
     * 获取公开借阅场景下的短时下载链接.
     */
    String getBorrowDownloadUrl(Long archiveId, Long fileId, int expirySeconds);
}
