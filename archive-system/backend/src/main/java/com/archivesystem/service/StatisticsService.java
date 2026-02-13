package com.archivesystem.service;

import java.util.List;
import java.util.Map;

/**
 * 统计服务接口.
 */
public interface StatisticsService {

    /**
     * 获取概览统计.
     */
    Map<String, Object> getOverview();

    /**
     * 按档案类型统计.
     */
    List<Map<String, Object>> countByArchiveType();

    /**
     * 按保管期限统计.
     */
    List<Map<String, Object>> countByRetentionPeriod();

    /**
     * 按状态统计.
     */
    List<Map<String, Object>> countByStatus();

    /**
     * 按月份统计接收趋势.
     */
    List<Map<String, Object>> countByMonth(int year);

    /**
     * 借阅统计.
     */
    Map<String, Object> getBorrowStatistics();

    /**
     * 存储容量统计.
     */
    Map<String, Object> getStorageStatistics();

    /**
     * 按档案类型统计（别名方法）.
     */
    default List<Map<String, Object>> getByArchiveType() {
        return countByArchiveType();
    }

    /**
     * 按月份统计趋势（别名方法）.
     */
    default List<Map<String, Object>> getMonthlyTrend(int year) {
        return countByMonth(year);
    }
}
