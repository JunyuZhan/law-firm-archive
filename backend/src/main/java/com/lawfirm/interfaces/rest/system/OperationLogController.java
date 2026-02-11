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
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志管理接口 */
@Slf4j
@Tag(name = "操作日志管理", description = "操作日志查询、统计、清理等接口")
@RestController
@RequestMapping("/admin/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

  /** Excel导出列索引：第0列（ID） */
  private static final int EXCEL_COLUMN_INDEX_0 = 0;

  /** Excel导出列索引：第1列（操作模块） */
  private static final int EXCEL_COLUMN_INDEX_1 = 1;

  /** Excel导出列索引：第2列（操作类型） */
  private static final int EXCEL_COLUMN_INDEX_2 = 2;

  /** Excel导出列索引：第3列（操作描述） */
  private static final int EXCEL_COLUMN_INDEX_3 = 3;

  /** Excel导出列索引：第4列（操作人） */
  private static final int EXCEL_COLUMN_INDEX_4 = 4;

  /** Excel导出列索引：第5列（IP地址） */
  private static final int EXCEL_COLUMN_INDEX_5 = 5;

  /** Excel导出列索引：第6列（请求URL） */
  private static final int EXCEL_COLUMN_INDEX_6 = 6;

  /** Excel导出列索引：第7列（请求方式） */
  private static final int EXCEL_COLUMN_INDEX_7 = 7;

  /** Excel导出列索引：第8列（耗时） */
  private static final int EXCEL_COLUMN_INDEX_8 = 8;

  /** Excel导出列索引：第9列（状态） */
  private static final int EXCEL_COLUMN_INDEX_9 = 9;

  /** Excel导出列索引：第10列（错误信息） */
  private static final int EXCEL_COLUMN_INDEX_10 = 10;

  /** Excel导出列索引：第11列（创建时间） */
  private static final int EXCEL_COLUMN_INDEX_11 = 11;

  /** Excel数据起始行索引：1 */
  private static final int EXCEL_DATA_START_ROW_INDEX = 1;

  /** 导出最大记录数：10000条 */
  private static final int MAX_EXPORT_RECORDS = 10000;

  /** HTTP状态码：500 Internal Server Error */
  private static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

  /** 默认页码 */
  private static final String DEFAULT_PAGE_NUM = "1";

  /** 默认每页大小 */
  private static final String DEFAULT_PAGE_SIZE = "20";

  /** 慢请求阈值 */
  private static final String DEFAULT_SLOW_THRESHOLD = "1000";

  /** 默认保留天数 */
  private static final String DEFAULT_KEEP_DAYS = "30";

  /** 失败状态 */
  private static final String FAIL_STATUS = "FAIL";

  /** 日期格式 */
  private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

  /** 导出日期格式 */
  private static final String EXPORT_DATE_PATTERN = "yyyyMMdd_HHmmss";

  /** Excel MIME类型 */
  private static final String EXCEL_MIME_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  /** 操作日志应用服务 */
  private final OperationLogAppService operationLogAppService;

  /**
   * 分页查询操作日志
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("sys:log:list")
  @Operation(summary = "分页查询操作日志")
  public Result<PageResult<OperationLogDTO>> listOperationLogs(
      @Parameter(description = "查询条件") @org.springframework.web.bind.annotation.ModelAttribute
          final OperationLogQueryDTO query) {
    // 设置默认值
    if (query.getPageNum() == null) {
      query.setPageNum(Integer.parseInt(DEFAULT_PAGE_NUM));
    }
    if (query.getPageSize() == null) {
      query.setPageSize(Integer.parseInt(DEFAULT_PAGE_SIZE));
    }
    return Result.success(operationLogAppService.listOperationLogs(query));
  }

  /**
   * 获取操作日志详情
   *
   * @param id 日志ID
   * @return 操作日志信息
   */
  @GetMapping("/{id}")
  @RequirePermission("sys:log:list")
  @Operation(summary = "获取操作日志详情")
  public Result<OperationLogDTO> getOperationLog(@PathVariable final Long id) {
    OperationLogDTO log = operationLogAppService.getOperationLogById(id);
    if (log == null) {
      return Result.fail("日志不存在");
    }
    return Result.success(log);
  }

  /**
   * 获取所有模块列表
   *
   * @return 模块列表
   */
  @GetMapping("/modules")
  @RequirePermission("sys:log:list")
  @Operation(summary = "获取所有模块列表", description = "用于下拉选择")
  public Result<List<String>> listModules() {
    return Result.success(operationLogAppService.listModules());
  }

  /**
   * 获取所有操作类型列表
   *
   * @return 操作类型列表
   */
  @GetMapping("/operation-types")
  @RequirePermission("sys:log:list")
  @Operation(summary = "获取所有操作类型列表", description = "用于下拉选择")
  public Result<List<String>> listOperationTypes() {
    return Result.success(operationLogAppService.listOperationTypes());
  }

  /**
   * 获取操作日志统计
   *
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 统计信息
   */
  @GetMapping("/statistics")
  @RequirePermission("sys:log:list")
  @Operation(summary = "获取操作日志统计")
  public Result<Map<String, Object>> getStatistics(
      @Parameter(description = "开始时间")
          @RequestParam(required = false)
          @DateTimeFormat(pattern = DATE_PATTERN)
          final LocalDateTime startTime,
      @Parameter(description = "结束时间")
          @RequestParam(required = false)
          @DateTimeFormat(pattern = DATE_PATTERN)
          final LocalDateTime endTime) {

    OperationLogQueryDTO query = new OperationLogQueryDTO();
    query.setStartTime(startTime);
    query.setEndTime(endTime);

    return Result.success(operationLogAppService.getStatistics(query));
  }

  /**
   * 清理历史日志
   *
   * @param keepDays 保留天数
   * @return 删除的记录数
   */
  @DeleteMapping("/clean")
  @RequirePermission("sys:log:delete")
  @Operation(summary = "清理历史日志", description = "清理指定天数之前的日志，最少保留7天")
  public Result<Integer> cleanHistoryLogs(
      @Parameter(description = "保留天数", example = "30")
          @RequestParam(defaultValue = DEFAULT_KEEP_DAYS)
          final Integer keepDays) {
    int deleted = operationLogAppService.cleanHistoryLogs(keepDays);
    return Result.success(deleted);
  }

  /**
   * 查询慢请求日志
   *
   * @param threshold 执行时长阈值(ms)
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 分页结果
   */
  @GetMapping("/slow-requests")
  @RequirePermission("sys:log:list")
  @Operation(summary = "查询慢请求日志", description = "查询执行时间超过指定阈值的请求")
  public Result<PageResult<OperationLogDTO>> listSlowRequests(
      @Parameter(description = "执行时长阈值(ms)", example = "1000")
          @RequestParam(defaultValue = DEFAULT_SLOW_THRESHOLD)
          final Long threshold,
      @Parameter(description = "开始时间")
          @RequestParam(required = false)
          @DateTimeFormat(pattern = DATE_PATTERN)
          final LocalDateTime startTime,
      @Parameter(description = "结束时间")
          @RequestParam(required = false)
          @DateTimeFormat(pattern = DATE_PATTERN)
          final LocalDateTime endTime,
      @Parameter(description = "页码") @RequestParam(defaultValue = DEFAULT_PAGE_NUM)
          final Integer pageNum,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
          final Integer pageSize) {

    OperationLogQueryDTO query = new OperationLogQueryDTO();
    query.setMinExecutionTime(threshold);
    query.setStartTime(startTime);
    query.setEndTime(endTime);
    query.setPageNum(pageNum);
    query.setPageSize(pageSize);

    return Result.success(operationLogAppService.listOperationLogs(query));
  }

  /**
   * 查询错误日志
   *
   * @param module 操作模块
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 分页结果
   */
  @GetMapping("/errors")
  @RequirePermission("sys:log:list")
  @Operation(summary = "查询错误日志", description = "查询执行失败的请求")
  public Result<PageResult<OperationLogDTO>> listErrorLogs(
      @Parameter(description = "操作模块") @RequestParam(required = false) final String module,
      @Parameter(description = "开始时间")
          @RequestParam(required = false)
          @DateTimeFormat(pattern = DATE_PATTERN)
          final LocalDateTime startTime,
      @Parameter(description = "结束时间")
          @RequestParam(required = false)
          @DateTimeFormat(pattern = DATE_PATTERN)
          final LocalDateTime endTime,
      @Parameter(description = "页码") @RequestParam(defaultValue = DEFAULT_PAGE_NUM)
          final Integer pageNum,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
          final Integer pageSize) {

    OperationLogQueryDTO query = new OperationLogQueryDTO();
    query.setModule(module);
    query.setStatus(FAIL_STATUS);
    query.setStartTime(startTime);
    query.setEndTime(endTime);
    query.setPageNum(pageNum);
    query.setPageSize(pageSize);

    return Result.success(operationLogAppService.listOperationLogs(query));
  }

  /**
   * 导出操作日志
   *
   * @param query 查询条件
   * @param response HTTP响应
   */
  @PostMapping("/export")
  @RequirePermission("sys:log:list")
  @Operation(summary = "导出操作日志", description = "导出操作日志为Excel文件，最多导出10000条")
  public void exportOperationLogs(
      @RequestBody(required = false) final OperationLogQueryDTO query,
      final HttpServletResponse response) {
    try {
      OperationLogQueryDTO effectiveQuery = query;
      if (effectiveQuery == null) {
        effectiveQuery = new OperationLogQueryDTO();
      }

      // 查询数据（最多10000条）
      List<OperationLogDTO> logs =
          operationLogAppService.listForExport(effectiveQuery, MAX_EXPORT_RECORDS);

      // 生成Excel
      byte[] excelData = generateExcel(logs);

      // 设置响应头
      String fileName =
          "操作日志_"
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern(EXPORT_DATE_PATTERN))
              + ".xlsx";
      String encodedFileName =
          URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

      response.setContentType(EXCEL_MIME_TYPE);
      response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
      response.setContentLength(excelData.length);
      response.getOutputStream().write(excelData);
      response.getOutputStream().flush();
    } catch (Exception e) {
      log.error("导出操作日志失败", e);
      try {
        response.sendError(HTTP_STATUS_INTERNAL_SERVER_ERROR, "导出失败: " + e.getMessage());
      } catch (IOException sendError) {
        log.debug("发送错误响应失败", sendError);
      }
    }
  }

  /**
   * 生成Excel文件
   *
   * @param logs 操作日志列表
   * @return Excel文件字节数组
   * @throws IOException IO异常
   */
  private byte[] generateExcel(final List<OperationLogDTO> logs) throws IOException {
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
      String[] headers = {
        "ID", "操作模块", "操作类型", "操作描述", "操作人", "IP地址", "请求URL", "请求方式", "耗时(ms)", "状态", "错误信息", "操作时间"
      };
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // 日期格式
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_PATTERN);

      // 填充数据
      int rowNum = EXCEL_DATA_START_ROW_INDEX;
      for (OperationLogDTO logDto : logs) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(EXCEL_COLUMN_INDEX_0)
            .setCellValue(logDto.getId() != null ? logDto.getId() : 0);
        row.createCell(EXCEL_COLUMN_INDEX_1)
            .setCellValue(logDto.getModule() != null ? logDto.getModule() : "");
        row.createCell(EXCEL_COLUMN_INDEX_2)
            .setCellValue(logDto.getOperationType() != null ? logDto.getOperationType() : "");
        row.createCell(EXCEL_COLUMN_INDEX_3)
            .setCellValue(logDto.getDescription() != null ? logDto.getDescription() : "");
        row.createCell(EXCEL_COLUMN_INDEX_4)
            .setCellValue(logDto.getUserName() != null ? logDto.getUserName() : "");
        row.createCell(EXCEL_COLUMN_INDEX_5)
            .setCellValue(logDto.getIpAddress() != null ? logDto.getIpAddress() : "");
        row.createCell(EXCEL_COLUMN_INDEX_6)
            .setCellValue(logDto.getRequestUrl() != null ? logDto.getRequestUrl() : "");
        row.createCell(EXCEL_COLUMN_INDEX_7)
            .setCellValue(logDto.getRequestMethod() != null ? logDto.getRequestMethod() : "");
        row.createCell(EXCEL_COLUMN_INDEX_8)
            .setCellValue(logDto.getExecutionTime() != null ? logDto.getExecutionTime() : 0);
        row.createCell(EXCEL_COLUMN_INDEX_9)
            .setCellValue(logDto.getStatusName() != null ? logDto.getStatusName() : "");
        row.createCell(EXCEL_COLUMN_INDEX_10)
            .setCellValue(logDto.getErrorMessage() != null ? logDto.getErrorMessage() : "");
        row.createCell(EXCEL_COLUMN_INDEX_11)
            .setCellValue(logDto.getCreatedAt() != null ? logDto.getCreatedAt().format(dtf) : "");
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
