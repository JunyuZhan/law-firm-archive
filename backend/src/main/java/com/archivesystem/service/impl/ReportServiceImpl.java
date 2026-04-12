package com.archivesystem.service.impl;

import com.archivesystem.dto.archive.ArchiveQueryRequest;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.repository.OperationLogMapper;
import com.archivesystem.service.ReportService;
import com.archivesystem.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 报表服务实现
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final StatisticsService statisticsService;
    private final ArchiveMapper archiveMapper;
    private final BorrowApplicationMapper borrowApplicationMapper;
    private final OperationLogMapper operationLogMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void exportOverviewReport(Integer year, OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建概览表
            createOverviewSheet(workbook, year);
            // 创建类型统计表
            createTypeStatisticsSheet(workbook);
            // 创建月度趋势表
            createTrendSheet(workbook, year != null ? year : LocalDate.now().getYear());
            
            workbook.write(outputStream);
            log.info("导出概览报表成功");
        } catch (IOException e) {
            log.error("导出概览报表失败", e);
            throw new RuntimeException("导出报表失败", e);
        }
    }

    @Override
    public void exportArchiveList(ArchiveQueryRequest request, OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("档案清单");
            
            // 创建标题样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "档案号", "题名", "档案类型", "全宗号", "分类号", 
                    "保管期限", "密级", "案件编号", "主办律师", "委托人", "文件数", "状态", "接收时间"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, headers[i].length() * 512);
            }
            
            // 查询数据（限制最大导出10000条）
            if (request.getPageSize() == null || request.getPageSize() > 10000) {
                request.setPageSize(10000);
            }
            request.setPageNum(1);
            
            LambdaQueryWrapper<Archive> wrapper = buildQueryWrapper(request);
            List<Archive> archives = archiveMapper.selectList(wrapper);
            
            // 填充数据
            int rowNum = 1;
            for (Archive archive : archives) {
                Row row = sheet.createRow(rowNum);
                int colNum = 0;
                
                createCell(row, colNum++, rowNum, dataStyle);
                createCell(row, colNum++, archive.getArchiveNo(), dataStyle);
                createCell(row, colNum++, archive.getTitle(), dataStyle);
                createCell(row, colNum++, getArchiveTypeName(archive.getArchiveType()), dataStyle);
                createCell(row, colNum++, archive.getFondsNo(), dataStyle);
                createCell(row, colNum++, archive.getCategoryCode(), dataStyle);
                createCell(row, colNum++, getRetentionPeriodName(archive.getRetentionPeriod()), dataStyle);
                createCell(row, colNum++, getSecurityLevelName(archive.getSecurityLevel()), dataStyle);
                createCell(row, colNum++, archive.getCaseNo(), dataStyle);
                createCell(row, colNum++, archive.getLawyerName(), dataStyle);
                createCell(row, colNum++, archive.getClientName(), dataStyle);
                createCell(row, colNum++, archive.getFileCount(), dataStyle);
                createCell(row, colNum++, getStatusName(archive.getStatus()), dataStyle);
                createCell(row, colNum++, archive.getReceivedAt() != null ? 
                        archive.getReceivedAt().format(DATETIME_FORMATTER) : "", dataStyle);
                
                rowNum++;
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(outputStream);
            log.info("导出档案清单成功: count={}", archives.size());
        } catch (IOException e) {
            log.error("导出档案清单失败", e);
            throw new RuntimeException("导出报表失败", e);
        }
    }

    @Override
    public void exportBorrowReport(LocalDate startDate, LocalDate endDate, OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("借阅统计");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "申请编号", "档案号", "档案题名", "申请人", "申请部门", 
                    "借阅目的", "申请时间", "审批状态", "审批人", "审批时间", "预计归还", "实际归还"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 查询借阅申请
            LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
            if (startDate != null) {
                wrapper.ge(BorrowApplication::getApplyTime, startDate.atStartOfDay());
            }
            if (endDate != null) {
                wrapper.le(BorrowApplication::getApplyTime, endDate.plusDays(1).atStartOfDay());
            }
            wrapper.orderByDesc(BorrowApplication::getApplyTime);
            
            List<BorrowApplication> applications = borrowApplicationMapper.selectList(wrapper);
            
            // 填充数据
            int rowNum = 1;
            for (BorrowApplication app : applications) {
                Row row = sheet.createRow(rowNum);
                int colNum = 0;
                
                createCell(row, colNum++, rowNum, dataStyle);
                createCell(row, colNum++, app.getApplicationNo(), dataStyle);
                createCell(row, colNum++, app.getArchiveNo(), dataStyle);
                createCell(row, colNum++, app.getArchiveTitle(), dataStyle);
                createCell(row, colNum++, app.getApplicantName(), dataStyle);
                createCell(row, colNum++, app.getApplicantDept(), dataStyle);
                createCell(row, colNum++, app.getBorrowPurpose(), dataStyle);
                createCell(row, colNum++, app.getApplyTime() != null ? 
                        app.getApplyTime().format(DATETIME_FORMATTER) : "", dataStyle);
                createCell(row, colNum++, getBorrowStatusName(app.getStatus()), dataStyle);
                createCell(row, colNum++, app.getApproverName(), dataStyle);
                createCell(row, colNum++, app.getApproveTime() != null ? 
                        app.getApproveTime().format(DATETIME_FORMATTER) : "", dataStyle);
                createCell(row, colNum++, app.getExpectedReturnDate() != null ? 
                        app.getExpectedReturnDate().format(DATE_FORMATTER) : "", dataStyle);
                createCell(row, colNum++, app.getActualReturnDate() != null ? 
                        app.getActualReturnDate().format(DATE_FORMATTER) : "", dataStyle);
                
                rowNum++;
            }
            
            workbook.write(outputStream);
            log.info("导出借阅报表成功: count={}", applications.size());
        } catch (IOException e) {
            log.error("导出借阅报表失败", e);
            throw new RuntimeException("导出报表失败", e);
        }
    }

    @Override
    public void exportOperationLogReport(LocalDate startDate, LocalDate endDate, OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作日志");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "操作时间", "操作类型", "操作对象", "对象ID", 
                    "操作描述", "操作人", "操作IP"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 查询操作日志
            LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
            if (startDate != null) {
                wrapper.ge(OperationLog::getOperatedAt, startDate.atStartOfDay());
            }
            if (endDate != null) {
                wrapper.le(OperationLog::getOperatedAt, endDate.plusDays(1).atStartOfDay());
            }
            wrapper.orderByDesc(OperationLog::getOperatedAt);
            wrapper.last("LIMIT 10000");
            
            List<OperationLog> logs = operationLogMapper.selectList(wrapper);
            
            // 填充数据
            int rowNum = 1;
            for (OperationLog opLog : logs) {
                Row row = sheet.createRow(rowNum);
                int colNum = 0;
                
                createCell(row, colNum++, rowNum, dataStyle);
                createCell(row, colNum++, opLog.getOperatedAt() != null ? 
                        opLog.getOperatedAt().format(DATETIME_FORMATTER) : "", dataStyle);
                createCell(row, colNum++, opLog.getOperationType(), dataStyle);
                createCell(row, colNum++, opLog.getObjectType(), dataStyle);
                createCell(row, colNum++, opLog.getObjectId() != null ? 
                        String.valueOf(opLog.getObjectId()) : "", dataStyle);
                createCell(row, colNum++, opLog.getOperationDesc(), dataStyle);
                createCell(row, colNum++, opLog.getOperatorName(), dataStyle);
                createCell(row, colNum++, opLog.getOperatorIp(), dataStyle);
                
                rowNum++;
            }
            
            workbook.write(outputStream);
            log.info("导出操作日志成功: count={}", logs.size());
        } catch (IOException e) {
            log.error("导出操作日志失败", e);
            throw new RuntimeException("导出报表失败", e);
        }
    }

    /**
     * 创建概览表
     */
    private void createOverviewSheet(Workbook workbook, Integer year) {
        Sheet sheet = workbook.createSheet("统计概览");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        
        // 标题
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("档案管理系统统计报表");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        // 生成日期
        Row dateRow = sheet.createRow(1);
        dateRow.createCell(0).setCellValue("生成日期：" + LocalDate.now().format(DATE_FORMATTER));
        
        // 获取统计数据
        Map<String, Object> overview = statisticsService.getOverview();
        
        // 概览数据
        int rowNum = 3;
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "指标", headerStyle);
        createCell(headerRow, 1, "数值", headerStyle);
        
        addStatRow(sheet, rowNum++, "档案总数", overview.get("totalArchives"), dataStyle);
        addStatRow(sheet, rowNum++, "本月新增", overview.get("monthlyNew"), dataStyle);
        addStatRow(sheet, rowNum++, "本年新增", overview.get("yearlyNew"), dataStyle);
        addStatRow(sheet, rowNum++, "文件总数", overview.get("totalFiles"), dataStyle);
        addStatRow(sheet, rowNum++, "存储总量", overview.get("totalStorageFormatted"), dataStyle);
        addStatRow(sheet, rowNum++, "待审批借阅", overview.get("pendingBorrows"), dataStyle);
        addStatRow(sheet, rowNum++, "借出中", overview.get("borrowedCount"), dataStyle);
        
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 5000);
    }

    /**
     * 创建类型统计表
     */
    private void createTypeStatisticsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("类型统计");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // 获取统计数据
        List<Map<String, Object>> typeStats = statisticsService.getByArchiveType();
        
        // 表头
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "档案类型", headerStyle);
        createCell(headerRow, 1, "数量", headerStyle);
        createCell(headerRow, 2, "占比", headerStyle);
        
        // 计算总数
        long total = typeStats.stream()
                .mapToLong(m -> ((Number) m.get("count")).longValue())
                .sum();
        
        // 数据
        int rowNum = 1;
        for (Map<String, Object> stat : typeStats) {
            Row row = sheet.createRow(rowNum++);
            String type = (String) stat.get("archiveType");
            long count = ((Number) stat.get("count")).longValue();
            double ratio = total > 0 ? (double) count / total * 100 : 0;
            
            createCell(row, 0, getArchiveTypeName(type), dataStyle);
            createCell(row, 1, count, dataStyle);
            createCell(row, 2, String.format("%.1f%%", ratio), dataStyle);
        }
        
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 3000);
        sheet.setColumnWidth(2, 3000);
    }

    /**
     * 创建月度趋势表
     */
    private void createTrendSheet(Workbook workbook, int year) {
        Sheet sheet = workbook.createSheet("月度趋势");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // 获取趋势数据
        List<Map<String, Object>> trendData = statisticsService.getMonthlyTrend(year);
        
        // 表头
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "月份", headerStyle);
        createCell(headerRow, 1, "新增档案数", headerStyle);
        
        // 数据
        int rowNum = 1;
        for (Map<String, Object> data : trendData) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, data.get("month") + "月", dataStyle);
            createCell(row, 1, ((Number) data.get("count")).longValue(), dataStyle);
        }
        
        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 4000);
    }

    /**
     * 添加统计行
     */
    private void addStatRow(Sheet sheet, int rowNum, String label, Object value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, label, style);
        if (value instanceof Number) {
            createCell(row, 1, ((Number) value).longValue(), style);
        } else {
            createCell(row, 1, value != null ? value.toString() : "", style);
        }
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Archive> buildQueryWrapper(ArchiveQueryRequest request) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getDeleted, false);
        
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(Archive::getTitle, request.getKeyword())
                    .or().like(Archive::getArchiveNo, request.getKeyword())
            );
        }
        if (StringUtils.hasText(request.getArchiveType())) {
            wrapper.eq(Archive::getArchiveType, request.getArchiveType());
        }
        if (request.getFondsId() != null) {
            wrapper.eq(Archive::getFondsId, request.getFondsId());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Archive::getStatus, request.getStatus());
        }
        if (request.getArchiveDateStart() != null) {
            wrapper.ge(Archive::getArchiveDate, request.getArchiveDateStart());
        }
        if (request.getArchiveDateEnd() != null) {
            wrapper.le(Archive::getArchiveDate, request.getArchiveDateEnd());
        }
        
        wrapper.orderByDesc(Archive::getCreatedAt);
        wrapper.last("LIMIT 10000");
        
        return wrapper;
    }

    // ===== 样式创建方法 =====
    
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        cell.setCellStyle(style);
    }

    // ===== 名称转换方法 =====
    
    private String getArchiveTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "DOCUMENT" -> "文书档案";
            case "SCIENCE" -> "科技档案";
            case "ACCOUNTING" -> "会计档案";
            case "PERSONNEL" -> "人事档案";
            case "SPECIAL" -> "专业档案";
            case "AUDIOVISUAL" -> "声像档案";
            default -> type;
        };
    }

    private String getRetentionPeriodName(String period) {
        if (period == null) return "";
        return switch (period) {
            case "PERMANENT" -> "永久";
            case "Y30" -> "30年";
            case "Y15" -> "15年";
            case "Y10" -> "10年";
            case "Y5" -> "5年";
            default -> period;
        };
    }

    private String getSecurityLevelName(String level) {
        if (level == null) return "";
        return switch (level) {
            case "PUBLIC" -> "公开";
            case "INTERNAL" -> "内部";
            case "SECRET" -> "秘密";
            case "CONFIDENTIAL" -> "机密";
            default -> level;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "RECEIVED" -> "已接收";
            case "PROCESSING" -> "处理中";
            case "CATALOGING" -> "整理中";
            case "STORED" -> "已归档";
            case "BORROWED" -> "借出中";
            case "APPRAISAL" -> "鉴定中";
            case "DESTROYED" -> "已销毁";
            default -> status;
        };
    }

    private String getBorrowStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已拒绝";
            case "BORROWED" -> "借阅中";
            case "RETURNED" -> "已归还";
            case "OVERDUE" -> "已逾期";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
