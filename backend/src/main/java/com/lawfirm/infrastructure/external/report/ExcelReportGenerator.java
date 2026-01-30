package com.lawfirm.infrastructure.external.report;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Excel报表生成器（使用Apache POI）.
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Component
public class ExcelReportGenerator {

  /** 合并区域列数：15列 */
  private static final int MERGED_REGION_COLUMNS_15 = 15;

  /** 合并区域列数：12列 */
  private static final int MERGED_REGION_COLUMNS_12 = 12;

  /** 合并区域列数：14列 */
  private static final int MERGED_REGION_COLUMNS_14 = 14;

  /**
   * 生成收入报表.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateRevenueReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("收入报表");

    // 创建标题样式
    CellStyle titleStyle = workbook.createCellStyle();
    Font titleFont = workbook.createFont();
    titleFont.setBold(true);
    titleFont.setFontHeightInPoints((short) 16);
    titleStyle.setFont(titleFont);
    titleStyle.setAlignment(HorizontalAlignment.CENTER);

    // 创建表头样式
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);

    // 创建数据样式
    CellStyle dataStyle = workbook.createCellStyle();
    dataStyle.setAlignment(HorizontalAlignment.LEFT);

    // 创建数字样式
    CellStyle numberStyle = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    numberStyle.setDataFormat(format.getFormat("#,##0.00"));

    int rowNum = 0;

    // 标题行
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("收入报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

    // 空行
    rowNum++;

    // 表头
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {"日期", "客户名称", "案件名称", "收费金额", "收款状态"};
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    // 数据行
    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;

      // 日期
      Cell dateCell = dataRow.createCell(colNum++);
      if (row.get("date") != null) {
        dateCell.setCellValue(row.get("date").toString());
      }
      dateCell.setCellStyle(dataStyle);

      // 客户名称
      Cell clientCell = dataRow.createCell(colNum++);
      if (row.get("clientName") != null) {
        clientCell.setCellValue(row.get("clientName").toString());
      }
      clientCell.setCellStyle(dataStyle);

      // 案件名称
      Cell matterCell = dataRow.createCell(colNum++);
      if (row.get("matterName") != null) {
        matterCell.setCellValue(row.get("matterName").toString());
      }
      matterCell.setCellStyle(dataStyle);

      // 收费金额
      Cell amountCell = dataRow.createCell(colNum++);
      if (row.get("amount") != null) {
        if (row.get("amount") instanceof Number) {
          amountCell.setCellValue(((Number) row.get("amount")).doubleValue());
        } else {
          amountCell.setCellValue(row.get("amount").toString());
        }
      }
      amountCell.setCellStyle(numberStyle);

      // 收款状态
      Cell statusCell = dataRow.createCell(colNum++);
      if (row.get("status") != null) {
        statusCell.setCellValue(row.get("status").toString());
      }
      statusCell.setCellStyle(dataStyle);
    }

    // 自动调整列宽
    for (int i = 0; i < headers.length; i++) {
      sheet.autoSizeColumn(i);
      sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // 增加一些宽度
    }

    // 写入到ByteArrayOutputStream
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成案件报表.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateMatterReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("案件报表");

    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    int rowNum = 0;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {"案件编号", "案件名称", "客户名称", "案件类型", "状态", "创建日期"};
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("matterNo") != null ? row.get("matterNo").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("matterName") != null ? row.get("matterName").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("clientName") != null ? row.get("clientName").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("matterType") != null ? row.get("matterType").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("status") != null ? row.get("status").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("createdAt") != null ? row.get("createdAt").toString() : "");
    }

    for (int i = 0; i < headers.length; i++) {
      sheet.autoSizeColumn(i);
      sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成客户报表.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateClientReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("客户报表");

    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    int rowNum = 0;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {"客户编号", "客户名称", "客户类型", "联系人", "联系电话", "状态"};
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("clientNo") != null ? row.get("clientNo").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("clientName") != null ? row.get("clientName").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("clientType") != null ? row.get("clientType").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(
              row.get("contactPerson") != null ? row.get("contactPerson").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("contactPhone") != null ? row.get("contactPhone").toString() : "");
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("status") != null ? row.get("status").toString() : "");
    }

    for (int i = 0; i < headers.length; i++) {
      sheet.autoSizeColumn(i);
      sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成律师业绩报表.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateLawyerPerformanceReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("律师业绩报表");

    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    CellStyle numberStyle = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    numberStyle.setDataFormat(format.getFormat("#,##0.00"));

    int rowNum = 0;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {"律师姓名", "案件数量", "总收入", "总工时", "平均案件收入"};
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow
          .createCell(colNum++)
          .setCellValue(row.get("lawyer_name") != null ? row.get("lawyer_name").toString() : "");

      Cell matterCountCell = dataRow.createCell(colNum++);
      Object matterCount = row.get("matter_count");
      if (matterCount != null) {
        matterCountCell.setCellValue(((Number) matterCount).doubleValue());
      }

      Cell revenueCell = dataRow.createCell(colNum++);
      Object revenue = row.get("total_revenue");
      if (revenue == null) {
        revenue = row.get("revenue"); // 兼容旧字段名
      }
      if (revenue != null) {
        revenueCell.setCellValue(((Number) revenue).doubleValue());
        revenueCell.setCellStyle(numberStyle);
      }

      Cell hoursCell = dataRow.createCell(colNum++);
      Object hours = row.get("total_hours");
      if (hours != null) {
        hoursCell.setCellValue(((Number) hours).doubleValue());
      }

      Cell avgRevenueCell = dataRow.createCell(colNum++);
      Object avgRevenue = row.get("avg_revenue");
      if (avgRevenue != null) {
        avgRevenueCell.setCellValue(((Number) avgRevenue).doubleValue());
        avgRevenueCell.setCellStyle(numberStyle);
      }
    }

    for (int i = 0; i < headers.length; i++) {
      sheet.autoSizeColumn(i);
      sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成应收报表.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateReceivableReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("应收报表");

    // 创建标题样式
    CellStyle titleStyle = workbook.createCellStyle();
    Font titleFont = workbook.createFont();
    titleFont.setBold(true);
    titleFont.setFontHeightInPoints((short) 16);
    titleStyle.setFont(titleFont);
    titleStyle.setAlignment(HorizontalAlignment.CENTER);

    // 创建表头样式
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);

    // 创建数据样式
    CellStyle dataStyle = workbook.createCellStyle();
    dataStyle.setAlignment(HorizontalAlignment.LEFT);

    // 创建数字样式
    CellStyle numberStyle = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    numberStyle.setDataFormat(format.getFormat("#,##0.00"));

    int rowNum = 0;

    // 标题行
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("应收报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 8));

    // 空行
    rowNum++;

    // 表头
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {"客户名称", "案件名称", "案件编号", "合同编号", "合同金额", "已收金额", "应收金额", "账龄(天)", "收款状态"};
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    // 数据行
    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;

      // 客户名称
      Cell clientCell = dataRow.createCell(colNum++);
      if (row.get("client_name") != null) {
        clientCell.setCellValue(row.get("client_name").toString());
      }
      clientCell.setCellStyle(dataStyle);

      // 案件名称
      Cell matterCell = dataRow.createCell(colNum++);
      if (row.get("matter_name") != null) {
        matterCell.setCellValue(row.get("matter_name").toString());
      }
      matterCell.setCellStyle(dataStyle);

      // 案件编号
      Cell matterNoCell = dataRow.createCell(colNum++);
      if (row.get("matter_no") != null) {
        matterNoCell.setCellValue(row.get("matter_no").toString());
      }
      matterNoCell.setCellStyle(dataStyle);

      // 合同编号
      Cell contractNoCell = dataRow.createCell(colNum++);
      if (row.get("contract_no") != null) {
        contractNoCell.setCellValue(row.get("contract_no").toString());
      }
      contractNoCell.setCellStyle(dataStyle);

      // 合同金额
      Cell contractAmountCell = dataRow.createCell(colNum++);
      if (row.get("contract_amount") != null) {
        if (row.get("contract_amount") instanceof Number) {
          contractAmountCell.setCellValue(((Number) row.get("contract_amount")).doubleValue());
        } else {
          contractAmountCell.setCellValue(row.get("contract_amount").toString());
        }
      }
      contractAmountCell.setCellStyle(numberStyle);

      // 已收金额
      Cell receivedCell = dataRow.createCell(colNum++);
      if (row.get("received_amount") != null) {
        if (row.get("received_amount") instanceof Number) {
          receivedCell.setCellValue(((Number) row.get("received_amount")).doubleValue());
        } else {
          receivedCell.setCellValue(row.get("received_amount").toString());
        }
      }
      receivedCell.setCellStyle(numberStyle);

      // 应收金额
      Cell receivableCell = dataRow.createCell(colNum++);
      if (row.get("receivable_amount") != null) {
        if (row.get("receivable_amount") instanceof Number) {
          receivableCell.setCellValue(((Number) row.get("receivable_amount")).doubleValue());
        } else {
          receivableCell.setCellValue(row.get("receivable_amount").toString());
        }
      }
      receivableCell.setCellStyle(numberStyle);

      // 账龄(天)
      Cell agingCell = dataRow.createCell(colNum++);
      if (row.get("aging_days") != null) {
        if (row.get("aging_days") instanceof Number) {
          agingCell.setCellValue(((Number) row.get("aging_days")).doubleValue());
        } else {
          agingCell.setCellValue(row.get("aging_days").toString());
        }
      }
      agingCell.setCellStyle(dataStyle);

      // 收款状态
      Cell statusCell = dataRow.createCell(colNum++);
      if (row.get("receivable_status") != null) {
        statusCell.setCellValue(row.get("receivable_status").toString());
      }
      statusCell.setCellStyle(dataStyle);
    }

    // 自动调整列宽
    for (int i = 0; i < headers.length; i++) {
      sheet.autoSizeColumn(i);
      sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
    }

    // 写入到ByteArrayOutputStream
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成项目进度报表（M3-025）.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateMatterProgressReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("项目进度报表");

    MatterProgressReportStyles styles = createMatterProgressReportStyles(workbook);
    int rowNum = createMatterProgressReportHeader(sheet, styles);
    String[] headers = createMatterProgressReportDataRows(sheet, data, styles, rowNum);
    autoSizeColumns(sheet, headers);

    // 写入到ByteArrayOutputStream
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /** 样式封装类 */
  private static class MatterProgressReportStyles {
    /** 标题样式 */
    private CellStyle titleStyle;

    /** 表头样式 */
    private CellStyle headerStyle;

    /** 数据样式 */
    private CellStyle dataStyle;

    /** 数字样式 */
    private CellStyle numberStyle;

    /** 百分比样式 */
    private CellStyle percentStyle;
  }

  /**
   * 创建项目进度报表样式
   *
   * @param workbook 工作簿
   * @return 样式对象
   */
  private MatterProgressReportStyles createMatterProgressReportStyles(final Workbook workbook) {
    MatterProgressReportStyles styles = new MatterProgressReportStyles();

    // 创建标题样式
    styles.titleStyle = workbook.createCellStyle();
    Font titleFont = workbook.createFont();
    titleFont.setBold(true);
    titleFont.setFontHeightInPoints((short) 16);
    styles.titleStyle.setFont(titleFont);
    styles.titleStyle.setAlignment(HorizontalAlignment.CENTER);

    // 创建表头样式
    styles.headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    styles.headerStyle.setFont(headerFont);
    styles.headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    styles.headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styles.headerStyle.setAlignment(HorizontalAlignment.CENTER);

    // 创建数据样式
    styles.dataStyle = workbook.createCellStyle();
    styles.dataStyle.setAlignment(HorizontalAlignment.LEFT);

    // 创建数字样式
    styles.numberStyle = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    styles.numberStyle.setDataFormat(format.getFormat("#,##0.00"));

    // 创建百分比样式
    styles.percentStyle = workbook.createCellStyle();
    styles.percentStyle.setDataFormat(format.getFormat("0.00%"));

    return styles;
  }

  /**
   * 创建项目进度报表表头
   *
   * @param sheet 工作表
   * @param styles 样式对象
   * @return 当前行号
   */
  private int createMatterProgressReportHeader(
      final Sheet sheet, final MatterProgressReportStyles styles) {
    int rowNum = 0;

    // 标题行
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("项目进度报表");
    titleCell.setCellStyle(styles.titleStyle);
    sheet.addMergedRegion(
        new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, MERGED_REGION_COLUMNS_15));

    // 空行
    rowNum++;

    // 表头
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {
      "项目编号", "项目名称", "客户名称", "业务类型", "状态", "主办律师",
      "总任务数", "已完成", "进行中", "完成率(%)", "总工时", "已审批工时",
      "创建日期", "预计完成", "实际完成", "预估收费"
    };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(styles.headerStyle);
    }

    return rowNum;
  }

  /**
   * 创建项目进度报表数据行
   *
   * @param sheet 工作表
   * @param data 数据
   * @param styles 样式对象
   * @param startRowNum 起始行号
   * @return 表头数组
   */
  private String[] createMatterProgressReportDataRows(
      final Sheet sheet,
      final List<Map<String, Object>> data,
      final MatterProgressReportStyles styles,
      final int startRowNum) {
    String[] headers = {
      "项目编号", "项目名称", "客户名称", "业务类型", "状态", "主办律师",
      "总任务数", "已完成", "进行中", "完成率(%)", "总工时", "已审批工时",
      "创建日期", "预计完成", "实际完成", "预估收费"
    };

    int rowNum = startRowNum;
    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      createMatterProgressReportDataRow(dataRow, row, styles);
    }

    return headers;
  }

  /**
   * 创建项目进度报表单行数据
   *
   * @param dataRow 数据行
   * @param row 行数据
   * @param styles 样式对象
   */
  private void createMatterProgressReportDataRow(
      final Row dataRow, final Map<String, Object> row, final MatterProgressReportStyles styles) {
    int colNum = 0;

    // 项目编号
    Cell matterNoCell = dataRow.createCell(colNum++);
    if (row.get("matter_no") != null) {
      matterNoCell.setCellValue(row.get("matter_no").toString());
    }
    matterNoCell.setCellStyle(styles.dataStyle);

    // 项目名称
    Cell matterNameCell = dataRow.createCell(colNum++);
    if (row.get("matter_name") != null) {
      matterNameCell.setCellValue(row.get("matter_name").toString());
    }
    matterNameCell.setCellStyle(styles.dataStyle);

    // 客户名称
    Cell clientNameCell = dataRow.createCell(colNum++);
    if (row.get("client_name") != null) {
      clientNameCell.setCellValue(row.get("client_name").toString());
    }
    clientNameCell.setCellStyle(styles.dataStyle);

    // 业务类型
    Cell businessTypeCell = dataRow.createCell(colNum++);
    if (row.get("business_type") != null) {
      businessTypeCell.setCellValue(row.get("business_type").toString());
    }
    businessTypeCell.setCellStyle(styles.dataStyle);

    // 状态
    Cell statusCell = dataRow.createCell(colNum++);
    if (row.get("status") != null) {
      statusCell.setCellValue(row.get("status").toString());
    }
    statusCell.setCellStyle(styles.dataStyle);

    // 主办律师
    Cell leadLawyerCell = dataRow.createCell(colNum++);
    if (row.get("lead_lawyer_name") != null) {
      leadLawyerCell.setCellValue(row.get("lead_lawyer_name").toString());
    }
    leadLawyerCell.setCellStyle(styles.dataStyle);

    // 总任务数
    Cell totalTasksCell = dataRow.createCell(colNum++);
    if (row.get("total_tasks") != null) {
      if (row.get("total_tasks") instanceof Number) {
        totalTasksCell.setCellValue(((Number) row.get("total_tasks")).doubleValue());
      } else {
        totalTasksCell.setCellValue(row.get("total_tasks").toString());
      }
    }
    totalTasksCell.setCellStyle(styles.dataStyle);

    // 已完成任务数
    Cell completedTasksCell = dataRow.createCell(colNum++);
    if (row.get("completed_tasks") != null) {
      if (row.get("completed_tasks") instanceof Number) {
        completedTasksCell.setCellValue(((Number) row.get("completed_tasks")).doubleValue());
      } else {
        completedTasksCell.setCellValue(row.get("completed_tasks").toString());
      }
    }
    completedTasksCell.setCellStyle(styles.dataStyle);

    // 进行中任务数
    Cell inProgressTasksCell = dataRow.createCell(colNum++);
    if (row.get("in_progress_tasks") != null) {
      if (row.get("in_progress_tasks") instanceof Number) {
        inProgressTasksCell.setCellValue(((Number) row.get("in_progress_tasks")).doubleValue());
      } else {
        inProgressTasksCell.setCellValue(row.get("in_progress_tasks").toString());
      }
    }
    inProgressTasksCell.setCellStyle(styles.dataStyle);

    // 任务完成率
    Cell completionRateCell = dataRow.createCell(colNum++);
    if (row.get("task_completion_rate") != null) {
      if (row.get("task_completion_rate") instanceof Number) {
        double rate = ((Number) row.get("task_completion_rate")).doubleValue();
        completionRateCell.setCellValue(rate / 100.0); // 转换为小数
        completionRateCell.setCellStyle(styles.percentStyle);
      } else {
        completionRateCell.setCellValue(row.get("task_completion_rate").toString());
        completionRateCell.setCellStyle(styles.dataStyle);
      }
    }

    // 总工时
    Cell totalHoursCell = dataRow.createCell(colNum++);
    if (row.get("total_hours") != null) {
      if (row.get("total_hours") instanceof Number) {
        totalHoursCell.setCellValue(((Number) row.get("total_hours")).doubleValue());
      } else {
        totalHoursCell.setCellValue(row.get("total_hours").toString());
      }
    }
    totalHoursCell.setCellStyle(styles.numberStyle);

    // 已审批工时
    Cell approvedHoursCell = dataRow.createCell(colNum++);
    if (row.get("approved_hours") != null) {
      if (row.get("approved_hours") instanceof Number) {
        approvedHoursCell.setCellValue(((Number) row.get("approved_hours")).doubleValue());
      } else {
        approvedHoursCell.setCellValue(row.get("approved_hours").toString());
      }
    }
    approvedHoursCell.setCellStyle(styles.numberStyle);

    // 创建日期
    Cell createdDateCell = dataRow.createCell(colNum++);
    if (row.get("created_date") != null) {
      createdDateCell.setCellValue(row.get("created_date").toString());
    }
    createdDateCell.setCellStyle(styles.dataStyle);

    // 预计完成日期
    Cell expectedEndDateCell = dataRow.createCell(colNum++);
    if (row.get("expected_end_date") != null) {
      expectedEndDateCell.setCellValue(row.get("expected_end_date").toString());
    }
    expectedEndDateCell.setCellStyle(styles.dataStyle);

    // 实际完成日期
    Cell actualEndDateCell = dataRow.createCell(colNum++);
    if (row.get("actual_end_date") != null) {
      actualEndDateCell.setCellValue(row.get("actual_end_date").toString());
    }
    actualEndDateCell.setCellStyle(styles.dataStyle);

    // 预估收费
    Cell estimatedFeeCell = dataRow.createCell(colNum++);
    if (row.get("estimated_fee") != null) {
      if (row.get("estimated_fee") instanceof Number) {
        estimatedFeeCell.setCellValue(((Number) row.get("estimated_fee")).doubleValue());
      } else {
        estimatedFeeCell.setCellValue(row.get("estimated_fee").toString());
      }
    }
    estimatedFeeCell.setCellStyle(styles.numberStyle);
  }

  /**
   * 自动调整列宽
   *
   * @param sheet 工作表
   * @param headers 表头数组
   */
  private void autoSizeColumns(final Sheet sheet, final String[] headers) {
    for (int i = 0; i < headers.length; i++) {
      sheet.autoSizeColumn(i);
      sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
    }
  }

  /**
   * 生成项目工时报表（M3-026）.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateMatterTimesheetReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("项目工时报表");

    CellStyle titleStyle = createTitleStyle(workbook);
    CellStyle headerStyle = createHeaderStyle(workbook);
    CellStyle numberStyle = createNumberStyle(workbook);

    int rowNum = 0;
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("项目工时报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9));

    rowNum++;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {"项目编号", "项目名称", "客户名称", "工作日期", "工作类型", "工作内容", "工时", "是否计费", "状态", "工作人员"};
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_no")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("client_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("work_date")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("work_type")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("work_content")));
      Cell hoursCell = dataRow.createCell(colNum++);
      if (row.get("hours") instanceof Number) {
        hoursCell.setCellValue(((Number) row.get("hours")).doubleValue());
        hoursCell.setCellStyle(numberStyle);
      } else {
        hoursCell.setCellValue(getStringValue(row.get("hours")));
      }
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("billable")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("timesheet_status")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("worker_name")));
    }

    autoSizeColumns(sheet, headers.length);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成项目任务报表（M3-027）.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateMatterTaskReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("项目任务报表");

    CellStyle titleStyle = createTitleStyle(workbook);
    CellStyle headerStyle = createHeaderStyle(workbook);

    int rowNum = 0;
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("项目任务报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10));

    rowNum++;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {
      "项目编号", "项目名称", "客户名称", "任务编号", "任务标题", "优先级", "状态", "进度(%)", "截止日期", "完成时间", "执行人"
    };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_no")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("client_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("task_no")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("task_title")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("priority")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("task_status_name")));
      Cell progressCell = dataRow.createCell(colNum++);
      if (row.get("progress") instanceof Number) {
        progressCell.setCellValue(((Number) row.get("progress")).doubleValue());
      } else {
        progressCell.setCellValue(getStringValue(row.get("progress")));
      }
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("due_date")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("completed_at")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("assignee_name")));
    }

    autoSizeColumns(sheet, headers.length);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成项目阶段进度报表（M3-028）.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateMatterStageReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("项目阶段进度报表");

    CellStyle titleStyle = createTitleStyle(workbook);
    CellStyle headerStyle = createHeaderStyle(workbook);
    CellStyle percentStyle = createPercentStyle(workbook);

    int rowNum = 0;
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("项目阶段进度报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(
        new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, MERGED_REGION_COLUMNS_12));

    rowNum++;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {
      "项目编号", "项目名称", "客户名称", "业务类型", "状态", "主办律师", "部门", "创建日期", "预计完成", "实际完成", "总任务", "已完成",
      "完成率(%)"
    };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_no")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("client_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("business_type")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("progress_status")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("lead_lawyer_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("department_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("created_date")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("expected_end_date")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("actual_end_date")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("total_tasks")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("completed_tasks")));
      Cell rateCell = dataRow.createCell(colNum++);
      if (row.get("completion_rate") instanceof Number) {
        double rate = ((Number) row.get("completion_rate")).doubleValue();
        rateCell.setCellValue(rate / 100.0);
        rateCell.setCellStyle(percentStyle);
      } else {
        rateCell.setCellValue(getStringValue(row.get("completion_rate")));
      }
    }

    autoSizeColumns(sheet, headers.length);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成项目趋势分析报表（M3-029）.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateMatterTrendReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("项目趋势分析报表");

    CellStyle titleStyle = createTitleStyle(workbook);
    CellStyle headerStyle = createHeaderStyle(workbook);
    CellStyle numberStyle = createNumberStyle(workbook);

    int rowNum = 0;
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("项目趋势分析报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

    rowNum++;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {"月份", "新增项目数", "完成项目数", "进行中项目数", "平均周期(天)"};
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("period")));
      Cell newCell = dataRow.createCell(colNum++);
      if (row.get("new_matters_count") instanceof Number) {
        newCell.setCellValue(((Number) row.get("new_matters_count")).doubleValue());
        newCell.setCellStyle(numberStyle);
      } else {
        newCell.setCellValue(getStringValue(row.get("new_matters_count")));
      }
      Cell closedCell = dataRow.createCell(colNum++);
      if (row.get("closed_matters_count") instanceof Number) {
        closedCell.setCellValue(((Number) row.get("closed_matters_count")).doubleValue());
        closedCell.setCellStyle(numberStyle);
      } else {
        closedCell.setCellValue(getStringValue(row.get("closed_matters_count")));
      }
      Cell activeCell = dataRow.createCell(colNum++);
      if (row.get("active_matters_count") instanceof Number) {
        activeCell.setCellValue(((Number) row.get("active_matters_count")).doubleValue());
        activeCell.setCellStyle(numberStyle);
      } else {
        activeCell.setCellValue(getStringValue(row.get("active_matters_count")));
      }
      Cell avgCell = dataRow.createCell(colNum++);
      if (row.get("avg_duration_days") instanceof Number) {
        avgCell.setCellValue(((Number) row.get("avg_duration_days")).doubleValue());
        avgCell.setCellStyle(numberStyle);
      } else {
        avgCell.setCellValue(getStringValue(row.get("avg_duration_days")));
      }
    }

    autoSizeColumns(sheet, headers.length);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 创建标题样式.
   *
   * @param workbook 工作簿对象
   * @return 标题样式
   */
  private CellStyle createTitleStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 16);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    return style;
  }

  /**
   * 创建表头样式.
   *
   * @param workbook 工作簿对象
   * @return 表头样式
   */
  private CellStyle createHeaderStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    return style;
  }

  /**
   * 创建数据样式.
   *
   * @param workbook 工作簿对象
   * @return 数据样式
   */
  @SuppressWarnings("unused")
  private CellStyle createDataStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.LEFT);
    return style;
  }

  /**
   * 创建数字样式.
   *
   * @param workbook 工作簿对象
   * @return 数字样式
   */
  private CellStyle createNumberStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    style.setDataFormat(format.getFormat("#,##0.00"));
    return style;
  }

  /**
   * 创建百分比样式.
   *
   * @param workbook 工作簿对象
   * @return 百分比样式
   */
  private CellStyle createPercentStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    style.setDataFormat(format.getFormat("0.00%"));
    return style;
  }

  /**
   * 自动调整列宽.
   *
   * @param sheet 工作表对象
   * @param columnCount 列数
   */
  private void autoSizeColumns(final Sheet sheet, final int columnCount) {
    for (int i = 0; i < columnCount; i++) {
      sheet.autoSizeColumn(i);
      sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
    }
  }

  /**
   * 生成项目成本分析报表（M4-044）.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateCostAnalysisReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("项目成本分析报表");

    CellStyle titleStyle = createTitleStyle(workbook);
    CellStyle headerStyle = createHeaderStyle(workbook);
    CellStyle numberStyle = createNumberStyle(workbook);
    CellStyle percentStyle = createPercentStyle(workbook);

    int rowNum = 0;
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("项目成本分析报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10));

    rowNum++;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {
      "项目编号", "项目名称", "客户名称", "主办律师", "归集成本", "分摊成本", "总成本", "合同金额", "已收款", "利润", "利润率(%)"
    };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_no")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("client_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("lead_lawyer_name")));

      // 归集成本
      Cell allocatedCell = dataRow.createCell(colNum++);
      if (row.get("allocated_cost") instanceof Number) {
        allocatedCell.setCellValue(((Number) row.get("allocated_cost")).doubleValue());
        allocatedCell.setCellStyle(numberStyle);
      } else {
        allocatedCell.setCellValue(getStringValue(row.get("allocated_cost")));
      }

      // 分摊成本
      Cell splitCell = dataRow.createCell(colNum++);
      if (row.get("split_cost") instanceof Number) {
        splitCell.setCellValue(((Number) row.get("split_cost")).doubleValue());
        splitCell.setCellStyle(numberStyle);
      } else {
        splitCell.setCellValue(getStringValue(row.get("split_cost")));
      }

      // 总成本
      Cell totalCell = dataRow.createCell(colNum++);
      if (row.get("total_cost") instanceof Number) {
        totalCell.setCellValue(((Number) row.get("total_cost")).doubleValue());
        totalCell.setCellStyle(numberStyle);
      } else {
        totalCell.setCellValue(getStringValue(row.get("total_cost")));
      }

      // 合同金额
      Cell contractCell = dataRow.createCell(colNum++);
      if (row.get("contract_amount") instanceof Number) {
        contractCell.setCellValue(((Number) row.get("contract_amount")).doubleValue());
        contractCell.setCellStyle(numberStyle);
      } else {
        contractCell.setCellValue(getStringValue(row.get("contract_amount")));
      }

      // 已收款
      Cell receivedCell = dataRow.createCell(colNum++);
      if (row.get("received_amount") instanceof Number) {
        receivedCell.setCellValue(((Number) row.get("received_amount")).doubleValue());
        receivedCell.setCellStyle(numberStyle);
      } else {
        receivedCell.setCellValue(getStringValue(row.get("received_amount")));
      }

      // 利润
      Cell profitCell = dataRow.createCell(colNum++);
      if (row.get("profit") instanceof Number) {
        profitCell.setCellValue(((Number) row.get("profit")).doubleValue());
        profitCell.setCellStyle(numberStyle);
      } else {
        profitCell.setCellValue(getStringValue(row.get("profit")));
      }

      // 利润率
      Cell rateCell = dataRow.createCell(colNum++);
      if (row.get("profit_rate") instanceof Number) {
        double rate = ((Number) row.get("profit_rate")).doubleValue();
        rateCell.setCellValue(rate / 100.0);
        rateCell.setCellStyle(percentStyle);
      } else {
        rateCell.setCellValue(getStringValue(row.get("profit_rate")));
      }
    }

    autoSizeColumns(sheet, headers.length);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成应收账款账龄分析报表（M4-053）.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateAgingAnalysisReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("应收账款账龄分析报表");

    CellStyle titleStyle = createTitleStyle(workbook);
    CellStyle headerStyle = createHeaderStyle(workbook);
    CellStyle numberStyle = createNumberStyle(workbook);

    int rowNum = 0;
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("应收账款账龄分析报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9));

    rowNum++;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {
      "客户名称", "项目名称", "项目编号", "合同编号", "合同金额", "已收款", "应收金额", "账龄天数", "账龄区间", "签署日期"
    };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("client_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_no")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("contract_no")));

      // 合同金额
      Cell contractCell = dataRow.createCell(colNum++);
      if (row.get("contract_amount") instanceof Number) {
        contractCell.setCellValue(((Number) row.get("contract_amount")).doubleValue());
        contractCell.setCellStyle(numberStyle);
      } else {
        contractCell.setCellValue(getStringValue(row.get("contract_amount")));
      }

      // 已收款
      Cell receivedCell = dataRow.createCell(colNum++);
      if (row.get("received_amount") instanceof Number) {
        receivedCell.setCellValue(((Number) row.get("received_amount")).doubleValue());
        receivedCell.setCellStyle(numberStyle);
      } else {
        receivedCell.setCellValue(getStringValue(row.get("received_amount")));
      }

      // 应收金额
      Cell receivableCell = dataRow.createCell(colNum++);
      if (row.get("receivable_amount") instanceof Number) {
        receivableCell.setCellValue(((Number) row.get("receivable_amount")).doubleValue());
        receivableCell.setCellStyle(numberStyle);
      } else {
        receivableCell.setCellValue(getStringValue(row.get("receivable_amount")));
      }

      // 账龄天数
      Cell agingCell = dataRow.createCell(colNum++);
      if (row.get("aging_days") instanceof Number) {
        agingCell.setCellValue(((Number) row.get("aging_days")).doubleValue());
        agingCell.setCellStyle(numberStyle);
      } else {
        agingCell.setCellValue(getStringValue(row.get("aging_days")));
      }

      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("aging_range")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("sign_date")));
    }

    autoSizeColumns(sheet, headers.length);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成项目利润分析报表（M4-054）.
   *
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream generateProfitAnalysisReport(final List<Map<String, Object>> data)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("项目利润分析报表");

    CellStyle titleStyle = createTitleStyle(workbook);
    CellStyle headerStyle = createHeaderStyle(workbook);
    CellStyle numberStyle = createNumberStyle(workbook);
    CellStyle percentStyle = createPercentStyle(workbook);

    int rowNum = 0;
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("项目利润分析报表");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(
        new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, MERGED_REGION_COLUMNS_14));

    rowNum++;
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {
      "项目编号",
      "项目名称",
      "客户名称",
      "主办律师",
      "业务类型",
      "状态",
      "合同金额",
      "已收款",
      "应收金额",
      "归集成本",
      "分摊成本",
      "总成本",
      "利润",
      "利润率(已收款)",
      "利润率(合同)"
    };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (Map<String, Object> row : data) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_no")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("matter_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("client_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("lead_lawyer_name")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("business_type")));
      dataRow.createCell(colNum++).setCellValue(getStringValue(row.get("status")));

      // 合同金额
      Cell contractCell = dataRow.createCell(colNum++);
      if (row.get("contract_amount") instanceof Number) {
        contractCell.setCellValue(((Number) row.get("contract_amount")).doubleValue());
        contractCell.setCellStyle(numberStyle);
      } else {
        contractCell.setCellValue(getStringValue(row.get("contract_amount")));
      }

      // 已收款
      Cell receivedCell = dataRow.createCell(colNum++);
      if (row.get("received_amount") instanceof Number) {
        receivedCell.setCellValue(((Number) row.get("received_amount")).doubleValue());
        receivedCell.setCellStyle(numberStyle);
      } else {
        receivedCell.setCellValue(getStringValue(row.get("received_amount")));
      }

      // 应收金额
      Cell receivableCell = dataRow.createCell(colNum++);
      if (row.get("receivable_amount") instanceof Number) {
        receivableCell.setCellValue(((Number) row.get("receivable_amount")).doubleValue());
        receivableCell.setCellStyle(numberStyle);
      } else {
        receivableCell.setCellValue(getStringValue(row.get("receivable_amount")));
      }

      // 归集成本
      Cell allocatedCell = dataRow.createCell(colNum++);
      if (row.get("allocated_cost") instanceof Number) {
        allocatedCell.setCellValue(((Number) row.get("allocated_cost")).doubleValue());
        allocatedCell.setCellStyle(numberStyle);
      } else {
        allocatedCell.setCellValue(getStringValue(row.get("allocated_cost")));
      }

      // 分摊成本
      Cell splitCell = dataRow.createCell(colNum++);
      if (row.get("split_cost") instanceof Number) {
        splitCell.setCellValue(((Number) row.get("split_cost")).doubleValue());
        splitCell.setCellStyle(numberStyle);
      } else {
        splitCell.setCellValue(getStringValue(row.get("split_cost")));
      }

      // 总成本
      Cell totalCostCell = dataRow.createCell(colNum++);
      if (row.get("total_cost") instanceof Number) {
        totalCostCell.setCellValue(((Number) row.get("total_cost")).doubleValue());
        totalCostCell.setCellStyle(numberStyle);
      } else {
        totalCostCell.setCellValue(getStringValue(row.get("total_cost")));
      }

      // 利润
      Cell profitCell = dataRow.createCell(colNum++);
      if (row.get("profit") instanceof Number) {
        profitCell.setCellValue(((Number) row.get("profit")).doubleValue());
        profitCell.setCellStyle(numberStyle);
      } else {
        profitCell.setCellValue(getStringValue(row.get("profit")));
      }

      // 利润率(已收款)
      Cell rateReceivedCell = dataRow.createCell(colNum++);
      if (row.get("profit_rate_on_received") instanceof Number) {
        double rate = ((Number) row.get("profit_rate_on_received")).doubleValue();
        rateReceivedCell.setCellValue(rate / 100.0);
        rateReceivedCell.setCellStyle(percentStyle);
      } else {
        rateReceivedCell.setCellValue(getStringValue(row.get("profit_rate_on_received")));
      }

      // 利润率(合同)
      Cell rateContractCell = dataRow.createCell(colNum++);
      if (row.get("profit_rate_on_contract") instanceof Number) {
        double rate = ((Number) row.get("profit_rate_on_contract")).doubleValue();
        rateContractCell.setCellValue(rate / 100.0);
        rateContractCell.setCellStyle(percentStyle);
      } else {
        rateContractCell.setCellValue(getStringValue(row.get("profit_rate_on_contract")));
      }
    }

    autoSizeColumns(sheet, headers.length);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 获取字符串值.
   *
   * @param value 对象值
   * @return 字符串值
   */
  private String getStringValue(final Object value) {
    if (value == null) {
      return "";
    }
    return value.toString();
  }
}
