package com.archivesystem.service;

import com.archivesystem.dto.archive.ArchiveQueryRequest;

import java.io.OutputStream;
import java.time.LocalDate;

/**
 * 报表服务接口
 */
public interface ReportService {

    /**
     * 导出档案统计概览报表
     * @param year 年份（可选）
     * @param outputStream 输出流
     */
    void exportOverviewReport(Integer year, OutputStream outputStream);

    /**
     * 导出档案清单
     * @param request 查询条件
     * @param outputStream 输出流
     */
    void exportArchiveList(ArchiveQueryRequest request, OutputStream outputStream);

    /**
     * 导出借阅统计报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param outputStream 输出流
     */
    void exportBorrowReport(LocalDate startDate, LocalDate endDate, OutputStream outputStream);

    /**
     * 导出操作日志报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param outputStream 输出流
     */
    void exportOperationLogReport(LocalDate startDate, LocalDate endDate, OutputStream outputStream);
}
