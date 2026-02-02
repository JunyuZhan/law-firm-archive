package com.lawfirm.application.clientservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.clientservice.dto.ClientDownloadLogDTO;
import com.lawfirm.application.clientservice.dto.DownloadLogCallbackRequest;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.clientservice.entity.ClientDownloadLog;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.infrastructure.persistence.mapper.ClientDownloadLogMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
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

    /**
     * 查询下载日志列表
     *
     * @param matterId 项目ID
     * @param clientId 客户ID（可选）
     * @param fileId 文件ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    public PageResult<ClientDownloadLogDTO> getDownloadLogs(
            final Long matterId,
            final Long clientId,
            final String fileId,
            final LocalDateTime startTime,
            final LocalDateTime endTime,
            final int pageNum,
            final int pageSize) {

        LambdaQueryWrapper<ClientDownloadLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClientDownloadLog::getMatterId, matterId)
                .eq(ClientDownloadLog::getDeleted, false);

        if (clientId != null) {
            queryWrapper.eq(ClientDownloadLog::getClientId, clientId);
        }

        if (fileId != null && !fileId.isEmpty()) {
            queryWrapper.eq(ClientDownloadLog::getFileId, fileId);
        }

        if (startTime != null) {
            queryWrapper.ge(ClientDownloadLog::getDownloadTime, startTime);
        }

        if (endTime != null) {
            queryWrapper.le(ClientDownloadLog::getDownloadTime, endTime);
        }

        queryWrapper.orderByDesc(ClientDownloadLog::getDownloadTime);

        IPage<ClientDownloadLog> page = clientDownloadLogMapper.selectPage(
                new Page<>(pageNum, pageSize), queryWrapper);

        List<ClientDownloadLogDTO> dtoList = page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 转换为DTO
     */
    private ClientDownloadLogDTO convertToDTO(final ClientDownloadLog log) {
        ClientDownloadLogDTO dto = new ClientDownloadLogDTO();
        dto.setId(log.getId());
        dto.setMatterId(log.getMatterId());
        dto.setClientId(log.getClientId());
        dto.setFileId(log.getFileId());
        dto.setFileName(log.getFileName());
        dto.setDownloadTime(log.getDownloadTime());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setEventType(log.getEventType());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
