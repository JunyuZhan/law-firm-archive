package com.lawfirm.application.workbench.service;

import com.lawfirm.application.workbench.dto.StatisticsDTO;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.system.repository.UserRepository;
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
    private final com.lawfirm.domain.matter.repository.TimesheetRepository timesheetRepository;
    private final com.lawfirm.domain.matter.repository.TaskRepository taskRepository;
    private final com.lawfirm.domain.finance.repository.CommissionRepository commissionRepository;
    private final com.lawfirm.domain.matter.repository.MatterParticipantRepository matterParticipantRepository;

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
        List<StatisticsDTO.RevenueTrend> trends = statisticsMapper.getRevenueTrends(accessibleMatterIds).stream()
                .map(item -> {
                    StatisticsDTO.RevenueTrend trend = new StatisticsDTO.RevenueTrend();
                    trend.setPeriod((String) item.get("period"));
                    trend.setAmount((BigDecimal) item.get("amount"));
                    return trend;
                })
                .collect(Collectors.toList());
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
                // 部门及下级部门
                // TODO: 需要实现部门递归查询
                totalMatters = matterRepository.lambdaQuery()
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                        .eq(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptId)
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
     */
    public List<StatisticsDTO.LawyerPerformance> getLawyerPerformanceRanking(Integer limit) {
        // 根据用户权限过滤数据
        String dataScope = com.lawfirm.common.util.SecurityUtils.getDataScope();
        Long currentUserId = com.lawfirm.common.util.SecurityUtils.getUserId();
        Long deptId = com.lawfirm.common.util.SecurityUtils.getDepartmentId();

        // 获取可访问的项目ID列表
        List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

        List<Map<String, Object>> rankings = statisticsMapper.getLawyerPerformanceRanking(limit, accessibleMatterIds);
        
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
            
            // 计算提成（根据权限过滤）
            BigDecimal commission = commissionRepository.sumCommissionByUserId(lawyerId);
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
     */
    public WorkbenchStatsDTO getWorkbenchStats() {
        Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
        
        WorkbenchStatsDTO stats = new WorkbenchStatsDTO();
        
        // 我的项目数（我参与的项目）- 直接使用MyBatis-Plus查询
        // 先查询参与者列表，然后去重统计
        var participantList = matterParticipantRepository.lambdaQuery()
                .select(com.lawfirm.domain.matter.entity.MatterParticipant::getMatterId)
                .eq(com.lawfirm.domain.matter.entity.MatterParticipant::getUserId, userId)
                .list();
        
        long matterCountLong = participantList.stream()
                .map(com.lawfirm.domain.matter.entity.MatterParticipant::getMatterId)
                .distinct()
                .filter(matterId -> {
                    // 检查项目是否存在且未删除
                    try {
                        var matter = matterRepository.getById(matterId);
                        return matter != null && !matter.getDeleted();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
        Long matterCount = Long.valueOf(matterCountLong);
        log.info("我的项目数查询结果: userId={}, matterCount={}, type={}", userId, matterCount, matterCount.getClass().getName());
        stats.setMatterCount(matterCount);
        
        // 我的客户数（我负责的客户）- 直接使用MyBatis-Plus查询
        long clientCountLong = clientRepository.lambdaQuery()
                .eq(com.lawfirm.domain.client.entity.Client::getResponsibleLawyerId, userId)
                .count();
        Long clientCount = Long.valueOf(clientCountLong);
        log.info("我的客户数查询结果: userId={}, clientCount={}, type={}", userId, clientCount, clientCount.getClass().getName());
        stats.setClientCount(clientCount);
        
        // 本月工时
        java.time.LocalDate now = java.time.LocalDate.now();
        BigDecimal monthlyHours = timesheetRepository.sumHoursByUserAndMonth(userId, now.getYear(), now.getMonthValue());
        stats.setTimesheetHours(monthlyHours != null ? monthlyHours.doubleValue() : 0.0);
        
        // 待办任务数
        int taskCountInt = taskRepository.countPendingByAssigneeId(userId);
        Long taskCount = Long.valueOf(taskCountInt);
        stats.setTaskCount(taskCount);
        
        log.info("获取工作台统计: userId={}, matterCount={}, clientCount={}, hours={}, taskCount={}", 
                userId, matterCount, clientCount, monthlyHours, taskCount);
        log.info("WorkbenchStatsDTO最终值: matterCount={}, clientCount={}, timesheetHours={}, taskCount={}", 
                stats.getMatterCount(), stats.getClientCount(), stats.getTimesheetHours(), stats.getTaskCount());
        
        return stats;
    }

    /**
     * 工作台统计数据DTO
     */
    @lombok.Data
    public static class WorkbenchStatsDTO {
        private Long matterCount;      // 我的项目数
        private Long clientCount;      // 我的客户数
        private Double timesheetHours;  // 本月工时
        private Long taskCount;        // 待办任务数
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
     * @return null表示可以访问所有项目，否则返回可访问的项目ID列表
     */
    private List<Long> getAccessibleMatterIds(String dataScope, Long currentUserId, Long deptId) {
        if ("ALL".equals(dataScope)) {
            return null; // null表示可以访问所有项目
        }
        
        List<Long> matterIds = new ArrayList<>();
        
        if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
            // 部门及下级部门：查询本部门及下级部门的项目
            // TODO: 需要实现部门递归查询
            matterIds = matterRepository.lambdaQuery()
                    .select(com.lawfirm.domain.matter.entity.Matter::getId)
                    .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                    .eq(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptId)
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
     * @return null表示可以访问所有客户，否则返回可访问的客户ID列表
     */
    private List<Long> getAccessibleClientIds(String dataScope, Long currentUserId, Long deptId) {
        if ("ALL".equals(dataScope)) {
            return null; // null表示可以访问所有客户
        }
        
        List<Long> clientIds = new ArrayList<>();
        
        if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
            // 部门及下级部门：查询本部门及下级部门负责的客户
            // 客户没有部门字段，通过负责律师的部门来过滤
            // TODO: 需要实现部门递归查询
            // 先查询该部门的用户，再查询这些用户负责的客户
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

