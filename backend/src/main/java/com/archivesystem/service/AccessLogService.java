package com.archivesystem.service;

/**
 * 访问日志服务接口.
 * @author junyuzhan
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

    /**
     * 记录搜索操作.
     * @param keyword 搜索关键词
     * @param resultCount 结果数量
     * @param duration 耗时（毫秒）
     * @param accessIp 访问IP
     */
    void logSearch(String keyword, int resultCount, long duration, String accessIp);
}
