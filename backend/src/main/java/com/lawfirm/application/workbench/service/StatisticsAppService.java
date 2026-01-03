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

    /**
     * 获取收入统计
     */
    public StatisticsDTO.RevenueStats getRevenueStats() {
        StatisticsDTO.RevenueStats stats = new StatisticsDTO.RevenueStats();

        // 查询总收入
        BigDecimal totalRevenue = statisticsMapper.sumTotalRevenue();
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        // 查询本月收入
        BigDecimal monthlyRevenue = statisticsMapper.sumMonthlyRevenue();
        stats.setMonthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);

        // 查询本年收入
        BigDecimal yearlyRevenue = statisticsMapper.sumYearlyRevenue();
        stats.setYearlyRevenue(yearlyRevenue != null ? yearlyRevenue : BigDecimal.ZERO);

        // 查询待收金额
        BigDecimal pendingRevenue = statisticsMapper.sumPendingRevenue();
        stats.setPendingRevenue(pendingRevenue != null ? pendingRevenue : BigDecimal.ZERO);

        // 计算增长率（与上月对比）
        BigDecimal growthRate = calculateGrowthRate(monthlyRevenue);
        stats.setGrowthRate(growthRate);

        // 查询收入趋势
        List<StatisticsDTO.RevenueTrend> trends = statisticsMapper.getRevenueTrends().stream()
                .map(item -> {
                    StatisticsDTO.RevenueTrend trend = new StatisticsDTO.RevenueTrend();
                    trend.setPeriod((String) item.get("period"));
                    trend.setAmount((BigDecimal) item.get("amount"));
                    return trend;
                })
                .collect(Collectors.toList());
        stats.setTrends(trends);

        log.info("获取收入统计: total={}, monthly={}, yearly={}", totalRevenue, monthlyRevenue, yearlyRevenue);
        return stats;
    }

    /**
     * 获取项目统计
     */
    public StatisticsDTO.MatterStats getMatterStats() {
        StatisticsDTO.MatterStats stats = new StatisticsDTO.MatterStats();

        // 总案件数
        long totalMatters = matterRepository.count();
        stats.setTotalMatters(totalMatters);

        // 进行中案件数
        Long activeMatters = statisticsMapper.countActiveMatters();
        stats.setActiveMatters(activeMatters != null ? activeMatters : 0L);

        // 已完成案件数
        Long completedMatters = statisticsMapper.countCompletedMatters();
        stats.setCompletedMatters(completedMatters != null ? completedMatters : 0L);

        // 各状态案件数
        Map<String, Long> statusCount = statisticsMapper.countMattersByStatus().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("status"),
                        item -> ((Number) item.get("count")).longValue()
                ));
        stats.setStatusCount(statusCount);

        // 各类型案件数
        Map<String, Long> typeCount = statisticsMapper.countMattersByType().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("business_type"),
                        item -> ((Number) item.get("count")).longValue()
                ));
        stats.setTypeCount(typeCount);

        log.info("获取项目统计: total={}, active={}, completed={}", totalMatters, activeMatters, completedMatters);
        return stats;
    }

    /**
     * 获取客户统计
     */
    public StatisticsDTO.ClientStats getClientStats() {
        StatisticsDTO.ClientStats stats = new StatisticsDTO.ClientStats();

        // 总客户数
        long totalClients = clientRepository.count();
        stats.setTotalClients(totalClients);

        // 正式客户数
        Long formalClients = statisticsMapper.countFormalClients();
        stats.setFormalClients(formalClients != null ? formalClients : 0L);

        // 潜在客户数
        Long potentialClients = statisticsMapper.countPotentialClients();
        stats.setPotentialClients(potentialClients != null ? potentialClients : 0L);

        // 本月新增客户数
        Long newClientsThisMonth = statisticsMapper.countNewClientsThisMonth();
        stats.setNewClientsThisMonth(newClientsThisMonth != null ? newClientsThisMonth : 0L);

        // 各类型客户数
        Map<String, Long> typeCount = statisticsMapper.countClientsByType().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("client_type"),
                        item -> ((Number) item.get("count")).longValue()
                ));
        stats.setTypeCount(typeCount);

        log.info("获取客户统计: total={}, formal={}, potential={}, newThisMonth={}", 
                totalClients, formalClients, potentialClients, newClientsThisMonth);
        return stats;
    }

    /**
     * 获取律师业绩排行
     */
    public List<StatisticsDTO.LawyerPerformance> getLawyerPerformanceRanking(Integer limit) {
        List<Map<String, Object>> rankings = statisticsMapper.getLawyerPerformanceRanking(limit);
        
        List<StatisticsDTO.LawyerPerformance> result = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> item : rankings) {
            StatisticsDTO.LawyerPerformance performance = new StatisticsDTO.LawyerPerformance();
            performance.setLawyerId(((Number) item.get("lawyer_id")).longValue());
            performance.setLawyerName((String) item.get("lawyer_name"));
            performance.setMatterCount(((Number) item.get("matter_count")).longValue());
            performance.setRevenue((BigDecimal) item.get("revenue"));
            performance.setCommission(BigDecimal.ZERO); // TODO: 计算提成
            performance.setHours(0.0); // TODO: 从工时表统计
            performance.setRank(rank++);
            result.add(performance);
        }
        
        log.info("获取律师业绩排行: count={}", result.size());
        return result;
    }

    /**
     * 获取工作台统计数据（用于仪表盘）
     */
    public WorkbenchStatsDTO getWorkbenchStats() {
        Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
        
        WorkbenchStatsDTO stats = new WorkbenchStatsDTO();
        
        // 我的项目数（我参与的项目）
        Long matterCount = statisticsMapper.countMyMatters(userId);
        stats.setMatterCount(matterCount != null ? matterCount : 0L);
        
        // 我的客户数（我负责的客户）
        Long clientCount = statisticsMapper.countMyClients(userId);
        stats.setClientCount(clientCount != null ? clientCount : 0L);
        
        // 本月工时
        java.time.LocalDate now = java.time.LocalDate.now();
        BigDecimal monthlyHours = timesheetRepository.sumHoursByUserAndMonth(userId, now.getYear(), now.getMonthValue());
        stats.setTimesheetHours(monthlyHours != null ? monthlyHours.doubleValue() : 0.0);
        
        // 待办任务数
        int taskCount = taskRepository.countPendingByAssigneeId(userId);
        stats.setTaskCount((long) taskCount);
        
        log.info("获取工作台统计: userId={}, matterCount={}, clientCount={}, hours={}, taskCount={}", 
                userId, matterCount, clientCount, monthlyHours, taskCount);
        
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
        // TODO: 查询上月收入并计算增长率
        // 这里先返回0，后续完善
        return BigDecimal.ZERO;
    }
}

