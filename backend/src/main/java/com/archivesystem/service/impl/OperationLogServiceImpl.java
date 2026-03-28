package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.repository.OperationLogMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.OperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    @Async
    public void log(OperationLog operationLog) {
        if (operationLog.getOperatedAt() == null) {
            operationLog.setOperatedAt(LocalDateTime.now());
        }
        
        // 补充操作人信息
        if (operationLog.getOperatorId() == null) {
            try {
                operationLog.setOperatorId(SecurityUtils.getCurrentUserId());
                operationLog.setOperatorName(SecurityUtils.getCurrentRealName());
            } catch (Exception e) {
                log.debug("获取操作人信息失败（可能是非登录场景）: {}", e.getMessage());
            }
        }
        
        // 补充IP和UA
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                operationLog.setOperatorIp(getClientIp(request));
                operationLog.setOperatorUa(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("获取请求信息失败（可能是非 HTTP 场景）: {}", e.getMessage());
        }
        
        operationLogMapper.insert(operationLog);
    }

    @Override
    public void log(String objectType, String objectId, Long archiveId, String operationType, String desc) {
        log(objectType, objectId, archiveId, operationType, desc, null);
    }

    @Override
    public void log(String objectType, String objectId, Long archiveId, String operationType, String desc, Map<String, Object> detail) {
        OperationLog operationLog = OperationLog.builder()
                .objectType(objectType)
                .objectId(objectId)
                .archiveId(archiveId)
                .operationType(operationType)
                .operationDesc(desc)
                .operationDetail(detail)
                .build();
        log(operationLog);
    }

    @Override
    public PageResult<OperationLog> query(String keyword, String objectType, String operationType,
            Long operatorId, LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(OperationLog::getOperationDesc, keyword)
                    .or().like(OperationLog::getOperatorName, keyword)
                    .or().like(OperationLog::getObjectId, keyword)
            );
        }
        
        if (StringUtils.hasText(objectType)) {
            wrapper.eq(OperationLog::getObjectType, objectType);
        }
        
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        
        if (operatorId != null) {
            wrapper.eq(OperationLog::getOperatorId, operatorId);
        }
        
        if (startDate != null) {
            wrapper.ge(OperationLog::getOperatedAt, startDate.atStartOfDay());
        }
        
        if (endDate != null) {
            wrapper.lt(OperationLog::getOperatedAt, endDate.plusDays(1).atStartOfDay());
        }
        
        wrapper.orderByDesc(OperationLog::getOperatedAt);
        
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        Page<OperationLog> result = operationLogMapper.selectPage(page, wrapper);
        
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public List<OperationLog> getByArchiveId(Long archiveId) {
        return operationLogMapper.selectByArchiveId(archiveId);
    }

    @Override
    public List<OperationLog> getByObject(String objectType, String objectId) {
        return operationLogMapper.selectByObject(objectType, objectId);
    }

    @Override
    public Map<String, Long> getOperationStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Long> stats = new HashMap<>();
        
        LambdaQueryWrapper<OperationLog> baseWrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            baseWrapper.ge(OperationLog::getOperatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            baseWrapper.lt(OperationLog::getOperatedAt, endDate.plusDays(1).atStartOfDay());
        }
        
        // 统计各操作类型
        for (String op : List.of(OperationLog.OP_CREATE, OperationLog.OP_UPDATE, OperationLog.OP_DELETE,
                OperationLog.OP_VIEW, OperationLog.OP_DOWNLOAD, OperationLog.OP_PRINT, OperationLog.OP_EXPORT)) {
            LambdaQueryWrapper<OperationLog> wrapper = baseWrapper.clone();
            wrapper.eq(OperationLog::getOperationType, op);
            stats.put(op, operationLogMapper.selectCount(wrapper));
        }
        
        return stats;
    }

    @Override
    public byte[] exportLogs(String objectType, String operationType, Long operatorId, LocalDate startDate, LocalDate endDate) {
        // 查询数据
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(objectType)) {
            wrapper.eq(OperationLog::getObjectType, objectType);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        if (operatorId != null) {
            wrapper.eq(OperationLog::getOperatorId, operatorId);
        }
        if (startDate != null) {
            wrapper.ge(OperationLog::getOperatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(OperationLog::getOperatedAt, endDate.plusDays(1).atStartOfDay());
        }
        
        wrapper.orderByDesc(OperationLog::getOperatedAt);
        wrapper.last("LIMIT 10000");
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        // 生成CSV
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            // BOM for Excel
            writer.print('\ufeff');
            // Header
            writer.println("ID,对象类型,对象ID,操作类型,操作描述,操作人,操作人ID,操作IP,操作时间");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (OperationLog logItem : logs) {
                writer.printf("%d,%s,%s,%s,\"%s\",%s,%d,%s,%s%n",
                        logItem.getId(),
                        logItem.getObjectType() != null ? logItem.getObjectType() : "",
                        logItem.getObjectId() != null ? logItem.getObjectId() : "",
                        logItem.getOperationType() != null ? logItem.getOperationType() : "",
                        logItem.getOperationDesc() != null ? logItem.getOperationDesc().replace("\"", "\"\"") : "",
                        logItem.getOperatorName() != null ? logItem.getOperatorName() : "",
                        logItem.getOperatorId() != null ? logItem.getOperatorId() : 0,
                        logItem.getOperatorIp() != null ? logItem.getOperatorIp() : "",
                        logItem.getOperatedAt() != null ? logItem.getOperatedAt().format(formatter) : ""
                );
            }
            writer.flush();
        }
        return baos.toByteArray();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
