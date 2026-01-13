package com.lawfirm.interfaces.rest.workbench;

import com.lawfirm.application.workbench.dto.StatisticsDTO;
import com.lawfirm.application.workbench.service.StatisticsAppService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统计中心 Controller
 */
@Tag(name = "统计中心", description = "统计数据相关接口")
@RestController
@RequestMapping("/workbench")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsAppService statisticsAppService;

    /**
     * 获取工作台统计数据（用于仪表盘）
     */
    @GetMapping("/stats")
    @Operation(summary = "获取工作台统计数据", description = "返回仪表盘需要的基础统计数据")
    public Result<StatisticsAppService.WorkbenchStatsDTO> getStats() {
        StatisticsAppService.WorkbenchStatsDTO stats = statisticsAppService.getWorkbenchStats();
        return Result.success(stats);
    }

    /**
     * 获取收入统计
     * 权限：report:list（报表中心访问权限）
     */
    @Operation(summary = "获取收入统计")
    @GetMapping("/statistics/revenue")
    @RequirePermission("report:list")
    public Result<StatisticsDTO.RevenueStats> getRevenueStats() {
        StatisticsDTO.RevenueStats stats = statisticsAppService.getRevenueStats();
        return Result.success(stats);
    }

    /**
     * 获取项目统计
     * 权限：report:list（报表中心访问权限）
     */
    @Operation(summary = "获取项目统计")
    @GetMapping("/statistics/matter")
    @RequirePermission("report:list")
    public Result<StatisticsDTO.MatterStats> getMatterStats() {
        StatisticsDTO.MatterStats stats = statisticsAppService.getMatterStats();
        return Result.success(stats);
    }

    /**
     * 获取客户统计
     * 权限：report:list（报表中心访问权限）
     */
    @Operation(summary = "获取客户统计")
    @GetMapping("/statistics/client")
    @RequirePermission("report:list")
    public Result<StatisticsDTO.ClientStats> getClientStats() {
        StatisticsDTO.ClientStats stats = statisticsAppService.getClientStats();
        return Result.success(stats);
    }

    /**
     * 获取律师业绩排行
     * 权限：report:list（报表中心访问权限）
     */
    @Operation(summary = "获取律师业绩排行")
    @GetMapping("/statistics/lawyer-performance")
    @RequirePermission("report:list")
    public Result<List<StatisticsDTO.LawyerPerformance>> getLawyerPerformanceRanking(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<StatisticsDTO.LawyerPerformance> rankings = statisticsAppService.getLawyerPerformanceRanking(limit);
        return Result.success(rankings);
    }
}

