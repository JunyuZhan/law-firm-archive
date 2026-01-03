package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.DocumentAuditQueryCommand;
import com.lawfirm.application.document.dto.DocumentAuditStatisticsDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.DocAccessLog;
import com.lawfirm.infrastructure.persistence.mapper.DocAccessLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档访问日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocAccessLogService {

    private final DocAccessLogMapper logMapper;

    /**
     * 记录访问日志
     */
    public void logAccess(Long documentId, String actionType, HttpServletRequest request) {
        DocAccessLog accessLog = DocAccessLog.builder()
                .documentId(documentId)
                .userId(SecurityUtils.getUserId())
                .actionType(actionType)
                .ipAddress(getClientIp(request))
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .createdAt(LocalDateTime.now())
                .build();
        logMapper.insert(accessLog);
    }

    /**
     * 查询文档访问日志
     */
    public PageResult<DocAccessLog> getAccessLogs(Long documentId, Long userId, 
                                                   String actionType, int pageNum, int pageSize) {
        IPage<DocAccessLog> page = logMapper.selectLogPage(
                new Page<>(pageNum, pageSize), documentId, userId, actionType);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取文档访问次数
     */
    public int getAccessCount(Long documentId) {
        return logMapper.countByDocumentId(documentId);
    }

    /**
     * 获取文档审计统计（M5-044）
     */
    public DocumentAuditStatisticsDTO getAuditStatistics(DocumentAuditQueryCommand command) {
        DocumentAuditStatisticsDTO statistics = new DocumentAuditStatisticsDTO();

        // 按用户统计
        List<Map<String, Object>> byUser = logMapper.countByUser(
                command.getDocumentId(),
                command.getStartTime(),
                command.getEndTime()
        );
        statistics.setByUser(byUser);

        // 按文档统计
        List<Map<String, Object>> byDocument = logMapper.countByDocument(
                command.getUserId(),
                command.getStartTime(),
                command.getEndTime()
        );
        statistics.setByDocument(byDocument);

        // 按操作类型统计
        List<Map<String, Object>> byActionType = logMapper.countByActionType(
                command.getDocumentId(),
                command.getUserId(),
                command.getStartTime(),
                command.getEndTime()
        );
        statistics.setByActionType(byActionType);

        // 按时间统计（趋势）
        List<Map<String, Object>> byDate = logMapper.countByDate(
                command.getDocumentId(),
                command.getUserId(),
                command.getStartTime(),
                command.getEndTime()
        );
        statistics.setByDate(byDate);

        // 计算总访问次数
        long totalCount = byActionType.stream()
                .mapToLong(item -> ((Number) item.get("access_count")).longValue())
                .sum();
        statistics.setTotalAccessCount(totalCount);

        return statistics;
    }

    /**
     * 查询审计报告数据（M5-045）
     */
    public List<Map<String, Object>> queryAuditReport(DocumentAuditQueryCommand command) {
        return logMapper.queryAuditReport(
                command.getDocumentId(),
                command.getUserId(),
                command.getActionType(),
                command.getStartTime(),
                command.getEndTime()
        );
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
