package com.lawfirm.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计查询 Mapper
 */
@Mapper
public interface StatisticsMapper {

    /**
     * 统计总收入（所有已确认的收款）
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM finance_payment WHERE status = 'CONFIRMED' AND deleted = false")
    BigDecimal sumTotalRevenue();

    /**
     * 统计本月收入
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM finance_payment " +
            "WHERE status = 'CONFIRMED' AND deleted = false " +
            "AND DATE_TRUNC('month', payment_date) = DATE_TRUNC('month', CURRENT_DATE)")
    BigDecimal sumMonthlyRevenue();

    /**
     * 统计本年收入
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM finance_payment " +
            "WHERE status = 'CONFIRMED' AND deleted = false " +
            "AND DATE_TRUNC('year', payment_date) = DATE_TRUNC('year', CURRENT_DATE)")
    BigDecimal sumYearlyRevenue();

    /**
     * 统计待收金额（收费记录中未收完的金额）
     */
    @Select("SELECT COALESCE(SUM(amount - COALESCE(paid_amount, 0)), 0) FROM finance_fee " +
            "WHERE deleted = false AND status != 'CANCELLED' " +
            "AND (paid_amount IS NULL OR paid_amount < amount)")
    BigDecimal sumPendingRevenue();

    /**
     * 统计各状态案件数
     */
    @Select("SELECT status, COUNT(*) as count FROM matter WHERE deleted = false GROUP BY status")
    List<Map<String, Object>> countMattersByStatus();

    /**
     * 统计各类型案件数
     */
    @Select("SELECT business_type, COUNT(*) as count FROM matter WHERE deleted = false GROUP BY business_type")
    List<Map<String, Object>> countMattersByType();

    /**
     * 统计进行中案件数
     */
    @Select("SELECT COUNT(*) FROM matter WHERE deleted = false AND status IN ('IN_PROGRESS', 'PENDING')")
    Long countActiveMatters();

    /**
     * 统计已完成案件数
     */
    @Select("SELECT COUNT(*) FROM matter WHERE deleted = false AND status = 'COMPLETED'")
    Long countCompletedMatters();

    /**
     * 统计各类型客户数
     */
    @Select("SELECT client_type, COUNT(*) as count FROM crm_client WHERE deleted = false GROUP BY client_type")
    List<Map<String, Object>> countClientsByType();

    /**
     * 统计正式客户数
     */
    @Select("SELECT COUNT(*) FROM crm_client WHERE deleted = false AND status = 'FORMAL'")
    Long countFormalClients();

    /**
     * 统计潜在客户数
     */
    @Select("SELECT COUNT(*) FROM crm_client WHERE deleted = false AND status = 'POTENTIAL'")
    Long countPotentialClients();

    /**
     * 统计本月新增客户数
     */
    @Select("SELECT COUNT(*) FROM crm_client " +
            "WHERE deleted = false " +
            "AND DATE_TRUNC('month', created_at) = DATE_TRUNC('month', CURRENT_DATE)")
    Long countNewClientsThisMonth();

    /**
     * 获取收入趋势（最近12个月）
     */
    @Select("SELECT " +
            "TO_CHAR(DATE_TRUNC('month', payment_date), 'YYYY-MM') as period, " +
            "COALESCE(SUM(amount), 0) as amount " +
            "FROM finance_payment " +
            "WHERE status = 'CONFIRMED' AND deleted = false " +
            "AND payment_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '11 months') " +
            "GROUP BY DATE_TRUNC('month', payment_date) " +
            "ORDER BY period")
    List<Map<String, Object>> getRevenueTrends();

    /**
     * 获取律师业绩排行（按收入）
     */
    @Select("SELECT " +
            "mp.user_id as lawyer_id, " +
            "u.real_name as lawyer_name, " +
            "COUNT(DISTINCT mp.matter_id) as matter_count, " +
            "COALESCE(SUM(p.amount), 0) as total_revenue, " +
            "COALESCE(SUM(ts.hours), 0) as total_hours, " +
            "CASE WHEN COUNT(DISTINCT mp.matter_id) > 0 " +
            "     THEN COALESCE(SUM(p.amount), 0) / COUNT(DISTINCT mp.matter_id) " +
            "     ELSE 0 END as avg_revenue " +
            "FROM matter_participant mp " +
            "LEFT JOIN sys_user u ON mp.user_id = u.id " +
            "LEFT JOIN matter m ON mp.matter_id = m.id " +
            "LEFT JOIN finance_fee f ON m.id = f.matter_id " +
            "LEFT JOIN finance_payment p ON f.id = p.fee_id AND p.status = 'CONFIRMED' AND p.deleted = false " +
            "LEFT JOIN timesheet ts ON mp.matter_id = ts.matter_id AND mp.user_id = ts.user_id AND ts.deleted = false " +
            "WHERE mp.deleted = false AND m.deleted = false " +
            "GROUP BY mp.user_id, u.real_name " +
            "ORDER BY total_revenue DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getLawyerPerformanceRanking(@Param("limit") Integer limit);

    /**
     * 查询收入报表数据
     */
    @Select("""
        <script>
        SELECT 
            TO_CHAR(p.payment_date, 'YYYY-MM-DD') as date,
            c.name as client_name,
            m.name as matter_name,
            p.amount,
            p.status
        FROM finance_payment p
        LEFT JOIN finance_fee f ON p.fee_id = f.id
        LEFT JOIN matter m ON f.matter_id = m.id
        LEFT JOIN crm_client c ON m.client_id = c.id
        WHERE p.deleted = false
        <if test="startDate != null">
            AND p.payment_date >= #{startDate}
        </if>
        <if test="endDate != null">
            AND p.payment_date &lt;= #{endDate}
        </if>
        <if test="clientId != null">
            AND c.id = #{clientId}
        </if>
        ORDER BY p.payment_date DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryRevenueReportData(@Param("startDate") String startDate,
                                                       @Param("endDate") String endDate,
                                                       @Param("clientId") Long clientId);

    /**
     * 查询案件报表数据
     */
    @Select("""
        <script>
        SELECT 
            m.matter_no,
            m.name as matter_name,
            c.name as client_name,
            m.business_type as matter_type,
            m.status,
            TO_CHAR(m.created_at, 'YYYY-MM-DD') as created_at
        FROM matter m
        LEFT JOIN crm_client c ON m.client_id = c.id
        WHERE m.deleted = false
        <if test="status != null and status != ''">
            AND m.status = #{status}
        </if>
        <if test="matterType != null and matterType != ''">
            AND m.business_type = #{matterType}
        </if>
        ORDER BY m.created_at DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryMatterReportData(@Param("status") String status,
                                                     @Param("matterType") String matterType);

    /**
     * 查询客户报表数据
     */
    @Select("""
        <script>
        SELECT 
            c.client_no,
            c.name as client_name,
            c.client_type,
            c.contact_person,
            c.contact_phone,
            c.status
        FROM crm_client c
        WHERE c.deleted = false
        <if test="clientType != null and clientType != ''">
            AND c.client_type = #{clientType}
        </if>
        <if test="status != null and status != ''">
            AND c.status = #{status}
        </if>
        ORDER BY c.created_at DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryClientReportData(@Param("clientType") String clientType,
                                                      @Param("status") String status);

    /**
     * 查询应收报表数据
     * 应收金额 = 合同金额 - 已收金额
     */
    @Select("""
        <script>
        SELECT 
            c.name as client_name,
            m.name as matter_name,
            m.matter_no,
            ct.contract_no,
            ct.total_amount as contract_amount,
            COALESCE(received.received_amount, 0) as received_amount,
            (ct.total_amount - COALESCE(received.received_amount, 0)) as receivable_amount,
            CASE 
                WHEN (ct.total_amount - COALESCE(received.received_amount, 0)) > 0 
                THEN CURRENT_DATE - ct.sign_date
                ELSE 0 
            END as aging_days,
            ct.sign_date,
            CASE 
                WHEN (ct.total_amount - COALESCE(received.received_amount, 0)) = 0 THEN '已收清'
                WHEN (ct.total_amount - COALESCE(received.received_amount, 0)) = ct.total_amount THEN '未收款'
                ELSE '部分收款'
            END as receivable_status
        FROM finance_contract ct
        LEFT JOIN matter m ON ct.matter_id = m.id
        LEFT JOIN crm_client c ON ct.client_id = c.id
        LEFT JOIN (
            SELECT contract_id, SUM(amount) as received_amount
            FROM finance_payment
            WHERE status = 'CONFIRMED' AND deleted = false
            GROUP BY contract_id
        ) received ON ct.id = received.contract_id
        WHERE ct.deleted = false
            AND (ct.total_amount - COALESCE(received.received_amount, 0)) > 0
        <if test="clientId != null">
            AND c.id = #{clientId}
        </if>
        <if test="startDate != null">
            AND ct.sign_date >= #{startDate}
        </if>
        <if test="endDate != null">
            AND ct.sign_date &lt;= #{endDate}
        </if>
        ORDER BY aging_days DESC, ct.sign_date DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryReceivableReportData(@Param("clientId") Long clientId,
                                                         @Param("startDate") String startDate,
                                                         @Param("endDate") String endDate);

    /**
     * 查询项目进度报表数据（M3-025）
     */
    @Select("""
        <script>
        SELECT 
            m.id as matter_id,
            m.matter_no,
            m.name as matter_name,
            c.name as client_name,
            m.business_type,
            m.status,
            m.fee_type,
            m.estimated_fee,
            m.actual_fee,
            TO_CHAR(m.created_at, 'YYYY-MM-DD') as created_date,
            TO_CHAR(m.expected_end_date, 'YYYY-MM-DD') as expected_end_date,
            TO_CHAR(m.actual_end_date, 'YYYY-MM-DD') as actual_end_date,
            u.real_name as lead_lawyer_name,
            COALESCE(task_stats.total_tasks, 0) as total_tasks,
            COALESCE(task_stats.completed_tasks, 0) as completed_tasks,
            COALESCE(task_stats.in_progress_tasks, 0) as in_progress_tasks,
            COALESCE(timesheet_stats.total_hours, 0) as total_hours,
            COALESCE(timesheet_stats.approved_hours, 0) as approved_hours,
            CASE 
                WHEN COALESCE(task_stats.total_tasks, 0) > 0 
                THEN ROUND(COALESCE(task_stats.completed_tasks, 0) * 100.0 / task_stats.total_tasks, 2)
                ELSE 0 
            END as task_completion_rate
        FROM matter m
        LEFT JOIN crm_client c ON m.client_id = c.id
        LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id
        LEFT JOIN (
            SELECT 
                matter_id,
                COUNT(*) as total_tasks,
                COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_tasks,
                COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') as in_progress_tasks
            FROM task
            WHERE deleted = false
            GROUP BY matter_id
        ) task_stats ON m.id = task_stats.matter_id
        LEFT JOIN (
            SELECT 
                matter_id,
                COALESCE(SUM(hours), 0) as total_hours,
                COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN hours ELSE 0 END), 0) as approved_hours
            FROM timesheet
            WHERE deleted = false
            GROUP BY matter_id
        ) timesheet_stats ON m.id = timesheet_stats.matter_id
        WHERE m.deleted = false
        <if test="status != null and status != ''">
            AND m.status = #{status}
        </if>
        <if test="matterType != null and matterType != ''">
            AND m.business_type = #{matterType}
        </if>
        <if test="leadLawyerId != null">
            AND m.lead_lawyer_id = #{leadLawyerId}
        </if>
        <if test="clientId != null">
            AND m.client_id = #{clientId}
        </if>
        ORDER BY m.created_at DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryMatterProgressReportData(@Param("status") String status,
                                                             @Param("matterType") String matterType,
                                                             @Param("leadLawyerId") Long leadLawyerId,
                                                             @Param("clientId") Long clientId);

    /**
     * 查询项目工时报表数据（M3-026）
     */
    @Select("""
        <script>
        SELECT 
            m.id as matter_id,
            m.matter_no,
            m.name as matter_name,
            c.name as client_name,
            u.real_name as lead_lawyer_name,
            TO_CHAR(ts.work_date, 'YYYY-MM-DD') as work_date,
            ts.work_type,
            ts.work_content,
            ts.hours,
            ts.billable,
            ts.status as timesheet_status,
            u2.real_name as worker_name
        FROM timesheet ts
        LEFT JOIN matter m ON ts.matter_id = m.id
        LEFT JOIN crm_client c ON m.client_id = c.id
        LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id
        LEFT JOIN sys_user u2 ON ts.user_id = u2.id
        WHERE ts.deleted = false AND m.deleted = false
        <if test="matterId != null">
            AND ts.matter_id = #{matterId}
        </if>
        <if test="userId != null">
            AND ts.user_id = #{userId}
        </if>
        <if test="startDate != null">
            AND ts.work_date >= #{startDate}
        </if>
        <if test="endDate != null">
            AND ts.work_date &lt;= #{endDate}
        </if>
        ORDER BY ts.work_date DESC, m.matter_no
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryMatterTimesheetReportData(@Param("matterId") Long matterId,
                                                              @Param("userId") Long userId,
                                                              @Param("startDate") String startDate,
                                                              @Param("endDate") String endDate);

    /**
     * 查询项目任务报表数据（M3-027）
     */
    @Select("""
        <script>
        SELECT 
            m.id as matter_id,
            m.matter_no,
            m.name as matter_name,
            c.name as client_name,
            t.task_no,
            t.title as task_title,
            t.priority,
            t.status as task_status,
            t.progress,
            TO_CHAR(t.due_date, 'YYYY-MM-DD') as due_date,
            TO_CHAR(t.completed_at, 'YYYY-MM-DD HH24:MI:SS') as completed_at,
            u.real_name as assignee_name,
            CASE 
                WHEN t.completed_at IS NOT NULL THEN '已完成'
                WHEN t.due_date IS NOT NULL AND t.due_date &lt; CURRENT_DATE AND t.status != 'COMPLETED' THEN '逾期'
                WHEN t.status = 'IN_PROGRESS' THEN '进行中'
                ELSE '待办'
            END as task_status_name
        FROM task t
        LEFT JOIN matter m ON t.matter_id = m.id
        LEFT JOIN crm_client c ON m.client_id = c.id
        LEFT JOIN sys_user u ON t.assignee_id = u.id
        WHERE t.deleted = false AND m.deleted = false
        <if test="matterId != null">
            AND t.matter_id = #{matterId}
        </if>
        <if test="assigneeId != null">
            AND t.assignee_id = #{assigneeId}
        </if>
        <if test="status != null and status != ''">
            AND t.status = #{status}
        </if>
        ORDER BY t.due_date ASC NULLS LAST, t.created_at DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryMatterTaskReportData(@Param("matterId") Long matterId,
                                                         @Param("assigneeId") Long assigneeId,
                                                         @Param("status") String status);

    /**
     * 查询项目阶段进度报表数据（M3-028）
     */
    @Select("""
        <script>
        SELECT 
            m.id as matter_id,
            m.matter_no,
            m.name as matter_name,
            c.name as client_name,
            m.business_type,
            m.status,
            u.real_name as lead_lawyer_name,
            d.name as department_name,
            TO_CHAR(m.created_at, 'YYYY-MM-DD') as created_date,
            TO_CHAR(m.expected_end_date, 'YYYY-MM-DD') as expected_end_date,
            TO_CHAR(m.actual_end_date, 'YYYY-MM-DD') as actual_end_date,
            CASE 
                WHEN m.actual_end_date IS NOT NULL THEN '已完成'
                WHEN m.expected_end_date IS NOT NULL AND m.expected_end_date &lt; CURRENT_DATE THEN '逾期'
                WHEN m.status = 'ACTIVE' THEN '进行中'
                ELSE m.status
            END as progress_status,
            COALESCE(task_stats.total_tasks, 0) as total_tasks,
            COALESCE(task_stats.completed_tasks, 0) as completed_tasks,
            CASE 
                WHEN COALESCE(task_stats.total_tasks, 0) > 0 
                THEN ROUND(COALESCE(task_stats.completed_tasks, 0) * 100.0 / task_stats.total_tasks, 2)
                ELSE 0 
            END as completion_rate
        FROM matter m
        LEFT JOIN crm_client c ON m.client_id = c.id
        LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id
        LEFT JOIN sys_department d ON m.department_id = d.id
        LEFT JOIN (
            SELECT 
                matter_id,
                COUNT(*) as total_tasks,
                COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_tasks
            FROM task
            WHERE deleted = false
            GROUP BY matter_id
        ) task_stats ON m.id = task_stats.matter_id
        WHERE m.deleted = false
        <if test="status != null and status != ''">
            AND m.status = #{status}
        </if>
        <if test="matterType != null and matterType != ''">
            AND m.business_type = #{matterType}
        </if>
        <if test="departmentId != null">
            AND m.department_id = #{departmentId}
        </if>
        ORDER BY m.created_at DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryMatterStageReportData(@Param("status") String status,
                                                         @Param("matterType") String matterType,
                                                         @Param("departmentId") Long departmentId);

    /**
     * 查询项目趋势分析报表数据（M3-029）
     */
    @Select("""
        <script>
        SELECT 
            TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') as period,
            COUNT(*) as new_matters_count,
            COUNT(*) FILTER (WHERE status = 'CLOSED') as closed_matters_count,
            COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_matters_count,
            AVG(CASE 
                WHEN actual_end_date IS NOT NULL AND created_at IS NOT NULL 
                THEN EXTRACT(EPOCH FROM (actual_end_date - created_at::date)) / 86400
                ELSE NULL 
            END) as avg_duration_days
        FROM matter
        WHERE deleted = false
        <if test="startDate != null">
            AND created_at >= #{startDate}::timestamp
        </if>
        <if test="endDate != null">
            AND created_at &lt;= #{endDate}::timestamp
        </if>
        GROUP BY DATE_TRUNC('month', created_at)
        ORDER BY period DESC
        LIMIT 24
        </script>
        """)
    List<Map<String, Object>> queryMatterTrendReportData(@Param("startDate") String startDate,
                                                          @Param("endDate") String endDate);

    /**
     * 查询项目成本分析报表数据（M4-044）
     */
    @Select("""
        <script>
        SELECT 
            m.id as matter_id,
            m.matter_no,
            m.name as matter_name,
            c.name as client_name,
            u.real_name as lead_lawyer_name,
            -- 归集成本（直接归集到项目的费用）
            COALESCE(allocated_costs.allocated_cost, 0) as allocated_cost,
            -- 分摊成本（从公共费用分摊过来的）
            COALESCE(split_costs.split_cost, 0) as split_cost,
            -- 总成本
            COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0) as total_cost,
            -- 合同金额
            COALESCE(ct.total_amount, 0) as contract_amount,
            -- 已收款
            COALESCE(received.received_amount, 0) as received_amount,
            -- 利润 = 已收款 - 总成本
            COALESCE(received.received_amount, 0) - 
            (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) as profit,
            -- 利润率
            CASE 
                WHEN COALESCE(received.received_amount, 0) > 0 
                THEN ((COALESCE(received.received_amount, 0) - 
                       (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0))) * 100.0 / 
                      received.received_amount)
                ELSE 0 
            END as profit_rate
        FROM matter m
        LEFT JOIN crm_client c ON m.client_id = c.id
        LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id
        LEFT JOIN (
            SELECT 
                matter_id,
                SUM(amount) as allocated_cost
            FROM finance_expense
            WHERE status = 'PAID' 
              AND is_cost_allocation = true 
              AND deleted = false
            <if test="startDate != null">
              AND expense_date >= #{startDate}::date
            </if>
            <if test="endDate != null">
              AND expense_date &lt;= #{endDate}::date
            </if>
            GROUP BY matter_id
        ) allocated_costs ON m.id = allocated_costs.matter_id
        LEFT JOIN (
            SELECT 
                matter_id,
                SUM(split_amount) as split_cost
            FROM finance_cost_split
            WHERE deleted = false
            <if test="startDate != null">
              AND split_date >= #{startDate}::date
            </if>
            <if test="endDate != null">
              AND split_date &lt;= #{endDate}::date
            </if>
            GROUP BY matter_id
        ) split_costs ON m.id = split_costs.matter_id
        LEFT JOIN (
            SELECT 
                matter_id,
                SUM(total_amount) as total_amount
            FROM finance_contract
            WHERE deleted = false
            GROUP BY matter_id
        ) ct ON m.id = ct.matter_id
        LEFT JOIN (
            SELECT 
                p.matter_id,
                SUM(p.amount) as received_amount
            FROM finance_payment p
            WHERE p.status = 'CONFIRMED' AND p.deleted = false
            GROUP BY p.matter_id
        ) received ON m.id = received.matter_id
        WHERE m.deleted = false
        <if test="matterId != null">
            AND m.id = #{matterId}
        </if>
        HAVING (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) > 0
           OR COALESCE(received.received_amount, 0) > 0
        ORDER BY total_cost DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryCostAnalysisReportData(@Param("matterId") Long matterId,
                                                            @Param("startDate") String startDate,
                                                            @Param("endDate") String endDate);

    /**
     * 查询应收账款账龄分析报表数据（M4-053）
     */
    @Select("""
        <script>
        SELECT 
            c.name as client_name,
            m.name as matter_name,
            m.matter_no,
            ct.contract_no,
            ct.total_amount as contract_amount,
            COALESCE(SUM(p.amount), 0) as received_amount,
            (ct.total_amount - COALESCE(SUM(p.amount), 0)) as receivable_amount,
            CASE
                WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) > 0
                THEN CURRENT_DATE - ct.sign_date
                ELSE 0
            END as aging_days,
            CASE
                WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) = 0 THEN '已收清'
                WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) > 0 
                     AND (CURRENT_DATE - ct.sign_date) &lt;= 30 THEN '0-30天'
                WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) > 0 
                     AND (CURRENT_DATE - ct.sign_date) &lt;= 60 THEN '31-60天'
                WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) > 0 
                     AND (CURRENT_DATE - ct.sign_date) &lt;= 90 THEN '61-90天'
                WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) > 0 
                     AND (CURRENT_DATE - ct.sign_date) > 90 THEN '90天以上'
                ELSE '已收清'
            END as aging_range,
            ct.sign_date,
            CASE
                WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) = 0 THEN '已收清'
                WHEN (ct.total_amount - COALESCE(SUM(p.amount), 0)) = ct.total_amount THEN '未收款'
                ELSE '部分收款'
            END as receivable_status
        FROM finance_contract ct
        LEFT JOIN matter m ON ct.matter_id = m.id
        LEFT JOIN crm_client c ON ct.client_id = c.id
        LEFT JOIN finance_payment p ON ct.id = p.contract_id AND p.status = 'CONFIRMED' AND p.deleted = false
        WHERE ct.deleted = false
          AND (ct.total_amount - COALESCE(SUM(p.amount), 0)) > 0
        <if test="clientId != null">
            AND c.id = #{clientId}
        </if>
        <if test="startDate != null">
            AND ct.sign_date >= #{startDate}::date
        </if>
        <if test="endDate != null">
            AND ct.sign_date &lt;= #{endDate}::date
        </if>
        GROUP BY c.name, m.name, m.matter_no, ct.contract_no, ct.total_amount, ct.sign_date
        ORDER BY aging_days DESC, ct.sign_date DESC
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryAgingAnalysisReportData(@Param("clientId") Long clientId,
                                                             @Param("startDate") String startDate,
                                                             @Param("endDate") String endDate);

    /**
     * 查询项目利润分析报表数据（M4-054）
     */
    @Select("""
        <script>
        SELECT 
            m.id as matter_id,
            m.matter_no,
            m.name as matter_name,
            c.name as client_name,
            u.real_name as lead_lawyer_name,
            m.business_type,
            m.status,
            -- 收入相关
            COALESCE(ct.total_amount, 0) as contract_amount,
            COALESCE(received.received_amount, 0) as received_amount,
            COALESCE(ct.total_amount, 0) - COALESCE(received.received_amount, 0) as receivable_amount,
            -- 成本相关
            COALESCE(allocated_costs.allocated_cost, 0) as allocated_cost,
            COALESCE(split_costs.split_cost, 0) as split_cost,
            COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0) as total_cost,
            -- 利润相关
            COALESCE(received.received_amount, 0) - 
            (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) as profit,
            -- 利润率（基于已收款）
            CASE 
                WHEN COALESCE(received.received_amount, 0) > 0 
                THEN ((COALESCE(received.received_amount, 0) - 
                       (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0))) * 100.0 / 
                      received.received_amount)
                ELSE 0 
            END as profit_rate_on_received,
            -- 利润率（基于合同金额）
            CASE 
                WHEN COALESCE(ct.total_amount, 0) > 0 
                THEN ((COALESCE(received.received_amount, 0) - 
                       (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0))) * 100.0 / 
                      ct.total_amount)
                ELSE 0 
            END as profit_rate_on_contract,
            -- 成本率
            CASE 
                WHEN COALESCE(received.received_amount, 0) > 0 
                THEN ((COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) * 100.0 / 
                      received.received_amount)
                ELSE 0 
            END as cost_rate,
            ct.sign_date,
            m.created_at as matter_created_at
        FROM matter m
        LEFT JOIN crm_client c ON m.client_id = c.id
        LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id
        LEFT JOIN (
            SELECT 
                matter_id,
                SUM(total_amount) as total_amount
            FROM finance_contract
            WHERE deleted = false
            GROUP BY matter_id
        ) ct ON m.id = ct.matter_id
        LEFT JOIN (
            SELECT 
                p.matter_id,
                SUM(p.amount) as received_amount
            FROM finance_payment p
            WHERE p.status = 'CONFIRMED' AND p.deleted = false
            GROUP BY p.matter_id
        ) received ON m.id = received.matter_id
        LEFT JOIN (
            SELECT 
                matter_id,
                SUM(amount) as allocated_cost
            FROM finance_expense
            WHERE status = 'PAID' 
              AND is_cost_allocation = true 
              AND deleted = false
            GROUP BY matter_id
        ) allocated_costs ON m.id = allocated_costs.matter_id
        LEFT JOIN (
            SELECT 
                matter_id,
                SUM(split_amount) as split_cost
            FROM finance_cost_split
            WHERE deleted = false
            GROUP BY matter_id
        ) split_costs ON m.id = split_costs.matter_id
        WHERE m.deleted = false
        <if test="matterId != null">
            AND m.id = #{matterId}
        </if>
        <if test="clientId != null">
            AND m.client_id = #{clientId}
        </if>
        <if test="startDate != null">
            AND m.created_at >= #{startDate}::timestamp
        </if>
        <if test="endDate != null">
            AND m.created_at &lt;= #{endDate}::timestamp
        </if>
        HAVING COALESCE(received.received_amount, 0) > 0
           OR (COALESCE(allocated_costs.allocated_cost, 0) + COALESCE(split_costs.split_cost, 0)) > 0
        ORDER BY profit DESC NULLS LAST
        LIMIT 1000
        </script>
        """)
    List<Map<String, Object>> queryProfitAnalysisReportData(@Param("matterId") Long matterId,
                                                              @Param("clientId") Long clientId,
                                                              @Param("startDate") String startDate,
                                                              @Param("endDate") String endDate);

    /**
     * 统计我的项目数（我参与的项目）
     */
    @Select("SELECT COUNT(DISTINCT mp.matter_id) FROM matter_participant mp " +
            "LEFT JOIN matter m ON mp.matter_id = m.id " +
            "WHERE mp.user_id = #{userId} AND mp.deleted = false AND m.deleted = false")
    Long countMyMatters(@Param("userId") Long userId);

    /**
     * 统计我的客户数（我负责的客户）
     */
    @Select("SELECT COUNT(*) FROM crm_client " +
            "WHERE responsible_lawyer_id = #{userId} AND deleted = false")
    Long countMyClients(@Param("userId") Long userId);
}

