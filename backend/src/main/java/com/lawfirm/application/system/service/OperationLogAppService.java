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
        if (keepDays < 0) {
            throw new com.lawfirm.common.exception.BusinessException("保留天数不能为负数");
        }
        
        // 计算截止日期：当前时间减去保留天数
        java.time.LocalDateTime beforeDate = java.time.LocalDateTime.now().minusDays(keepDays);
        
        // 统计要删除的日志数量
        long count = operationLogMapper.countLogsBeforeDate(beforeDate);
        
        if (count == 0) {
            log.info("没有需要清理的操作日志（保留{}天）", keepDays);
            return;
        }
        
        // 执行软删除
        int deletedCount = operationLogMapper.deleteLogsBeforeDate(beforeDate);
        
        log.info("清理操作日志完成：删除{}条记录（保留{}天，删除{}天前的记录）", deletedCount, keepDays, keepDays);
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
        
        // 为了兼容前端字段名，添加额外的字段映射
        // 前端期望的字段：action, operatorName, operatorIp, duration, operationTime
        // 这些字段可以通过JSON序列化时的@JsonProperty或自定义序列化器来处理
        // 但为了简单，我们在DTO中添加这些字段的getter方法
        return dto;
    }
}
