package com.lawfirm.application.clientservice.service;

import com.lawfirm.application.clientservice.dto.AccessLogCallbackRequest;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.clientservice.entity.ClientAccessLog;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.infrastructure.persistence.mapper.ClientAccessLogMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户访问日志服务
 * 处理客户服务系统回调的访问日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAccessLogService {

    private final MatterMapper matterMapper;
    private final ClientAccessLogMapper clientAccessLogMapper;

    /**
     * 保存访问日志
     *
     * @param request 访问日志回调请求
     */
    @Transactional
    public void saveAccessLog(final AccessLogCallbackRequest request) {
        // 1. 验证项目存在
        Matter matter = matterMapper.selectById(request.getMatterId());
        if (matter == null || matter.getDeleted()) {
            log.warn("访问日志回调：项目不存在，跳过保存: matterId={}", request.getMatterId());
            throw new BusinessException("项目不存在");
        }

        // 2. 验证事件类型
        if (!ClientAccessLog.EVENT_TYPE_ACCESS.equals(request.getEventType())) {
            log.warn("访问日志回调：事件类型不正确: eventType={}", request.getEventType());
            throw new BusinessException("事件类型不正确");
        }

        // 3. 保存访问日志到数据库
        ClientAccessLog accessLog = ClientAccessLog.builder()
                .matterId(request.getMatterId())
                .clientId(request.getClientId())
                .accessTime(request.getAccessTime())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .eventType(request.getEventType())
                .build();

        clientAccessLogMapper.insert(accessLog);

        log.info("保存客户访问日志成功: id={}, matterId={}, clientId={}, accessTime={}",
                accessLog.getId(), request.getMatterId(), request.getClientId(), request.getAccessTime());
    }
}
