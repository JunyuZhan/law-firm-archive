package com.archivesystem.service;

import com.archivesystem.entity.AccessLog;

/**
 * 访问日志服务接口.
 */
public interface AccessLogService {

    /**
     * 记录文件访问.
     */
    void logAccess(Long archiveId, Long fileId, String accessType, String accessIp);

    /**
     * 记录文件下载.
     */
    void logDownload(Long archiveId, Long fileId, String accessIp);

    /**
     * 记录文件预览.
     */
    void logPreview(Long archiveId, Long fileId, String accessIp);
}
