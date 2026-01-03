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
@RequestMapping("/workbench/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsAppService statisticsAppService;

    /**
     * 获取收入统计
     */
    @Operation()
    @GetMapping("/revenue")
    @RequirePermission("statistics:view")
    public Result<StatisticsDTO.RevenueStats> getRevenueStats() {
        StatisticsDTO.RevenueStats stats = statisticsAppService.getRevenueStats();
        return Result.success(stats);
    }

    /**
     * 获取项目统计
     */
    @Operation()
    @GetMapping("/matter")
    @RequirePermission("statistics:view")
    public Result<StatisticsDTO.MatterStats> getMatterStats() {
        StatisticsDTO.MatterStats stats = statisticsAppService.getMatterStats();
        return Result.success(stats);
    }

    /**
     * 获取客户统计
     */
    @Operation()
    @GetMapping("/client")
    @RequirePermission("statistics:view")
    public Result<StatisticsDTO.ClientStats> getClientStats() {
        StatisticsDTO.ClientStats stats = statisticsAppService.getClientStats();
        return Result.success(stats);
    }

    /**
     * 获取律师业绩排行
     */
    @Operation()
    @GetMapping("/lawyer-performance")
    @RequirePermission("statistics:view")
    public Result<List<StatisticsDTO.LawyerPerformance>> getLawyerPerformanceRanking(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<StatisticsDTO.LawyerPerformance> rankings = statisticsAppService.getLawyerPerformanceRanking(limit);
        return Result.success(rankings);
    }
}

