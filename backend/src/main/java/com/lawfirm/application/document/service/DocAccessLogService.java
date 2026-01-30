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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 文档访问日志服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocAccessLogService {

  /** 文档访问日志Mapper. */
  private final DocAccessLogMapper logMapper;

  /**
   * 记录访问日志。
   *
   * @param documentId 文档ID
   * @param actionType 操作类型
   * @param request HTTP请求
   */
  public void logAccess(
      final Long documentId, final String actionType, final HttpServletRequest request) {
    DocAccessLog accessLog =
        DocAccessLog.builder()
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
   * 查询文档访问日志。
   *
   * @param documentId 文档ID
   * @param userId 用户ID
   * @param actionType 操作类型
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 访问日志分页结果
   */
  public PageResult<DocAccessLog> getAccessLogs(
      final Long documentId,
      final Long userId,
      final String actionType,
      final int pageNum,
      final int pageSize) {
    IPage<DocAccessLog> page =
        logMapper.selectLogPage(new Page<>(pageNum, pageSize), documentId, userId, actionType);
    return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
  }

  /**
   * 获取文档访问次数。
   *
   * @param documentId 文档ID
   * @return 访问次数
   */
  public int getAccessCount(final Long documentId) {
    return logMapper.countByDocumentId(documentId);
  }

  /**
   * 获取文档审计统计（M5-044）。
   *
   * @param command 查询命令
   * @return 审计统计DTO
   */
  public DocumentAuditStatisticsDTO getAuditStatistics(final DocumentAuditQueryCommand command) {
    DocumentAuditStatisticsDTO statistics = new DocumentAuditStatisticsDTO();

    // 按用户统计
    List<Map<String, Object>> byUser =
        logMapper.countByUser(
            command.getDocumentId(), command.getStartTime(), command.getEndTime());
    statistics.setByUser(byUser);

    // 按文档统计
    List<Map<String, Object>> byDocument =
        logMapper.countByDocument(
            command.getUserId(), command.getStartTime(), command.getEndTime());
    statistics.setByDocument(byDocument);

    // 按操作类型统计
    List<Map<String, Object>> byActionType =
        logMapper.countByActionType(
            command.getDocumentId(),
            command.getUserId(),
            command.getStartTime(),
            command.getEndTime());
    statistics.setByActionType(byActionType);

    // 按时间统计（趋势）
    List<Map<String, Object>> byDate =
        logMapper.countByDate(
            command.getDocumentId(),
            command.getUserId(),
            command.getStartTime(),
            command.getEndTime());
    statistics.setByDate(byDate);

    // 计算总访问次数
    long totalCount =
        byActionType.stream()
            .mapToLong(item -> ((Number) item.get("access_count")).longValue())
            .sum();
    statistics.setTotalAccessCount(totalCount);

    return statistics;
  }

  /**
   * 查询审计报告数据（M5-045）。
   *
   * @param command 查询命令
   * @return 审计报告数据
   */
  public List<Map<String, Object>> queryAuditReport(final DocumentAuditQueryCommand command) {
    return logMapper.queryAuditReport(
        command.getDocumentId(),
        command.getUserId(),
        command.getActionType(),
        command.getStartTime(),
        command.getEndTime());
  }

  /**
   * 获取客户端IP地址。
   *
   * @param request HTTP请求
   * @return IP地址
   */
  private String getClientIp(final HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty()) {
      ip = request.getRemoteAddr();
    }
    return ip;
  }
}
