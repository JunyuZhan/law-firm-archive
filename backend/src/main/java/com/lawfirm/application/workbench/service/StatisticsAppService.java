package com.lawfirm.application.workbench.service;

import com.lawfirm.application.workbench.dto.StatisticsDTO;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.DepartmentMapper;
import com.lawfirm.infrastructure.persistence.mapper.StatisticsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * 统计应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsAppService {

    private final FeeRepository feeRepository;
    private final PaymentRepository paymentRepository;
    private final MatterRepository matterRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final StatisticsMapper statisticsMapper;
    private final DepartmentMapper departmentMapper;
    private final com.lawfirm.domain.matter.repository.TimesheetRepository timesheetRepository;
    private final com.lawfirm.domain.matter.repository.TaskRepository taskRepository;
    private final com.lawfirm.domain.finance.repository.CommissionRepository commissionRepository;
    private final com.lawfirm.domain.matter.repository.MatterParticipantRepository matterParticipantRepository;
    // 行政相关
    private final com.lawfirm.domain.admin.repository.LetterApplicationRepository letterApplicationRepository;
    private final com.lawfirm.domain.document.repository.SealApplicationRepository sealApplicationRepository;
    private final com.lawfirm.domain.admin.repository.LeaveApplicationRepository leaveApplicationRepository;
    private final com.lawfirm.domain.admin.repository.AssetRecordRepository assetRecordRepository;
    // 财务相关
    private final com.lawfirm.domain.finance.repository.ExpenseRepository expenseRepository;
    private final com.lawfirm.domain.finance.repository.InvoiceRepository invoiceRepository;
    
    // ✅ 修复问题554: 使用ThreadLocal缓存（同一请求内复用）
    private static final ThreadLocal<Map<String, List<Long>>> MATTER_IDS_CACHE = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, List<Long>>> CLIENT_IDS_CACHE = new ThreadLocal<>();
    private static final ThreadLocal<Map<Long, List<Long>>> DEPT_CHILDREN_CACHE = new ThreadLocal<>();
    
    /**
     * 清理缓存（请求结束后调用，可在Filter或Interceptor中调用）
     */
    public static void clearCache() {
        MATTER_IDS_CACHE.remove();
        CLIENT_IDS_CACHE.remove();
        DEPT_CHILDREN_CACHE.remove();
    }
    
    /**
     * 获取部门及所有下级部门ID列表
     * 使用递归CTE一次性查询，并添加缓存
     */
    private List<Long> getAllDepartmentIds(Long deptId) {
        if (deptId == null) {
            return new ArrayList<>();
        }
        
        // 检查缓存
        Map<Long, List<Long>> cache = DEPT_CHILDREN_CACHE.get();
        if (cache != null && cache.containsKey(deptId)) {
            return new ArrayList<>(cache.get(deptId));
        }
        
        // 使用递归CTE查询所有后代部门
        List<Long> result = new ArrayList<>();
        result.add(deptId); // 包含自身
        
        try {
            List<Long> descendantIds = departmentMapper.selectAllDescendantDeptIds(deptId);
            if (descendantIds != null) {
                result.addAll(descendantIds);
            }
        } catch (Exception e) {
            log.warn("查询子部门失败: deptId={}, error={}", deptId, e.getMessage());
        }
        
        // 放入缓存
        if (cache == null) {
            cache = new HashMap<>();
            DEPT_CHILDREN_CACHE.set(cache);
        }
        cache.put(deptId, result);
        
        return result;
    }

    /**
     * 获取收入统计（根据权限过滤）
     */
    public StatisticsDTO.RevenueStats getRevenueStats() {
        StatisticsDTO.RevenueStats stats = new StatisticsDTO.RevenueStats();

        // 根据用户权限过滤数据
        String dataScope = com.lawfirm.common.util.SecurityUtils.getDataScope();
        Long currentUserId = com.lawfirm.common.util.SecurityUtils.getUserId();
        Long deptId = com.lawfirm.common.util.SecurityUtils.getDepartmentId();

        // 获取可访问的项目ID列表（用于过滤收入数据）
        List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

        // 查询总收入
        BigDecimal totalRevenue = statisticsMapper.sumTotalRevenue(accessibleMatterIds);
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        // 查询本月收入
        BigDecimal monthlyRevenue = statisticsMapper.sumMonthlyRevenue(accessibleMatterIds);
        stats.setMonthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);

        // 查询本年收入
        BigDecimal yearlyRevenue = statisticsMapper.sumYearlyRevenue(accessibleMatterIds);
        stats.setYearlyRevenue(yearlyRevenue != null ? yearlyRevenue : BigDecimal.ZERO);

        // 查询待收金额
        BigDecimal pendingRevenue = statisticsMapper.sumPendingRevenue(accessibleMatterIds);
        stats.setPendingRevenue(pendingRevenue != null ? pendingRevenue : BigDecimal.ZERO);

        // 计算增长率（与上月对比）
        BigDecimal growthRate = calculateGrowthRate(monthlyRevenue);
        stats.setGrowthRate(growthRate);

        // 查询收入趋势
        List<Map<String, Object>> trendData = statisticsMapper.getRevenueTrends(accessibleMatterIds);
        List<StatisticsDTO.RevenueTrend> trends = new java.util.ArrayList<>();
        if (trendData != null) {
            trends = trendData.stream()
                    .filter(item -> item != null)
                    .map(item -> {
                        StatisticsDTO.RevenueTrend trend = new StatisticsDTO.RevenueTrend();
                        trend.setPeriod((String) item.get("period"));
                        Object amountObj = item.get("amount");
                        if (amountObj instanceof BigDecimal) {
                            trend.setAmount((BigDecimal) amountObj);
                        } else if (amountObj != null) {
                            trend.setAmount(new BigDecimal(amountObj.toString()));
                        } else {
                            trend.setAmount(BigDecimal.ZERO);
                        }
                        return trend;
                    })
                    .collect(Collectors.toList());
        }
        stats.setTrends(trends);

        log.info("获取收入统计: total={}, monthly={}, yearly={}, dataScope={}", totalRevenue, monthlyRevenue, yearlyRevenue, dataScope);
        return stats;
    }

    /**
     * 获取项目统计（根据权限过滤）
     */
    public StatisticsDTO.MatterStats getMatterStats() {
        StatisticsDTO.MatterStats stats = new StatisticsDTO.MatterStats();
        
        // 根据用户权限过滤数据
        String dataScope = com.lawfirm.common.util.SecurityUtils.getDataScope();
        Long currentUserId = com.lawfirm.common.util.SecurityUtils.getUserId();
        Long deptId = com.lawfirm.common.util.SecurityUtils.getDepartmentId();

        try {
            // 总案件数 - 根据权限过滤
            long totalMatters;
            if ("ALL".equals(dataScope)) {
                // ALL权限：查看所有案件
                totalMatters = matterRepository.lambdaQuery()
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                        .count();
            } else if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
                // 部门及下级部门：使用递归CTE查询所有下级部门
                List<Long> allDeptIds = getAllDepartmentIds(deptId);
                totalMatters = matterRepository.lambdaQuery()
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                        .in(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, allDeptIds)
                        .count();
            } else if ("DEPT".equals(dataScope) && deptId != null) {
                // 本部门
                totalMatters = matterRepository.lambdaQuery()
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptId)
                        .count();
            } else {
                // SELF：只查看自己负责的项目
                totalMatters = matterRepository.lambdaQuery()
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                        .eq(com.lawfirm.domain.matter.entity.Matter::getLeadLawyerId, currentUserId)
                        .count();
            }
            stats.setTotalMatters(totalMatters);

            // 获取可访问的项目ID列表
            List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

            // 进行中案件数 - 使用Mapper查询（应用权限过滤）
            Long activeMatters = statisticsMapper.countActiveMatters(accessibleMatterIds);
            stats.setActiveMatters(activeMatters != null ? activeMatters : 0L);

            // 已完成案件数
            Long completedMatters = statisticsMapper.countCompletedMatters(accessibleMatterIds);
            stats.setCompletedMatters(completedMatters != null ? completedMatters : 0L);

            // 各状态案件数
            List<Map<String, Object>> statusCountList = statisticsMapper.countMattersByStatus(accessibleMatterIds);
            Map<String, Long> statusCount = new HashMap<>();
            if (statusCountList != null) {
                for (Map<String, Object> item : statusCountList) {
                    if (item == null) continue;
                    Object statusObj = item.get("status");
                    Object countObj = item.get("count");
                    if (statusObj != null && countObj != null) {
                        statusCount.put(statusObj.toString(), ((Number) countObj).longValue());
                    }
                }
            }
            stats.setStatusCount(statusCount);

            // 各类型案件数
            List<Map<String, Object>> typeCountList = statisticsMapper.countMattersByType(accessibleMatterIds);
            Map<String, Long> typeCount = new HashMap<>();
            if (typeCountList != null) {
                for (Map<String, Object> item : typeCountList) {
                    if (item == null) continue;
                    Object typeObj = item.get("business_type");
                    Object countObj = item.get("count");
                    if (typeObj != null && countObj != null) {
                        typeCount.put(typeObj.toString(), ((Number) countObj).longValue());
                    }
                }
            }
            stats.setTypeCount(typeCount);

            log.info("获取项目统计: total={}, active={}, completed={}", totalMatters, activeMatters, completedMatters);
        } catch (Exception e) {
            log.error("获取项目统计失败", e);
            // 返回空统计，避免500错误
            stats.setTotalMatters(0L);
            stats.setActiveMatters(0L);
            stats.setCompletedMatters(0L);
            stats.setStatusCount(new HashMap<>());
            stats.setTypeCount(new HashMap<>());
        }
        
        return stats;
    }

    /**
     * 获取客户统计（根据权限过滤）
     */
    public StatisticsDTO.ClientStats getClientStats() {
        StatisticsDTO.ClientStats stats = new StatisticsDTO.ClientStats();

        // 根据用户权限过滤数据
        String dataScope = com.lawfirm.common.util.SecurityUtils.getDataScope();
        Long currentUserId = com.lawfirm.common.util.SecurityUtils.getUserId();
        Long deptId = com.lawfirm.common.util.SecurityUtils.getDepartmentId();

        // 获取可访问的客户ID列表
        List<Long> accessibleClientIds = getAccessibleClientIds(dataScope, currentUserId, deptId);

        // 总客户数
        long totalClients;
        if (accessibleClientIds == null) {
            totalClients = clientRepository.count();
        } else {
            totalClients = clientRepository.lambdaQuery()
                    .in(com.lawfirm.domain.client.entity.Client::getId, accessibleClientIds)
                    .count();
        }
        stats.setTotalClients(totalClients);

        // 正式客户数
        Long formalClients = statisticsMapper.countFormalClients(accessibleClientIds);
        stats.setFormalClients(formalClients != null ? formalClients : 0L);

        // 潜在客户数
        Long potentialClients = statisticsMapper.countPotentialClients(accessibleClientIds);
        stats.setPotentialClients(potentialClients != null ? potentialClients : 0L);

        // 本月新增客户数
        Long newClientsThisMonth = statisticsMapper.countNewClientsThisMonth(accessibleClientIds);
        stats.setNewClientsThisMonth(newClientsThisMonth != null ? newClientsThisMonth : 0L);

        // 各类型客户数
        List<Map<String, Object>> typeCountList = statisticsMapper.countClientsByType(accessibleClientIds);
        Map<String, Long> typeCount = new HashMap<>();
        if (typeCountList != null) {
            for (Map<String, Object> item : typeCountList) {
                if (item == null) continue;
                Object typeObj = item.get("client_type");
                Object countObj = item.get("count");
                if (typeObj != null && countObj != null) {
                    typeCount.put(typeObj.toString(), ((Number) countObj).longValue());
                }
            }
        }
        stats.setTypeCount(typeCount);

        log.info("获取客户统计: total={}, formal={}, potential={}, newThisMonth={}, dataScope={}", 
                totalClients, formalClients, potentialClients, newClientsThisMonth, dataScope);
        return stats;
    }

    /**
     * 获取律师业绩排行（根据权限过滤）
     * ✅ 修复问题553: 使用批量查询避免N+1查询
     */
    public List<StatisticsDTO.LawyerPerformance> getLawyerPerformanceRanking(Integer limit) {
        // 根据用户权限过滤数据
        String dataScope = com.lawfirm.common.util.SecurityUtils.getDataScope();
        Long currentUserId = com.lawfirm.common.util.SecurityUtils.getUserId();
        Long deptId = com.lawfirm.common.util.SecurityUtils.getDepartmentId();

        // 获取可访问的项目ID列表
        List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

        List<Map<String, Object>> rankings = statisticsMapper.getLawyerPerformanceRanking(limit, accessibleMatterIds);

        if (rankings.isEmpty()) {
            return new ArrayList<>();
        }

        // ✅ 批量加载所有律师的提成数据（避免N+1查询）
        Set<Long> lawyerIds = rankings.stream()
                .filter(Objects::nonNull)  // 先过滤掉null的item
                .map(item -> item.get("lawyer_id"))
                .filter(Objects::nonNull)
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toSet());

        Map<Long, BigDecimal> commissionMap = lawyerIds.isEmpty() ? Collections.emptyMap() :
                commissionRepository.sumCommissionByUserIds(new ArrayList<>(lawyerIds));

        // 转换DTO（从Map获取提成，避免查询）
        List<StatisticsDTO.LawyerPerformance> result = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> item : rankings) {
            if (item == null) continue;
            Object lawyerIdObj = item.get("lawyer_id");
            if (lawyerIdObj == null) continue;
            Long lawyerId = ((Number) lawyerIdObj).longValue();
            StatisticsDTO.LawyerPerformance performance = new StatisticsDTO.LawyerPerformance();
            performance.setLawyerId(lawyerId);
            performance.setLawyerName((String) item.get("lawyer_name"));
            Object matterCountObj = item.get("matter_count");
            performance.setMatterCount(matterCountObj != null ? ((Number) matterCountObj).longValue() : 0L);
            Object revenueObj = item.get("total_revenue");
            performance.setRevenue(revenueObj != null ? new BigDecimal(revenueObj.toString()) : BigDecimal.ZERO);

            // ✅ 从Map获取提成，避免N+1查询
            BigDecimal commission = commissionMap.get(lawyerId);
            performance.setCommission(commission != null ? commission : BigDecimal.ZERO);

            // 从工时表统计工时（已审批的工时）
            BigDecimal totalHours = (BigDecimal) item.get("total_hours");
            performance.setHours(totalHours != null ? totalHours.doubleValue() : 0.0);

            performance.setRank(rank++);
            result.add(performance);
        }

        log.info("获取律师业绩排行: count={}, dataScope={}", result.size(), dataScope);
        return result;
    }

    /**
     * 获取工作台统计数据（用于仪表盘）
     * 根据用户角色返回不同的统计数据：
     * - 律师/团队负责人/主任：项目数、客户数、工时、任务数
     * - 财务：待确认收款、待开票、待审批报销、本月已收金额
     * - 行政：待处理出函、待处理用印、待审批请假、待处理资产领用
     */
    public WorkbenchStatsDTO getWorkbenchStats() {
        Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
        Set<String> roles = com.lawfirm.common.util.SecurityUtils.getRoles();
        
        WorkbenchStatsDTO stats = new WorkbenchStatsDTO();
        
        // 待办任务数（所有角色通用）
        int taskCountInt = taskRepository.countPendingByAssigneeId(userId);
        stats.setTaskCount(Long.valueOf(taskCountInt));
        
        // 根据角色类型返回不同的统计数据
        if (roles.contains("FINANCE")) {
            // 财务角色
            stats.setRoleType("FINANCE");
            loadFinanceStats(stats);
        } else if (roles.contains("ADMIN_STAFF")) {
            // 行政角色
            stats.setRoleType("ADMIN_STAFF");
            loadAdminStats(stats);
        } else {
            // 律师/团队负责人/主任等业务角色
            stats.setRoleType("LAWYER");
            loadLawyerStats(stats, userId);
        }
        
        log.info("获取工作台统计: userId={}, roleType={}", userId, stats.getRoleType());
        
        return stats;
    }
    
    /**
     * 加载律师相关统计数据
     */
    private void loadLawyerStats(WorkbenchStatsDTO stats, Long userId) {
        // 我的项目数（我参与的项目）
        var participantList = matterParticipantRepository.lambdaQuery()
                .select(com.lawfirm.domain.matter.entity.MatterParticipant::getMatterId)
                .eq(com.lawfirm.domain.matter.entity.MatterParticipant::getUserId, userId)
                .list();
        
        java.util.List<Long> matterIds = participantList.stream()
                .map(com.lawfirm.domain.matter.entity.MatterParticipant::getMatterId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        
        long matterCountLong = 0;
        if (!matterIds.isEmpty()) {
            matterCountLong = matterRepository.lambdaQuery()
                    .in(com.lawfirm.domain.matter.entity.Matter::getId, matterIds)
                    .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                    .count();
        }
        stats.setMatterCount(matterCountLong);
        
        // 我的客户数
        long clientCountLong = clientRepository.lambdaQuery()
                .eq(com.lawfirm.domain.client.entity.Client::getResponsibleLawyerId, userId)
                .count();
        stats.setClientCount(clientCountLong);
        
        // 本月工时
        java.time.LocalDate now = java.time.LocalDate.now();
        BigDecimal monthlyHours = timesheetRepository.sumHoursByUserAndMonth(userId, now.getYear(), now.getMonthValue());
        stats.setTimesheetHours(monthlyHours != null ? monthlyHours.doubleValue() : 0.0);
    }
    
    /**
     * 加载财务相关统计数据
     */
    private void loadFinanceStats(WorkbenchStatsDTO stats) {
        try {
            // 待确认收款数
            long pendingPaymentCount = paymentRepository.lambdaQuery()
                    .eq(com.lawfirm.domain.finance.entity.Payment::getStatus, "PENDING")
                    .eq(com.lawfirm.domain.finance.entity.Payment::getDeleted, false)
                    .count();
            stats.setPendingPaymentCount(pendingPaymentCount);
            
            // 待开票数
            long pendingInvoiceCount = invoiceRepository.lambdaQuery()
                    .eq(com.lawfirm.domain.finance.entity.Invoice::getStatus, "PENDING")
                    .eq(com.lawfirm.domain.finance.entity.Invoice::getDeleted, false)
                    .count();
            stats.setPendingInvoiceCount(pendingInvoiceCount);
            
            // 待审批报销数
            long pendingExpenseCount = expenseRepository.lambdaQuery()
                    .eq(com.lawfirm.domain.finance.entity.Expense::getStatus, "PENDING")
                    .eq(com.lawfirm.domain.finance.entity.Expense::getDeleted, false)
                    .count();
            stats.setPendingExpenseCount(pendingExpenseCount);
            
            // 本月已收金额 - 使用简化查询
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate startOfMonth = now.withDayOfMonth(1);
            var confirmedPayments = paymentRepository.lambdaQuery()
                    .eq(com.lawfirm.domain.finance.entity.Payment::getStatus, "CONFIRMED")
                    .eq(com.lawfirm.domain.finance.entity.Payment::getDeleted, false)
                    .ge(com.lawfirm.domain.finance.entity.Payment::getPaymentDate, startOfMonth)
                    .le(com.lawfirm.domain.finance.entity.Payment::getPaymentDate, now)
                    .list();
            BigDecimal monthlyReceived = confirmedPayments.stream()
                    .map(com.lawfirm.domain.finance.entity.Payment::getAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.setMonthlyReceivedAmount(monthlyReceived);
        } catch (Exception e) {
            log.error("加载财务统计数据失败", e);
            stats.setPendingPaymentCount(0L);
            stats.setPendingInvoiceCount(0L);
            stats.setPendingExpenseCount(0L);
            stats.setMonthlyReceivedAmount(BigDecimal.ZERO);
        }
    }
    
    /**
     * 加载行政相关统计数据
     */
    private void loadAdminStats(WorkbenchStatsDTO stats) {
        try {
            // 待处理出函数（待审批 + 已审批待打印的出函申请）
            long pendingLetterCount = letterApplicationRepository.lambdaQuery()
                    .in(com.lawfirm.domain.admin.entity.LetterApplication::getStatus, "PENDING", "APPROVED")
                    .eq(com.lawfirm.domain.admin.entity.LetterApplication::getDeleted, false)
                    .count();
            stats.setPendingLetterCount(pendingLetterCount);
            
            // 待处理用印数
            long pendingSealCount = sealApplicationRepository.lambdaQuery()
                    .in(com.lawfirm.domain.document.entity.SealApplication::getStatus, "PENDING", "APPROVED")
                    .eq(com.lawfirm.domain.document.entity.SealApplication::getDeleted, false)
                    .count();
            stats.setPendingSealCount(pendingSealCount);
            
            // 待审批请假数
            long pendingLeaveCount = leaveApplicationRepository.lambdaQuery()
                    .eq(com.lawfirm.domain.admin.entity.LeaveApplication::getStatus, "PENDING")
                    .eq(com.lawfirm.domain.admin.entity.LeaveApplication::getDeleted, false)
                    .count();
            stats.setPendingLeaveCount(pendingLeaveCount);
            
            // 待处理资产领用数（领用申请待审批）
            long pendingAssetCount = assetRecordRepository.lambdaQuery()
                    .eq(com.lawfirm.domain.admin.entity.AssetRecord::getRecordType, "RECEIVE")
                    .eq(com.lawfirm.domain.admin.entity.AssetRecord::getApprovalStatus, "PENDING")
                    .eq(com.lawfirm.domain.admin.entity.AssetRecord::getDeleted, false)
                    .count();
            stats.setPendingAssetCount(pendingAssetCount);
        } catch (Exception e) {
            log.error("加载行政统计数据失败", e);
            stats.setPendingLetterCount(0L);
            stats.setPendingSealCount(0L);
            stats.setPendingLeaveCount(0L);
            stats.setPendingAssetCount(0L);
        }
    }

    /**
     * 工作台统计数据DTO
     * 根据角色类型返回不同的统计数据
     */
    @lombok.Data
    public static class WorkbenchStatsDTO {
        // 通用字段
        private Long taskCount;           // 待办任务数
        private String roleType;          // 角色类型：LAWYER/FINANCE/ADMIN_STAFF
        
        // 律师/团队负责人/主任相关
        private Long matterCount;         // 我的项目数
        private Long clientCount;         // 我的客户数
        private Double timesheetHours;    // 本月工时
        
        // 财务相关
        private Long pendingPaymentCount;    // 待确认收款数
        private Long pendingInvoiceCount;    // 待开票数
        private Long pendingExpenseCount;    // 待审批报销数
        private java.math.BigDecimal monthlyReceivedAmount;  // 本月已收金额
        
        // 行政相关
        private Long pendingLetterCount;     // 待处理出函数
        private Long pendingSealCount;       // 待处理用印数
        private Long pendingLeaveCount;      // 待审批请假数
        private Long pendingAssetCount;      // 待处理资产领用数
    }

    /**
     * 计算增长率（与上月对比）
     */
    private BigDecimal calculateGrowthRate(BigDecimal currentMonth) {
        if (currentMonth == null || currentMonth.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // 获取可访问的项目ID列表（用于查询上月收入）
        String dataScope = com.lawfirm.common.util.SecurityUtils.getDataScope();
        Long currentUserId = com.lawfirm.common.util.SecurityUtils.getUserId();
        Long deptId = com.lawfirm.common.util.SecurityUtils.getDepartmentId();
        List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);
        
        // 查询上月收入
        BigDecimal lastMonthRevenue = statisticsMapper.sumLastMonthRevenue(accessibleMatterIds);
        if (lastMonthRevenue == null || lastMonthRevenue.compareTo(BigDecimal.ZERO) == 0) {
            // 如果上月收入为0，本月有收入则增长率为100%
            if (currentMonth.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(100);
            }
            return BigDecimal.ZERO;
        }
        
        // 计算增长率：(本月收入 - 上月收入) / 上月收入 * 100
        BigDecimal difference = currentMonth.subtract(lastMonthRevenue);
        BigDecimal growthRate = difference.divide(lastMonthRevenue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        return growthRate;
    }

    /**
     * 获取可访问的项目ID列表（根据数据权限）
     * ✅ 修复问题554: 使用ThreadLocal缓存避免同一请求内重复查询
     * @return null表示可以访问所有项目，否则返回可访问的项目ID列表
     */
    private List<Long> getAccessibleMatterIds(String dataScope, Long currentUserId, Long deptId) {
        if ("ALL".equals(dataScope)) {
            return null; // null表示可以访问所有项目
        }
        
        // ✅ 检查缓存
        String cacheKey = dataScope + "_" + currentUserId + "_" + deptId;
        Map<String, List<Long>> cache = MATTER_IDS_CACHE.get();
        if (cache != null && cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        List<Long> matterIds = calculateAccessibleMatterIds(dataScope, currentUserId, deptId);
        
        // ✅ 放入缓存
        if (cache == null) {
            cache = new HashMap<>();
            MATTER_IDS_CACHE.set(cache);
        }
        cache.put(cacheKey, matterIds);
        
        return matterIds;
    }
    
    /**
     * 计算可访问的项目ID列表（实际查询）
     */
    private List<Long> calculateAccessibleMatterIds(String dataScope, Long currentUserId, Long deptId) {
        List<Long> matterIds = new ArrayList<>();
        
        if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
            // 部门及下级部门：使用递归CTE查询所有下级部门的项目
            List<Long> allDeptIds = getAllDepartmentIds(deptId);
            matterIds = matterRepository.lambdaQuery()
                    .select(com.lawfirm.domain.matter.entity.Matter::getId)
                    .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                    .in(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, allDeptIds)
                    .list()
                    .stream()
                    .map(com.lawfirm.domain.matter.entity.Matter::getId)
                    .collect(Collectors.toList());
        } else if ("DEPT".equals(dataScope) && deptId != null) {
            // 本部门：查询本部门的项目
            matterIds = matterRepository.lambdaQuery()
                    .select(com.lawfirm.domain.matter.entity.Matter::getId)
                    .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                    .eq(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptId)
                    .list()
                    .stream()
                    .map(com.lawfirm.domain.matter.entity.Matter::getId)
                    .collect(Collectors.toList());
        } else {
            // SELF：只查看自己负责的项目或参与的项目
            // 查询自己负责的项目
            List<Long> leadMatterIds = matterRepository.lambdaQuery()
                    .select(com.lawfirm.domain.matter.entity.Matter::getId)
                    .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                    .eq(com.lawfirm.domain.matter.entity.Matter::getLeadLawyerId, currentUserId)
                    .list()
                    .stream()
                    .map(com.lawfirm.domain.matter.entity.Matter::getId)
                    .collect(Collectors.toList());
            
            // 查询自己参与的项目
            var participantList = matterParticipantRepository.lambdaQuery()
                    .select(com.lawfirm.domain.matter.entity.MatterParticipant::getMatterId)
                    .eq(com.lawfirm.domain.matter.entity.MatterParticipant::getUserId, currentUserId)
                    .eq(com.lawfirm.domain.matter.entity.MatterParticipant::getDeleted, false)
                    .list();
            
            List<Long> participantMatterIds = participantList.stream()
                    .map(com.lawfirm.domain.matter.entity.MatterParticipant::getMatterId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 合并去重
            matterIds.addAll(leadMatterIds);
            matterIds.addAll(participantMatterIds);
            matterIds = matterIds.stream().distinct().collect(Collectors.toList());
        }
        
        return matterIds.isEmpty() ? Collections.emptyList() : matterIds;
    }

    /**
     * 获取可访问的客户ID列表（根据数据权限）
     * ✅ 修复问题558: 使用ThreadLocal缓存避免同一请求内重复查询
     * @return null表示可以访问所有客户，否则返回可访问的客户ID列表
     */
    private List<Long> getAccessibleClientIds(String dataScope, Long currentUserId, Long deptId) {
        if ("ALL".equals(dataScope)) {
            return null; // null表示可以访问所有客户
        }
        
        // ✅ 检查缓存
        String cacheKey = dataScope + "_" + currentUserId + "_" + deptId;
        Map<String, List<Long>> cache = CLIENT_IDS_CACHE.get();
        if (cache != null && cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        List<Long> clientIds = calculateAccessibleClientIds(dataScope, currentUserId, deptId);
        
        // ✅ 放入缓存
        if (cache == null) {
            cache = new HashMap<>();
            CLIENT_IDS_CACHE.set(cache);
        }
        cache.put(cacheKey, clientIds);
        
        return clientIds;
    }
    
    /**
     * 计算可访问的客户ID列表（实际查询）
     */
    private List<Long> calculateAccessibleClientIds(String dataScope, Long currentUserId, Long deptId) {
        List<Long> clientIds = new ArrayList<>();
        
        if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
            // 部门及下级部门：使用递归CTE查询所有下级部门的用户负责的客户
            // 客户没有部门字段，通过负责律师的部门来过滤
            List<Long> allDeptIds = getAllDepartmentIds(deptId);
            var users = userRepository.lambdaQuery()
                    .select(com.lawfirm.domain.system.entity.User::getId)
                    .in(com.lawfirm.domain.system.entity.User::getDepartmentId, allDeptIds)
                    .eq(com.lawfirm.domain.system.entity.User::getDeleted, false)
                    .list();
            List<Long> userIds = users.stream()
                    .map(com.lawfirm.domain.system.entity.User::getId)
                    .collect(Collectors.toList());
            
            if (!userIds.isEmpty()) {
                clientIds = clientRepository.lambdaQuery()
                        .select(com.lawfirm.domain.client.entity.Client::getId)
                        .eq(com.lawfirm.domain.client.entity.Client::getDeleted, false)
                        .in(com.lawfirm.domain.client.entity.Client::getResponsibleLawyerId, userIds)
                        .list()
                        .stream()
                        .map(com.lawfirm.domain.client.entity.Client::getId)
                        .collect(Collectors.toList());
            }
        } else if ("DEPT".equals(dataScope) && deptId != null) {
            // 本部门：查询本部门负责的客户
            // 客户没有部门字段，通过负责律师的部门来过滤
            var users = userRepository.lambdaQuery()
                    .select(com.lawfirm.domain.system.entity.User::getId)
                    .eq(com.lawfirm.domain.system.entity.User::getDepartmentId, deptId)
                    .eq(com.lawfirm.domain.system.entity.User::getDeleted, false)
                    .list();
            List<Long> userIds = users.stream()
                    .map(com.lawfirm.domain.system.entity.User::getId)
                    .collect(Collectors.toList());
            
            if (!userIds.isEmpty()) {
                clientIds = clientRepository.lambdaQuery()
                        .select(com.lawfirm.domain.client.entity.Client::getId)
                        .eq(com.lawfirm.domain.client.entity.Client::getDeleted, false)
                        .in(com.lawfirm.domain.client.entity.Client::getResponsibleLawyerId, userIds)
                        .list()
                        .stream()
                        .map(com.lawfirm.domain.client.entity.Client::getId)
                        .collect(Collectors.toList());
            }
        } else {
            // SELF：只查看自己负责的客户
            clientIds = clientRepository.lambdaQuery()
                    .select(com.lawfirm.domain.client.entity.Client::getId)
                    .eq(com.lawfirm.domain.client.entity.Client::getDeleted, false)
                    .eq(com.lawfirm.domain.client.entity.Client::getResponsibleLawyerId, currentUserId)
                    .list()
                    .stream()
                    .map(com.lawfirm.domain.client.entity.Client::getId)
                    .collect(Collectors.toList());
        }
        
        return clientIds.isEmpty() ? Collections.emptyList() : clientIds;
    }
}

