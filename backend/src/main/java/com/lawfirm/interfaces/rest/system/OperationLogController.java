package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.dto.OperationLogDTO;
import com.lawfirm.application.system.dto.OperationLogQueryDTO;
import com.lawfirm.application.system.service.OperationLogAppService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 操作日志管理接口
 */
@Slf4j
@Tag(name = "操作日志管理", description = "操作日志查询、统计、清理等接口")
@RestController
@RequestMapping("/admin/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {
    
    private final OperationLogAppService operationLogAppService;
    
    @GetMapping
    @RequirePermission("system:log:list")
    @Operation(summary = "分页查询操作日志")
    public Result<PageResult<OperationLogDTO>> listOperationLogs(
            @Parameter(description = "操作模块") @RequestParam(required = false) String module,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "操作用户名") @RequestParam(required = false) String userName,
            @Parameter(description = "操作用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "IP地址") @RequestParam(required = false) String ipAddress,
            @Parameter(description = "请求URL") @RequestParam(required = false) String requestUrl,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "最小执行时长(ms)") @RequestParam(required = false) Long minExecutionTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setModule(module);
        query.setOperationType(operationType);
        query.setUserName(userName);
        query.setUserId(userId);
        query.setStatus(status);
        query.setIpAddress(ipAddress);
        query.setRequestUrl(requestUrl);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setMinExecutionTime(minExecutionTime);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        return Result.success(operationLogAppService.listOperationLogs(query));
    }
    
    @GetMapping("/{id}")
    @RequirePermission("system:log:list")
    @Operation(summary = "获取操作日志详情")
    public Result<OperationLogDTO> getOperationLog(@PathVariable Long id) {
        OperationLogDTO log = operationLogAppService.getOperationLogById(id);
        if (log == null) {
            return Result.fail("日志不存在");
        }
        return Result.success(log);
    }
    
    @GetMapping("/modules")
    @RequirePermission("system:log:list")
    @Operation(summary = "获取所有模块列表", description = "用于下拉选择")
    public Result<List<String>> listModules() {
        return Result.success(operationLogAppService.listModules());
    }
    
    @GetMapping("/operation-types")
    @RequirePermission("system:log:list")
    @Operation(summary = "获取所有操作类型列表", description = "用于下拉选择")
    public Result<List<String>> listOperationTypes() {
        return Result.success(operationLogAppService.listOperationTypes());
    }
    
    @GetMapping("/statistics")
    @RequirePermission("system:log:list")
    @Operation(summary = "获取操作日志统计")
    public Result<Map<String, Object>> getStatistics(
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        
        return Result.success(operationLogAppService.getStatistics(query));
    }
    
    @DeleteMapping("/clean")
    @RequirePermission("system:log:delete")
    @Operation(summary = "清理历史日志", description = "清理指定天数之前的日志，最少保留7天")
    public Result<Integer> cleanHistoryLogs(
            @Parameter(description = "保留天数", example = "30") @RequestParam(defaultValue = "30") Integer keepDays) {
        int deleted = operationLogAppService.cleanHistoryLogs(keepDays);
        return Result.success(deleted);
    }
    
    @GetMapping("/slow-requests")
    @RequirePermission("system:log:list")
    @Operation(summary = "查询慢请求日志", description = "查询执行时间超过指定阈值的请求")
    public Result<PageResult<OperationLogDTO>> listSlowRequests(
            @Parameter(description = "执行时长阈值(ms)", example = "1000") @RequestParam(defaultValue = "1000") Long threshold,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setMinExecutionTime(threshold);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        return Result.success(operationLogAppService.listOperationLogs(query));
    }
    
    @GetMapping("/errors")
    @RequirePermission("system:log:list")
    @Operation(summary = "查询错误日志", description = "查询执行失败的请求")
    public Result<PageResult<OperationLogDTO>> listErrorLogs(
            @Parameter(description = "操作模块") @RequestParam(required = false) String module,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setModule(module);
        query.setStatus("FAIL");
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        return Result.success(operationLogAppService.listOperationLogs(query));
    }
    
    @PostMapping("/export")
    @RequirePermission("system:log:list")
    @Operation(summary = "导出操作日志", description = "导出操作日志为Excel文件，最多导出10000条")
    public void exportOperationLogs(
            @RequestBody(required = false) OperationLogQueryDTO query,
            HttpServletResponse response) {
        try {
            if (query == null) {
                query = new OperationLogQueryDTO();
            }
            
            // 查询数据（最多10000条）
            List<OperationLogDTO> logs = operationLogAppService.listForExport(query, 10000);
            
            // 生成Excel
            byte[] excelData = generateExcel(logs);
            
            // 设置响应头
            String fileName = "操作日志_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentLength(excelData.length);
            response.getOutputStream().write(excelData);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出操作日志失败", e);
            try {
                response.sendError(500, "导出失败: " + e.getMessage());
            } catch (IOException sendError) {
                log.debug("发送错误响应失败", sendError);
            }
        }
    }
    
    /**
     * 生成Excel文件
     */
    private byte[] generateExcel(List<OperationLogDTO> logs) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作日志");
            
            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 创建标题行
            String[] headers = {"ID", "操作模块", "操作类型", "操作描述", "操作人", "IP地址", "请求URL", "请求方式", "耗时(ms)", "状态", "错误信息", "操作时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 日期格式
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            // 填充数据
            int rowNum = 1;
            for (OperationLogDTO logDto : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(logDto.getId() != null ? logDto.getId() : 0);
                row.createCell(1).setCellValue(logDto.getModule() != null ? logDto.getModule() : "");
                row.createCell(2).setCellValue(logDto.getOperationType() != null ? logDto.getOperationType() : "");
                row.createCell(3).setCellValue(logDto.getDescription() != null ? logDto.getDescription() : "");
                row.createCell(4).setCellValue(logDto.getUserName() != null ? logDto.getUserName() : "");
                row.createCell(5).setCellValue(logDto.getIpAddress() != null ? logDto.getIpAddress() : "");
                row.createCell(6).setCellValue(logDto.getRequestUrl() != null ? logDto.getRequestUrl() : "");
                row.createCell(7).setCellValue(logDto.getRequestMethod() != null ? logDto.getRequestMethod() : "");
                row.createCell(8).setCellValue(logDto.getExecutionTime() != null ? logDto.getExecutionTime() : 0);
                row.createCell(9).setCellValue(logDto.getStatusName() != null ? logDto.getStatusName() : "");
                row.createCell(10).setCellValue(logDto.getErrorMessage() != null ? logDto.getErrorMessage() : "");
                row.createCell(11).setCellValue(logDto.getCreatedAt() != null ? logDto.getCreatedAt().format(dtf) : "");
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 输出到字节数组
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}
