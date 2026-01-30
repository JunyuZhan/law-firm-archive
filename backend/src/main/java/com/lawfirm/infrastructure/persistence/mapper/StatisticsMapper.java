package com.lawfirm.infrastructure.persistence.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 统计查询 Mapper */
@Mapper
public interface StatisticsMapper {

  /**
   * 统计总收入（所有已确认的收款，根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 总收入
   */
  @Select(
      "<script>"
          + "SELECT COALESCE(SUM(p.amount), 0) "
          + "FROM finance_payment p "
          + "LEFT JOIN finance_fee f ON p.fee_id = f.id "
          + "WHERE p.status = 'CONFIRMED' AND p.deleted = false "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND f.matter_id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "<if test=\"matterIds == null\">"
          + "AND f.matter_id IS NOT NULL"
          + "</if>"
          + "</script>")
  BigDecimal sumTotalRevenue(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计本月收入（根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 本月收入
   */
  @Select(
      "<script>"
          + "SELECT COALESCE(SUM(p.amount), 0) "
          + "FROM finance_payment p "
          + "LEFT JOIN finance_fee f ON p.fee_id = f.id "
          + "WHERE p.status = 'CONFIRMED' AND p.deleted = false "
          + "AND DATE_TRUNC('month', p.payment_date) = DATE_TRUNC('month', CURRENT_DATE) "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND f.matter_id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "<if test=\"matterIds == null\">"
          + "AND f.matter_id IS NOT NULL"
          + "</if>"
          + "</script>")
  BigDecimal sumMonthlyRevenue(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计上月收入（根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 上月收入
   */
  @Select(
      "<script>"
          + "SELECT COALESCE(SUM(p.amount), 0) "
          + "FROM finance_payment p "
          + "LEFT JOIN finance_fee f ON p.fee_id = f.id "
          + "WHERE p.status = 'CONFIRMED' AND p.deleted = false "
          + "AND DATE_TRUNC('month', p.payment_date) = DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month') "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND f.matter_id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "<if test=\"matterIds == null\">"
          + "AND f.matter_id IS NOT NULL"
          + "</if>"
          + "</script>")
  BigDecimal sumLastMonthRevenue(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计本年收入（根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 本年收入
   */
  @Select(
      "<script>"
          + "SELECT COALESCE(SUM(p.amount), 0) "
          + "FROM finance_payment p "
          + "LEFT JOIN finance_fee f ON p.fee_id = f.id "
          + "WHERE p.status = 'CONFIRMED' AND p.deleted = false "
          + "AND DATE_TRUNC('year', p.payment_date) = DATE_TRUNC('year', CURRENT_DATE) "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND f.matter_id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "<if test=\"matterIds == null\">"
          + "AND f.matter_id IS NOT NULL"
          + "</if>"
          + "</script>")
  BigDecimal sumYearlyRevenue(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计待收金额（收费记录中未收完的金额，根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 待收金额
   */
  @Select(
      "<script>"
          + "SELECT COALESCE(SUM(f.amount - COALESCE(f.paid_amount, 0)), 0) "
          + "FROM finance_fee f "
          + "WHERE f.deleted = false AND f.status != 'CANCELLED' "
          + "AND (f.paid_amount IS NULL OR f.paid_amount &lt; f.amount) "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND f.matter_id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "<if test=\"matterIds == null\">"
          + "AND f.matter_id IS NOT NULL"
          + "</if>"
          + "</script>")
  BigDecimal sumPendingRevenue(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计各状态案件数（根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 各状态案件数统计
   */
  @Select(
      "<script>"
          + "SELECT status, COUNT(*) as count "
          + "FROM matter "
          + "WHERE deleted = false "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "GROUP BY status"
          + "</script>")
  List<Map<String, Object>> countMattersByStatus(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计各类型案件数（根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 各类型案件数统计
   */
  @Select(
      "<script>"
          + "SELECT business_type, COUNT(*) as count "
          + "FROM matter "
          + "WHERE deleted = false "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "GROUP BY business_type"
          + "</script>")
  List<Map<String, Object>> countMattersByType(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计进行中案件数（根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 进行中案件数
   */
  @Select(
      "<script>"
          + "SELECT COUNT(*) "
          + "FROM matter "
          + "WHERE deleted = false AND status IN ('IN_PROGRESS', 'PENDING') "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "</script>")
  Long countActiveMatters(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计已完成案件数（根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 已完成案件数
   */
  @Select(
      "<script>"
          + "SELECT COUNT(*) "
          + "FROM matter "
          + "WHERE deleted = false AND status = 'COMPLETED' "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "</script>")
  Long countCompletedMatters(@Param("matterIds") List<Long> matterIds);

  /**
   * 统计各类型客户数（根据客户权限过滤）.
   *
   * @param clientIds 客户ID列表
   * @return 各类型客户数统计
   */
  @Select(
      "<script>"
          + "SELECT client_type, COUNT(*) as count "
          + "FROM crm_client "
          + "WHERE deleted = false "
          + "<if test=\"clientIds != null and clientIds.size() &gt; 0\">"
          + "AND id IN "
          + "<foreach collection=\"clientIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "GROUP BY client_type"
          + "</script>")
  List<Map<String, Object>> countClientsByType(@Param("clientIds") List<Long> clientIds);

  /**
   * 统计正式客户数（根据客户权限过滤）.
   *
   * @param clientIds 客户ID列表
   * @return 正式客户数
   */
  @Select(
      "<script>"
          + "SELECT COUNT(*) "
          + "FROM crm_client "
          + "WHERE deleted = false AND status = 'FORMAL' "
          + "<if test=\"clientIds != null and clientIds.size() &gt; 0\">"
          + "AND id IN "
          + "<foreach collection=\"clientIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "</script>")
  Long countFormalClients(@Param("clientIds") List<Long> clientIds);

  /**
   * 统计潜在客户数（根据客户权限过滤）.
   *
   * @param clientIds 客户ID列表
   * @return 潜在客户数
   */
  @Select(
      "<script>"
          + "SELECT COUNT(*) "
          + "FROM crm_client "
          + "WHERE deleted = false AND status = 'POTENTIAL' "
          + "<if test=\"clientIds != null and clientIds.size() &gt; 0\">"
          + "AND id IN "
          + "<foreach collection=\"clientIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "</script>")
  Long countPotentialClients(@Param("clientIds") List<Long> clientIds);

  /**
   * 统计本月新增客户数（根据客户权限过滤）.
   *
   * @param clientIds 客户ID列表
   * @return 本月新增客户数
   */
  @Select(
      "<script>"
          + "SELECT COUNT(*) "
          + "FROM crm_client "
          + "WHERE deleted = false "
          + "AND DATE_TRUNC('month', created_at) = DATE_TRUNC('month', CURRENT_DATE) "
          + "<if test=\"clientIds != null and clientIds.size() &gt; 0\">"
          + "AND id IN "
          + "<foreach collection=\"clientIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "</script>")
  Long countNewClientsThisMonth(@Param("clientIds") List<Long> clientIds);

  /**
   * 获取收入趋势（最近12个月，根据项目权限过滤）.
   *
   * @param matterIds 项目ID列表
   * @return 收入趋势数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "TO_CHAR(DATE_TRUNC('month', p.payment_date), 'YYYY-MM') as period, "
          + "COALESCE(SUM(p.amount), 0) as amount "
          + "FROM finance_payment p "
          + "LEFT JOIN finance_fee f ON p.fee_id = f.id "
          + "WHERE p.status = 'CONFIRMED' AND p.deleted = false "
          + "AND p.payment_date &gt;= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '11 months') "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND f.matter_id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "<if test=\"matterIds == null\">"
          + "AND f.matter_id IS NOT NULL"
          + "</if>"
          + "GROUP BY DATE_TRUNC('month', p.payment_date) "
          + "ORDER BY period"
          + "</script>")
  List<Map<String, Object>> getRevenueTrends(@Param("matterIds") List<Long> matterIds);

  /**
   * 获取律师业绩排行（按收入，根据项目权限过滤）.
   *
   * @param limit 限制数量
   * @param matterIds 项目ID列表
   * @return 律师业绩排行数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "mp.user_id as lawyer_id, "
          + "u.real_name as lawyer_name, "
          + "COUNT(DISTINCT mp.matter_id) as matter_count, "
          + "COALESCE(SUM(p.amount), 0) as total_revenue, "
          + "COALESCE(SUM(ts.hours), 0) as total_hours, "
          + "CASE WHEN COUNT(DISTINCT mp.matter_id) &gt; 0 "
          + "THEN COALESCE(SUM(p.amount), 0) / COUNT(DISTINCT mp.matter_id) "
          + "ELSE 0 END as avg_revenue "
          + "FROM matter_participant mp "
          + "LEFT JOIN sys_user u ON mp.user_id = u.id "
          + "LEFT JOIN matter m ON mp.matter_id = m.id "
          + "LEFT JOIN finance_fee f ON m.id = f.matter_id "
          + "LEFT JOIN finance_payment p ON f.id = p.fee_id AND p.status = 'CONFIRMED' AND p.deleted = false "
          + "LEFT JOIN timesheet ts ON mp.matter_id = ts.matter_id AND mp.user_id = ts.user_id AND ts.deleted = false "
          + "WHERE mp.deleted = false AND m.deleted = false "
          + "<if test=\"matterIds != null and matterIds.size() &gt; 0\">"
          + "AND mp.matter_id IN "
          + "<foreach collection=\"matterIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "GROUP BY mp.user_id, u.real_name "
          + "ORDER BY total_revenue DESC "
          + "LIMIT #{limit}"
          + "</script>")
  List<Map<String, Object>> getLawyerPerformanceRanking(
      @Param("limit") Integer limit, @Param("matterIds") List<Long> matterIds);

  /**
   * 查询收入报表数据.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param clientId 客户ID
   * @return 收入报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "TO_CHAR(p.payment_date, 'YYYY-MM-DD') as date, "
          + "c.name as client_name, "
          + "m.name as matter_name, "
          + "p.amount, "
          + "p.status "
          + "FROM finance_payment p "
          + "LEFT JOIN finance_fee f ON p.fee_id = f.id "
          + "LEFT JOIN matter m ON f.matter_id = m.id "
          + "LEFT JOIN crm_client c ON m.client_id = c.id "
          + "WHERE p.deleted = false "
          + "<if test=\"startDate != null\">"
          + "AND p.payment_date &gt;= #{startDate}"
          + "</if>"
          + "<if test=\"endDate != null\">"
          + "AND p.payment_date &lt;= #{endDate}"
          + "</if>"
          + "<if test=\"clientId != null\">"
          + "AND c.id = #{clientId}"
          + "</if>"
          + "ORDER BY p.payment_date DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryRevenueReportData(
      @Param("startDate") String startDate,
      @Param("endDate") String endDate,
      @Param("clientId") Long clientId);

  /**
   * 查询案件报表数据.
   *
   * @param status 状态
   * @param matterType 案件类型
   * @return 案件报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "m.matter_no, "
          + "m.name as matter_name, "
          + "c.name as client_name, "
          + "m.business_type as matter_type, "
          + "m.status, "
          + "TO_CHAR(m.created_at, 'YYYY-MM-DD') as created_at "
          + "FROM matter m "
          + "LEFT JOIN crm_client c ON m.client_id = c.id "
          + "WHERE m.deleted = false "
          + "<if test=\"status != null and status != ''\">"
          + "AND m.status = #{status}"
          + "</if>"
          + "<if test=\"matterType != null and matterType != ''\">"
          + "AND m.business_type = #{matterType}"
          + "</if>"
          + "ORDER BY m.created_at DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryMatterReportData(
      @Param("status") String status, @Param("matterType") String matterType);

  /**
   * 查询客户报表数据.
   *
   * @param clientType 客户类型
   * @param status 状态
   * @return 客户报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "c.client_no, "
          + "c.name as client_name, "
          + "c.client_type, "
          + "c.contact_person, "
          + "c.contact_phone, "
          + "c.status "
          + "FROM crm_client c "
          + "WHERE c.deleted = false "
          + "<if test=\"clientType != null and clientType != ''\">"
          + "AND c.client_type = #{clientType}"
          + "</if>"
          + "<if test=\"status != null and status != ''\">"
          + "AND c.status = #{status}"
          + "</if>"
          + "ORDER BY c.created_at DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryClientReportData(
      @Param("clientType") String clientType, @Param("status") String status);

  /**
   * 查询应收报表数据 应收金额 = 合同金额 - 已收金额.
   *
   * @param clientId 客户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 应收报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "c.name as client_name, "
          + "m.name as matter_name, "
          + "m.matter_no, "
          + "ct.contract_no, "
          + "ct.total_amount as contract_amount, "
          + "COALESCE(received.received_amount, 0) as received_amount, "
          + "(ct.total_amount - COALESCE(received.received_amount, 0)) as receivable_amount, "
          + "CASE "
          + "WHEN (ct.total_amount - COALESCE(received.received_amount, 0)) &gt; 0 "
          + "THEN CURRENT_DATE - ct.sign_date "
          + "ELSE 0 "
          + "END as aging_days, "
          + "ct.sign_date, "
          + "CASE "
          + "WHEN (ct.total_amount - COALESCE(received.received_amount, 0)) = 0 THEN '已收清' "
          + "WHEN (ct.total_amount - COALESCE(received.received_amount, 0)) = ct.total_amount THEN '未收款' "
          + "ELSE '部分收款' "
          + "END as receivable_status "
          + "FROM finance_contract ct "
          + "LEFT JOIN matter m ON ct.matter_id = m.id "
          + "LEFT JOIN crm_client c ON ct.client_id = c.id "
          + "LEFT JOIN ("
          + "SELECT contract_id, SUM(amount) as received_amount "
          + "FROM finance_payment "
          + "WHERE status = 'CONFIRMED' AND deleted = false "
          + "GROUP BY contract_id"
          + ") received ON ct.id = received.contract_id "
          + "WHERE ct.deleted = false "
          + "AND (ct.total_amount - COALESCE(received.received_amount, 0)) &gt; 0 "
          + "<if test=\"clientId != null\">"
          + "AND c.id = #{clientId}"
          + "</if>"
          + "<if test=\"startDate != null\">"
          + "AND ct.sign_date &gt;= #{startDate}"
          + "</if>"
          + "<if test=\"endDate != null\">"
          + "AND ct.sign_date &lt;= #{endDate}"
          + "</if>"
          + "ORDER BY aging_days DESC, ct.sign_date DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryReceivableReportData(
      @Param("clientId") Long clientId,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /**
   * 查询项目进度报表数据（M3-025）.
   *
   * @param status 状态
   * @param matterType 案件类型
   * @param leadLawyerId 主办律师ID
   * @param clientId 客户ID
   * @return 项目进度报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "m.id as matter_id, "
          + "m.matter_no, "
          + "m.name as matter_name, "
          + "c.name as client_name, "
          + "m.business_type, "
          + "m.status, "
          + "m.fee_type, "
          + "m.estimated_fee, "
          + "m.actual_fee, "
          + "TO_CHAR(m.created_at, 'YYYY-MM-DD') as created_date, "
          + "TO_CHAR(m.expected_end_date, 'YYYY-MM-DD') as expected_end_date, "
          + "TO_CHAR(m.actual_end_date, 'YYYY-MM-DD') as actual_end_date, "
          + "u.real_name as lead_lawyer_name, "
          + "COALESCE(task_stats.total_tasks, 0) as total_tasks, "
          + "COALESCE(task_stats.completed_tasks, 0) as completed_tasks, "
          + "COALESCE(task_stats.in_progress_tasks, 0) as in_progress_tasks, "
          + "COALESCE(timesheet_stats.total_hours, 0) as total_hours, "
          + "COALESCE(timesheet_stats.approved_hours, 0) as approved_hours, "
          + "CASE "
          + "WHEN COALESCE(task_stats.total_tasks, 0) &gt; 0 "
          + "THEN ROUND(COALESCE(task_stats.completed_tasks, 0) * 100.0 / task_stats.total_tasks, 2) "
          + "ELSE 0 "
          + "END as task_completion_rate "
          + "FROM matter m "
          + "LEFT JOIN crm_client c ON m.client_id = c.id "
          + "LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "COUNT(*) as total_tasks, "
          + "COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_tasks, "
          + "COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') as in_progress_tasks "
          + "FROM task "
          + "WHERE deleted = false "
          + "GROUP BY matter_id"
          + ") task_stats ON m.id = task_stats.matter_id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "COALESCE(SUM(hours), 0) as total_hours, "
          + "COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN hours ELSE 0 END), 0) as approved_hours "
          + "FROM timesheet "
          + "WHERE deleted = false "
          + "GROUP BY matter_id"
          + ") timesheet_stats ON m.id = timesheet_stats.matter_id "
          + "WHERE m.deleted = false "
          + "<if test=\"status != null and status != ''\">"
          + "AND m.status = #{status}"
          + "</if>"
          + "<if test=\"matterType != null and matterType != ''\">"
          + "AND m.business_type = #{matterType}"
          + "</if>"
          + "<if test=\"leadLawyerId != null\">"
          + "AND m.lead_lawyer_id = #{leadLawyerId}"
          + "</if>"
          + "<if test=\"clientId != null\">"
          + "AND m.client_id = #{clientId}"
          + "</if>"
          + "ORDER BY m.created_at DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryMatterProgressReportData(
      @Param("status") String status,
      @Param("matterType") String matterType,
      @Param("leadLawyerId") Long leadLawyerId,
      @Param("clientId") Long clientId);

  /**
   * 查询项目工时报表数据（M3-026）.
   *
   * @param matterId 项目ID
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 项目工时报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "m.id as matter_id, "
          + "m.matter_no, "
          + "m.name as matter_name, "
          + "c.name as client_name, "
          + "u.real_name as lead_lawyer_name, "
          + "TO_CHAR(ts.work_date, 'YYYY-MM-DD') as work_date, "
          + "ts.work_type, "
          + "ts.work_content, "
          + "ts.hours, "
          + "ts.billable, "
          + "ts.status as timesheet_status, "
          + "u2.real_name as worker_name "
          + "FROM timesheet ts "
          + "LEFT JOIN matter m ON ts.matter_id = m.id "
          + "LEFT JOIN crm_client c ON m.client_id = c.id "
          + "LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id "
          + "LEFT JOIN sys_user u2 ON ts.user_id = u2.id "
          + "WHERE ts.deleted = false AND m.deleted = false "
          + "<if test=\"matterId != null\">"
          + "AND ts.matter_id = #{matterId}"
          + "</if>"
          + "<if test=\"userId != null\">"
          + "AND ts.user_id = #{userId}"
          + "</if>"
          + "<if test=\"startDate != null\">"
          + "AND ts.work_date &gt;= #{startDate}"
          + "</if>"
          + "<if test=\"endDate != null\">"
          + "AND ts.work_date &lt;= #{endDate}"
          + "</if>"
          + "ORDER BY ts.work_date DESC, m.matter_no "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryMatterTimesheetReportData(
      @Param("matterId") Long matterId,
      @Param("userId") Long userId,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /**
   * 查询项目任务报表数据（M3-027）.
   *
   * @param matterId 项目ID
   * @param assigneeId 执行人ID
   * @param status 状态
   * @return 项目任务报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "m.id as matter_id, "
          + "m.matter_no, "
          + "m.name as matter_name, "
          + "c.name as client_name, "
          + "t.task_no, "
          + "t.title as task_title, "
          + "t.priority, "
          + "t.status as task_status, "
          + "t.progress, "
          + "TO_CHAR(t.due_date, 'YYYY-MM-DD') as due_date, "
          + "TO_CHAR(t.completed_at, 'YYYY-MM-DD HH24:MI:SS') as completed_at, "
          + "u.real_name as assignee_name, "
          + "CASE "
          + "WHEN t.completed_at IS NOT NULL THEN '已完成' "
          + "WHEN t.due_date IS NOT NULL AND t.due_date &lt; CURRENT_DATE AND t.status != 'COMPLETED' THEN '逾期' "
          + "WHEN t.status = 'IN_PROGRESS' THEN '进行中' "
          + "ELSE '待办' "
          + "END as task_status_name "
          + "FROM task t "
          + "LEFT JOIN matter m ON t.matter_id = m.id "
          + "LEFT JOIN crm_client c ON m.client_id = c.id "
          + "LEFT JOIN sys_user u ON t.assignee_id = u.id "
          + "WHERE t.deleted = false AND m.deleted = false "
          + "<if test=\"matterId != null\">"
          + "AND t.matter_id = #{matterId}"
          + "</if>"
          + "<if test=\"assigneeId != null\">"
          + "AND t.assignee_id = #{assigneeId}"
          + "</if>"
          + "<if test=\"status != null and status != ''\">"
          + "AND t.status = #{status}"
          + "</if>"
          + "ORDER BY t.due_date ASC NULLS LAST, t.created_at DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryMatterTaskReportData(
      @Param("matterId") Long matterId,
      @Param("assigneeId") Long assigneeId,
      @Param("status") String status);

  /**
   * 查询项目阶段进度报表数据（M3-028）.
   *
   * @param status 状态
   * @param matterType 案件类型
   * @param departmentId 部门ID
   * @return 项目阶段进度报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "m.id as matter_id, "
          + "m.matter_no, "
          + "m.name as matter_name, "
          + "c.name as client_name, "
          + "m.business_type, "
          + "m.status, "
          + "u.real_name as lead_lawyer_name, "
          + "d.name as department_name, "
          + "TO_CHAR(m.created_at, 'YYYY-MM-DD') as created_date, "
          + "TO_CHAR(m.expected_end_date, 'YYYY-MM-DD') as expected_end_date, "
          + "TO_CHAR(m.actual_end_date, 'YYYY-MM-DD') as actual_end_date, "
          + "CASE "
          + "WHEN m.actual_end_date IS NOT NULL THEN '已完成' "
          + "WHEN m.expected_end_date IS NOT NULL AND m.expected_end_date &lt; CURRENT_DATE THEN '逾期' "
          + "WHEN m.status = 'ACTIVE' THEN '进行中' "
          + "ELSE m.status "
          + "END as progress_status, "
          + "COALESCE(task_stats.total_tasks, 0) as total_tasks, "
          + "COALESCE(task_stats.completed_tasks, 0) as completed_tasks, "
          + "CASE "
          + "WHEN COALESCE(task_stats.total_tasks, 0) &gt; 0 "
          + "THEN ROUND(COALESCE(task_stats.completed_tasks, 0) * 100.0 / task_stats.total_tasks, 2) "
          + "ELSE 0 "
          + "END as completion_rate "
          + "FROM matter m "
          + "LEFT JOIN crm_client c ON m.client_id = c.id "
          + "LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id "
          + "LEFT JOIN sys_department d ON m.department_id = d.id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "COUNT(*) as total_tasks, "
          + "COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_tasks "
          + "FROM task "
          + "WHERE deleted = false "
          + "GROUP BY matter_id"
          + ") task_stats ON m.id = task_stats.matter_id "
          + "WHERE m.deleted = false "
          + "<if test=\"status != null and status != ''\">"
          + "AND m.status = #{status}"
          + "</if>"
          + "<if test=\"matterType != null and matterType != ''\">"
          + "AND m.business_type = #{matterType}"
          + "</if>"
          + "<if test=\"departmentId != null\">"
          + "AND m.department_id = #{departmentId}"
          + "</if>"
          + "ORDER BY m.created_at DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryMatterStageReportData(
      @Param("status") String status,
      @Param("matterType") String matterType,
      @Param("departmentId") Long departmentId);

  /**
   * 查询项目趋势分析报表数据（M3-029）.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 项目趋势分析报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') as period, "
          + "COUNT(*) as new_matters_count, "
          + "COUNT(*) FILTER (WHERE status = 'CLOSED') as closed_matters_count, "
          + "COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_matters_count, "
          + "AVG(CASE "
          + "WHEN actual_end_date IS NOT NULL AND created_at IS NOT NULL "
          + "THEN EXTRACT(EPOCH FROM (actual_end_date - created_at::date)) / 86400 "
          + "ELSE NULL "
          + "END) as avg_duration_days "
          + "FROM matter "
          + "WHERE deleted = false "
          + "<if test=\"startDate != null\">"
          + "AND created_at &gt;= #{startDate}::timestamp"
          + "</if>"
          + "<if test=\"endDate != null\">"
          + "AND created_at &lt;= #{endDate}::timestamp"
          + "</if>"
          + "GROUP BY DATE_TRUNC('month', created_at) "
          + "ORDER BY period DESC "
          + "LIMIT 24"
          + "</script>")
  List<Map<String, Object>> queryMatterTrendReportData(
      @Param("startDate") String startDate, @Param("endDate") String endDate);

  /**
   * 查询项目成本分析报表数据（M4-044）.
   *
   * @param matterId 项目ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 项目成本分析报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "m.id as matter_id, "
          + "m.matter_no, "
          + "m.name as matter_name, "
          + "c.name as client_name, "
          + "u.real_name as lead_lawyer_name, "
          + "COALESCE(allocated_costs.allocated_cost, 0) as allocated_cost, "
          + "COALESCE(split_costs.split_cost, 0) as split_cost, "
          + "COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0) as total_cost, "
          + "COALESCE(ct.total_amount, 0) as contract_amount, "
          + "COALESCE(received.received_amount, 0) as received_amount, "
          + "COALESCE(received.received_amount, 0) - "
          + "(COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) as profit, "
          + "CASE "
          + "WHEN COALESCE(received.received_amount, 0) &gt; 0 "
          + "THEN ((COALESCE(received.received_amount, 0) - "
          + "(COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0))) * 100.0 / "
          + "received.received_amount) "
          + "ELSE 0 "
          + "END as profit_rate "
          + "FROM matter m "
          + "LEFT JOIN crm_client c ON m.client_id = c.id "
          + "LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "SUM(amount) as allocated_cost "
          + "FROM finance_expense "
          + "WHERE status = 'PAID' "
          + "AND is_cost_allocation = true "
          + "AND deleted = false "
          + "<if test=\"startDate != null\">"
          + "AND expense_date &gt;= #{startDate}::date"
          + "</if>"
          + "<if test=\"endDate != null\">"
          + "AND expense_date &lt;= #{endDate}::date"
          + "</if>"
          + "GROUP BY matter_id"
          + ") allocated_costs ON m.id = allocated_costs.matter_id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "SUM(split_amount) as split_cost "
          + "FROM finance_cost_split "
          + "WHERE deleted = false "
          + "<if test=\"startDate != null\">"
          + "AND split_date &gt;= #{startDate}::date"
          + "</if>"
          + "<if test=\"endDate != null\">"
          + "AND split_date &lt;= #{endDate}::date"
          + "</if>"
          + "GROUP BY matter_id"
          + ") split_costs ON m.id = split_costs.matter_id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "SUM(total_amount) as total_amount "
          + "FROM finance_contract "
          + "WHERE deleted = false "
          + "GROUP BY matter_id"
          + ") ct ON m.id = ct.matter_id "
          + "LEFT JOIN ("
          + "SELECT "
          + "p.matter_id, "
          + "SUM(p.amount) as received_amount "
          + "FROM finance_payment p "
          + "WHERE p.status = 'CONFIRMED' AND p.deleted = false "
          + "GROUP BY p.matter_id"
          + ") received ON m.id = received.matter_id "
          + "WHERE m.deleted = false "
          + "<if test=\"matterId != null\">"
          + "AND m.id = #{matterId}"
          + "</if>"
          + "HAVING (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) &gt; 0 "
          + "OR COALESCE(received.received_amount, 0) &gt; 0 "
          + "ORDER BY total_cost DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryCostAnalysisReportData(
      @Param("matterId") Long matterId,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /**
   * 查询应收账款账龄分析报表数据（M4-053）.
   *
   * @param clientId 客户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 应收账款账龄分析报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "c.name as client_name, "
          + "m.name as matter_name, "
          + "m.matter_no, "
          + "ct.contract_no, "
          + "ct.total_amount as contract_amount, "
          + "COALESCE(SUM(p.amount), 0) as received_amount, "
          + "(ct.total_amount - COALESCE(SUM(p.amount), 0)) as receivable_amount, "
          + "CASE "
          + "WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) &gt; 0 "
          + "THEN CURRENT_DATE - ct.sign_date "
          + "ELSE 0 "
          + "END as aging_days, "
          + "CASE "
          + "WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) = 0 THEN '已收清' "
          + "WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) &gt; 0 "
          + "AND (CURRENT_DATE - ct.sign_date) &lt;= 30 THEN '0-30天' "
          + "WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) &gt; 0 "
          + "AND (CURRENT_DATE - ct.sign_date) &lt;= 60 THEN '31-60天' "
          + "WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) &gt; 0 "
          + "AND (CURRENT_DATE - ct.sign_date) &lt;= 90 THEN '61-90天' "
          + "WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) &gt; 0 "
          + "AND (CURRENT_DATE - ct.sign_date) &gt; 90 THEN '90天以上' "
          + "ELSE '已收清' "
          + "END as aging_range, "
          + "ct.sign_date, "
          + "CASE "
          + "WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) = 0 THEN '已收清' "
          + "WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) = ct.total_amount THEN '未收款' "
          + "ELSE '部分收款' "
          + "END as receivable_status "
          + "FROM finance_contract ct "
          + "LEFT JOIN matter m ON ct.matter_id = m.id "
          + "LEFT JOIN crm_client c ON ct.client_id = c.id "
          + "LEFT JOIN finance_payment p ON ct.id = p.contract_id AND p.status = 'CONFIRMED' AND p.deleted = false "
          + "WHERE ct.deleted = false "
          + "AND (ct.total_amount - COALESCE(SUM(p.amount), 0)) &gt; 0 "
          + "<if test=\"clientId != null\">"
          + "AND c.id = #{clientId}"
          + "</if>"
          + "<if test=\"startDate != null\">"
          + "AND ct.sign_date &gt;= #{startDate}::date"
          + "</if>"
          + "<if test=\"endDate != null\">"
          + "AND ct.sign_date &lt;= #{endDate}::date"
          + "</if>"
          + "GROUP BY c.name, m.name, m.matter_no, ct.contract_no, ct.total_amount, ct.sign_date "
          + "ORDER BY aging_days DESC, ct.sign_date DESC "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryAgingAnalysisReportData(
      @Param("clientId") Long clientId,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /**
   * 查询项目利润分析报表数据（M4-054）.
   *
   * @param matterId 项目ID
   * @param clientId 客户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 项目利润分析报表数据
   */
  @Select(
      "<script>"
          + "SELECT "
          + "m.id as matter_id, "
          + "m.matter_no, "
          + "m.name as matter_name, "
          + "c.name as client_name, "
          + "u.real_name as lead_lawyer_name, "
          + "m.business_type, "
          + "m.status, "
          + "COALESCE(ct.total_amount, 0) as contract_amount, "
          + "COALESCE(received.received_amount, 0) as received_amount, "
          + "COALESCE(ct.total_amount, 0) - COALESCE(received.received_amount, 0) as receivable_amount, "
          + "COALESCE(allocated_costs.allocated_cost, 0) as allocated_cost, "
          + "COALESCE(split_costs.split_cost, 0) as split_cost, "
          + "COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0) as total_cost, "
          + "COALESCE(received.received_amount, 0) - "
          + "(COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) as profit, "
          + "CASE "
          + "WHEN COALESCE(received.received_amount, 0) &gt; 0 "
          + "THEN ((COALESCE(received.received_amount, 0) - "
          + "(COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0))) * 100.0 / "
          + "received.received_amount) "
          + "ELSE 0 "
          + "END as profit_rate_on_received, "
          + "CASE "
          + "WHEN COALESCE(ct.total_amount, 0) &gt; 0 "
          + "THEN ((COALESCE(received.received_amount, 0) - "
          + "(COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0))) * 100.0 / "
          + "ct.total_amount) "
          + "ELSE 0 "
          + "END as profit_rate_on_contract, "
          + "CASE "
          + "WHEN COALESCE(received.received_amount, 0) &gt; 0 "
          + "THEN ((COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) * 100.0 / "
          + "received.received_amount) "
          + "ELSE 0 "
          + "END as cost_rate, "
          + "ct.sign_date, "
          + "m.created_at as matter_created_at "
          + "FROM matter m "
          + "LEFT JOIN crm_client c ON m.client_id = c.id "
          + "LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "SUM(total_amount) as total_amount "
          + "FROM finance_contract "
          + "WHERE deleted = false "
          + "GROUP BY matter_id"
          + ") ct ON m.id = ct.matter_id "
          + "LEFT JOIN ("
          + "SELECT "
          + "p.matter_id, "
          + "SUM(p.amount) as received_amount "
          + "FROM finance_payment p "
          + "WHERE p.status = 'CONFIRMED' AND p.deleted = false "
          + "GROUP BY p.matter_id"
          + ") received ON m.id = received.matter_id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "SUM(amount) as allocated_cost "
          + "FROM finance_expense "
          + "WHERE status = 'PAID' "
          + "AND is_cost_allocation = true "
          + "AND deleted = false "
          + "GROUP BY matter_id"
          + ") allocated_costs ON m.id = allocated_costs.matter_id "
          + "LEFT JOIN ("
          + "SELECT "
          + "matter_id, "
          + "SUM(split_amount) as split_cost "
          + "FROM finance_cost_split "
          + "WHERE deleted = false "
          + "GROUP BY matter_id"
          + ") split_costs ON m.id = split_costs.matter_id "
          + "WHERE m.deleted = false "
          + "<if test=\"matterId != null\">"
          + "AND m.id = #{matterId}"
          + "</if>"
          + "<if test=\"clientId != null\">"
          + "AND m.client_id = #{clientId}"
          + "</if>"
          + "<if test=\"startDate != null\">"
          + "AND m.created_at &gt;= #{startDate}::timestamp"
          + "</if>"
          + "<if test=\"endDate != null\">"
          + "AND m.created_at &lt;= #{endDate}::timestamp"
          + "</if>"
          + "HAVING COALESCE(received.received_amount, 0) &gt; 0 "
          + "OR (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) &gt; 0 "
          + "ORDER BY profit DESC NULLS LAST "
          + "LIMIT 1000"
          + "</script>")
  List<Map<String, Object>> queryProfitAnalysisReportData(
      @Param("matterId") Long matterId,
      @Param("clientId") Long clientId,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /**
   * 统计我的项目数（我参与的项目）.
   *
   * @param userId 用户ID
   * @return 我的项目数
   */
  @Select(
      "SELECT COALESCE(COUNT(DISTINCT mp.matter_id), 0) FROM matter_participant mp "
          + "LEFT JOIN matter m ON mp.matter_id = m.id "
          + "WHERE mp.user_id = #{userId} AND mp.deleted = false AND m.deleted = false")
  Long countMyMatters(@Param("userId") Long userId);

  /**
   * 统计我的客户数（我负责的客户）.
   *
   * @param userId 用户ID
   * @return 我的客户数
   */
  @Select(
      "SELECT COALESCE(COUNT(*), 0) FROM crm_client "
          + "WHERE responsible_lawyer_id = #{userId} AND deleted = false")
  Long countMyClients(@Param("userId") Long userId);
}
