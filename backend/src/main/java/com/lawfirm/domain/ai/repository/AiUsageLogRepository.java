package com.lawfirm.domain.ai.repository;

import com.lawfirm.application.ai.dto.AiUsageLogDTO;
import com.lawfirm.application.ai.dto.AiUsageQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.ai.entity.AiUsageLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** AI使用记录仓储接口 */
public interface AiUsageLogRepository {

  /**
   * 分页查询
   *
   * @param query 查询条件
   * @return 分页结果
   */
  PageResult<AiUsageLogDTO> queryPage(AiUsageQueryDTO query);

  /**
   * 获取统计摘要
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 统计摘要
   */
  Map<String, Object> getSummary(Long userId, LocalDate startDate, LocalDate endDate);

  /**
   * 保存使用记录
   *
   * @param usageLog 使用记录
   */
  void save(AiUsageLog usageLog);

  /**
   * 根据ID查询
   *
   * @param id 记录ID
   * @return 使用记录
   */
  AiUsageLog findById(Long id);

  /**
   * 查询用户使用记录（分页）
   *
   * @param userId 用户ID
   * @param offset 偏移量
   * @param limit 限制数量
   * @return 使用记录列表
   */
  List<AiUsageLog> findByUserId(Long userId, int offset, int limit);

  /**
   * 查询用户使用记录总数
   *
   * @param userId 用户ID
   * @return 记录总数
   */
  long countByUserId(Long userId);

  /**
   * 查询用户在指定时间范围的使用记录
   *
   * @param userId 用户ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 使用记录列表
   */
  List<AiUsageLog> findByUserIdAndTimeRange(
      Long userId, LocalDateTime startTime, LocalDateTime endTime);

  /**
   * 获取用户月度统计
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 月度统计
   */
  Map<String, Object> getUserMonthlySummary(Long userId, LocalDate startDate, LocalDate endDate);

  /**
   * 获取用户按模型分组的使用统计
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 按模型分组的统计列表
   */
  List<Map<String, Object>> getUsageByModel(Long userId, LocalDate startDate, LocalDate endDate);

  /**
   * 获取所有用户的月度统计（管理员）
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 所有用户的月度统计列表
   */
  List<Map<String, Object>> getAllUsersMonthlySummary(LocalDate startDate, LocalDate endDate);

  /**
   * 获取部门统计
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 部门统计列表
   */
  List<Map<String, Object>> getDepartmentSummary(LocalDate startDate, LocalDate endDate);

  /**
   * 获取指定月份需要生成账单的用户列表
   *
   * @param year 年份
   * @param month 月份
   * @return 用户列表
   */
  List<Map<String, Object>> getUsersForBilling(int year, int month);

  /**
   * 获取使用趋势（按日统计）
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 使用趋势列表
   */
  List<Map<String, Object>> getUsageTrend(Long userId, LocalDate startDate, LocalDate endDate);

  /**
   * 获取全员统计（返回DTO）
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 全员统计列表
   */
  List<com.lawfirm.application.ai.dto.AiUsageSummaryDTO> getAllUsersSummary(
      LocalDate startDate, LocalDate endDate);

  /**
   * 获取指定时间段内有使用记录的用户列表
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 活跃用户列表
   */
  List<Map<String, Object>> getActiveUsersInPeriod(LocalDate startDate, LocalDate endDate);
}
