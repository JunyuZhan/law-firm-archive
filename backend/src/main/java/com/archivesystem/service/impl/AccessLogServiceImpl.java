package com.archivesystem.service.impl;

import com.archivesystem.entity.AccessLog;
import com.archivesystem.repository.AccessLogMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.AccessLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 访问日志服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogServiceImpl implements AccessLogService {

    private final AccessLogMapper accessLogMapper;

    @Override
    @Async
    public void logAccess(Long archiveId, Long fileId, String accessType, String accessIp) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            String userName = SecurityUtils.getCurrentRealName();
            
            AccessLog accessLog = AccessLog.builder()
                    .archiveId(archiveId)
                    .fileId(fileId)
                    .accessType(accessType)
                    .accessIp(accessIp)
                    .userId(userId)
                    .userName(userName)
                    .accessedAt(LocalDateTime.now())
                    .build();
            
            accessLogMapper.insert(accessLog);
            log.debug("记录访问日志: archiveId={}, fileId={}, type={}", archiveId, fileId, accessType);
        } catch (Exception e) {
            log.error("记录访问日志失败", e);
        }
    }

    @Override
    public void logDownload(Long archiveId, Long fileId, String accessIp) {
        logAccess(archiveId, fileId, AccessLog.TYPE_DOWNLOAD, accessIp);
    }

    @Override
    public void logPreview(Long archiveId, Long fileId, String accessIp) {
        logAccess(archiveId, fileId, AccessLog.TYPE_PREVIEW, accessIp);
    }

    @Override
    @Async
    public void logSearch(String keyword, int resultCount, long duration, String accessIp) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            String userName = SecurityUtils.getCurrentRealName();
            
            AccessLog accessLog = AccessLog.builder()
                    .accessType(AccessLog.TYPE_SEARCH)
                    .accessIp(accessIp)
                    .userId(userId)
                    .userName(userName)
                    .searchKeyword(keyword)
                    .searchResultCount(resultCount)
                    .duration(duration)
                    .accessedAt(LocalDateTime.now())
                    .build();
            
            accessLogMapper.insert(accessLog);
            log.debug("记录搜索日志: keyword={}, resultCount={}, duration={}ms", keyword, resultCount, duration);
        } catch (Exception e) {
            log.error("记录搜索日志失败", e);
        }
    }
}
