package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.OperationLogDTO;
import com.lawfirm.application.system.dto.OperationLogQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.system.entity.OperationLog;
import com.lawfirm.domain.system.repository.OperationLogRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 操作日志应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogAppService {

  /** 操作日志仓储 */
  private final OperationLogRepository operationLogRepository;

  /**
   * 分页查询操作日志
   *
   * @param query 查询参数
   * @return 分页结果
   */
  public PageResult<OperationLogDTO> listOperationLogs(final OperationLogQueryDTO query) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

    // 模块过滤
    if (StringUtils.hasText(query.getModule())) {
      wrapper.eq(OperationLog::getModule, query.getModule());
    }

    // 操作类型过滤
    if (StringUtils.hasText(query.getOperationType())) {
      wrapper.eq(OperationLog::getOperationType, query.getOperationType());
    }

    // 用户名模糊查询
    if (StringUtils.hasText(query.getUserName())) {
      wrapper.like(OperationLog::getUserName, query.getUserName());
    }

    // 用户ID精确查询
    if (query.getUserId() != null) {
      wrapper.eq(OperationLog::getUserId, query.getUserId());
    }

    // 状态过滤
    if (StringUtils.hasText(query.getStatus())) {
      wrapper.eq(OperationLog::getStatus, query.getStatus());
    }

    // IP地址模糊查询
    if (StringUtils.hasText(query.getIpAddress())) {
      wrapper.like(OperationLog::getIpAddress, query.getIpAddress());
    }

    // 请求URL模糊查询
    if (StringUtils.hasText(query.getRequestUrl())) {
      wrapper.like(OperationLog::getRequestUrl, query.getRequestUrl());
    }

    // 时间范围过滤
    if (query.getStartTime() != null) {
      wrapper.ge(OperationLog::getCreatedAt, query.getStartTime());
    }
    if (query.getEndTime() != null) {
      wrapper.le(OperationLog::getCreatedAt, query.getEndTime());
    }

    // 执行时长过滤（慢请求）
    if (query.getMinExecutionTime() != null) {
      wrapper.ge(OperationLog::getExecutionTime, query.getMinExecutionTime());
    }

    // 排除已删除
    wrapper.eq(OperationLog::getDeleted, false);

    // 按创建时间倒序
    wrapper.orderByDesc(OperationLog::getCreatedAt);

    // 分页查询
    IPage<OperationLog> page =
        operationLogRepository.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

    List<OperationLogDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取操作日志详情
   *
   * @param id 日志ID
   * @return 日志DTO
   */
  public OperationLogDTO getOperationLogById(final Long id) {
    OperationLog log = operationLogRepository.getById(id);
    return log != null ? toDTO(log) : null;
  }

  /**
   * 获取所有模块列表（用于下拉选择）
   *
   * @return 模块名称列表
   */
  public List<String> listModules() {
    return operationLogRepository
        .lambdaQuery()
        .select(OperationLog::getModule)
        .eq(OperationLog::getDeleted, false)
        .groupBy(OperationLog::getModule)
        .list()
        .stream()
        .map(OperationLog::getModule)
        .filter(StringUtils::hasText)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * 获取所有操作类型列表（用于下拉选择）
   *
   * @return 操作类型列表
   */
  public List<String> listOperationTypes() {
    return operationLogRepository
        .lambdaQuery()
        .select(OperationLog::getOperationType)
        .eq(OperationLog::getDeleted, false)
        .groupBy(OperationLog::getOperationType)
        .list()
        .stream()
        .map(OperationLog::getOperationType)
        .filter(StringUtils::hasText)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * 获取操作日志统计
   *
   * @param query 查询参数
   * @return 统计数据
   */
  public Map<String, Object> getStatistics(final OperationLogQueryDTO query) {
    Map<String, Object> stats = new HashMap<>();

    LambdaQueryWrapper<OperationLog> baseWrapper = new LambdaQueryWrapper<>();
    baseWrapper.eq(OperationLog::getDeleted, false);

    // 时间范围
    if (query.getStartTime() != null) {
      baseWrapper.ge(OperationLog::getCreatedAt, query.getStartTime());
    }
    if (query.getEndTime() != null) {
      baseWrapper.le(OperationLog::getCreatedAt, query.getEndTime());
    }

    // 总数
    long total = operationLogRepository.count(baseWrapper);
    stats.put("total", total);

    // 成功数
    LambdaQueryWrapper<OperationLog> successWrapper = baseWrapper.clone();
    successWrapper.eq(OperationLog::getStatus, OperationLog.STATUS_SUCCESS);
    long successCount = operationLogRepository.count(successWrapper);
    stats.put("successCount", successCount);

    // 失败数
    stats.put("failCount", total - successCount);

    // 成功率
    stats.put(
        "successRate", total > 0 ? String.format("%.2f%%", successCount * 100.0 / total) : "0%");

    return stats;
  }

  /**
   * 清理历史日志（保留指定天数）
   *
   * @param keepDays 保留天数
   * @return 删除数量
   */
  public int cleanHistoryLogs(final int keepDays) {
    int finalKeepDays = keepDays;
    if (finalKeepDays < 7) {
      finalKeepDays = 7; // 最少保留7天
    }

    java.time.LocalDateTime cutoffTime = java.time.LocalDateTime.now().minusDays(finalKeepDays);

    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.lt(OperationLog::getCreatedAt, cutoffTime);

    int deleted = operationLogRepository.getBaseMapper().delete(wrapper);
    log.info("清理历史操作日志: 保留{}天, 删除{}条", keepDays, deleted);

    return deleted;
  }

  /**
   * 查询日志列表（不分页，用于导出）
   *
   * @param query 查询参数
   * @param maxRows 最大行数
   * @return 日志列表
   */
  public List<OperationLogDTO> listForExport(final OperationLogQueryDTO query, final int maxRows) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

    // 模块过滤
    if (StringUtils.hasText(query.getModule())) {
      wrapper.eq(OperationLog::getModule, query.getModule());
    }

    // 操作类型过滤
    if (StringUtils.hasText(query.getOperationType())) {
      wrapper.eq(OperationLog::getOperationType, query.getOperationType());
    }

    // 用户名模糊查询
    if (StringUtils.hasText(query.getUserName())) {
      wrapper.like(OperationLog::getUserName, query.getUserName());
    }

    // 用户ID精确查询
    if (query.getUserId() != null) {
      wrapper.eq(OperationLog::getUserId, query.getUserId());
    }

    // 状态过滤
    if (StringUtils.hasText(query.getStatus())) {
      wrapper.eq(OperationLog::getStatus, query.getStatus());
    }

    // IP地址模糊查询
    if (StringUtils.hasText(query.getIpAddress())) {
      wrapper.like(OperationLog::getIpAddress, query.getIpAddress());
    }

    // 时间范围过滤
    if (query.getStartTime() != null) {
      wrapper.ge(OperationLog::getCreatedAt, query.getStartTime());
    }
    if (query.getEndTime() != null) {
      wrapper.le(OperationLog::getCreatedAt, query.getEndTime());
    }

    // 排除已删除
    wrapper.eq(OperationLog::getDeleted, false);

    // 按创建时间倒序
    wrapper.orderByDesc(OperationLog::getCreatedAt);

    // 限制最大行数
    wrapper.last("LIMIT " + maxRows);

    return operationLogRepository.list(wrapper).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 实体转DTO
   *
   * @param entity 日志实体
   * @return 日志DTO
   */
  private OperationLogDTO toDTO(final OperationLog entity) {
    return OperationLogDTO.builder()
        .id(entity.getId())
        .userId(entity.getUserId())
        .userName(entity.getUserName())
        .module(entity.getModule())
        .operationType(entity.getOperationType())
        .description(entity.getDescription())
        .method(entity.getMethod())
        .requestUrl(entity.getRequestUrl())
        .requestMethod(entity.getRequestMethod())
        .requestParams(entity.getRequestParams())
        .ipAddress(entity.getIpAddress())
        .executionTime(entity.getExecutionTime())
        .status(entity.getStatus())
        .statusName(OperationLog.STATUS_SUCCESS.equals(entity.getStatus()) ? "成功" : "失败")
        .errorMessage(entity.getErrorMessage())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}
