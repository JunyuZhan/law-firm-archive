package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.OperationLogDTO;
import com.lawfirm.application.system.dto.OperationLogQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.system.entity.OperationLog;
import com.lawfirm.domain.system.repository.OperationLogRepository;
import com.lawfirm.infrastructure.persistence.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogAppService {

    private final OperationLogRepository operationLogRepository;
    private final OperationLogMapper operationLogMapper;

    /**
     * 分页查询操作日志
     */
    public PageResult<OperationLogDTO> listLogs(OperationLogQueryDTO query) {
        IPage<OperationLog> page = operationLogMapper.selectLogPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getUserId(),
                query.getModule(),
                query.getStatus(),
                query.getStartTime(),
                query.getEndTime()
        );

        List<OperationLogDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取日志详情
     */
    public OperationLogDTO getLogById(Long id) {
        OperationLog log = operationLogRepository.getByIdOrThrow(id, "日志不存在");
        return toDTO(log);
    }

    /**
     * 异步保存操作日志
     */
    @Async
    public void saveLogAsync(OperationLog operationLog) {
        try {
            operationLogRepository.save(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 清理历史日志
     */
    public void cleanOldLogs(int keepDays) {
        // TODO: 实现日志清理逻辑
        log.info("清理{}天前的操作日志", keepDays);
    }

    private OperationLogDTO toDTO(OperationLog log) {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        dto.setUserName(log.getUserName());
        dto.setModule(log.getModule());
        dto.setOperationType(log.getOperationType());
        dto.setDescription(log.getDescription());
        dto.setMethod(log.getMethod());
        dto.setRequestUrl(log.getRequestUrl());
        dto.setRequestMethod(log.getRequestMethod());
        dto.setRequestParams(log.getRequestParams());
        dto.setIpAddress(log.getIpAddress());
        dto.setExecutionTime(log.getExecutionTime());
        dto.setStatus(log.getStatus());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
