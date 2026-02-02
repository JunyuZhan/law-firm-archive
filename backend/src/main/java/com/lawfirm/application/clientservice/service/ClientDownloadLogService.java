package com.lawfirm.application.clientservice.service;

import com.lawfirm.application.clientservice.dto.DownloadLogCallbackRequest;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.clientservice.entity.ClientDownloadLog;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.infrastructure.persistence.mapper.ClientDownloadLogMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户下载日志服务
 * 处理客户服务系统回调的下载日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientDownloadLogService {

    private final MatterMapper matterMapper;
    private final ClientDownloadLogMapper clientDownloadLogMapper;

    /**
     * 保存下载日志
     *
     * @param request 下载日志回调请求
     */
    @Transactional
    public void saveDownloadLog(final DownloadLogCallbackRequest request) {
        // 1. 验证项目存在
        Matter matter = matterMapper.selectById(request.getMatterId());
        if (matter == null || matter.getDeleted()) {
            log.warn("下载日志回调：项目不存在，跳过保存: matterId={}", request.getMatterId());
            throw new BusinessException("项目不存在");
        }

        // 2. 验证事件类型
        if (!ClientDownloadLog.EVENT_TYPE_DOWNLOAD.equals(request.getEventType())) {
            log.warn("下载日志回调：事件类型不正确: eventType={}", request.getEventType());
            throw new BusinessException("事件类型不正确");
        }

        // 3. 保存下载日志到数据库
        ClientDownloadLog downloadLog = ClientDownloadLog.builder()
                .matterId(request.getMatterId())
                .clientId(request.getClientId())
                .fileId(request.getFileId())
                .fileName(request.getFileName())
                .downloadTime(request.getDownloadTime())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .eventType(request.getEventType())
                .build();

        clientDownloadLogMapper.insert(downloadLog);

        log.info("保存客户下载日志成功: id={}, matterId={}, clientId={}, fileId={}, fileName={}, downloadTime={}",
                downloadLog.getId(), request.getMatterId(), request.getClientId(),
                request.getFileId(), request.getFileName(), request.getDownloadTime());
    }
}
