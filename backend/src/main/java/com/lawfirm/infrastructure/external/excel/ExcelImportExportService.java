package com.lawfirm.infrastructure.external.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel导入导出服务.
 *
 * @author system
 * @since 2026-01-10
 */
@Slf4j
@Service
public class ExcelImportExportService {

  /** 日期格式化器. */
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /** 最小列宽（字符数）. */
  private static final int MIN_COLUMN_WIDTH = 10;

  /** 最大列宽（字符数）. */
  private static final int MAX_COLUMN_WIDTH = 100;

  /** 列宽单位转换因子. */
  private static final int COLUMN_WIDTH_UNIT = 256;

  /**
   * 读取Excel文件为Map列表
   *
   * @param file Excel文件
   * @return Map列表
   * @throws IOException IO异常
   */
  public List<Map<String, Object>> readExcel(final MultipartFile file) throws IOException {
    List<Map<String, Object>> data = new ArrayList<>();

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      final int minRequiredRows = 2;
      if (sheet.getPhysicalNumberOfRows() < minRequiredRows) {
        return data; // 至少需要表头和数据行
      }

      // 读取表头
      Row headerRow = sheet.getRow(0);
      List<String> headers = new ArrayList<>();
      for (Cell cell : headerRow) {
        headers.add(getCellValueAsString(cell));
      }

      // 读取数据行
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        final Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }

        final Map<String, Object> rowData = new java.util.HashMap<>();
        for (int j = 0; j < headers.size() && j < row.getLastCellNum(); j++) {
          final Cell cell = row.getCell(j);
          final String header = headers.get(j);
          final Object value = getCellValue(cell);
          rowData.put(header, value);
        }
        data.add(rowData);
      }
    }

    return data;
  }

  /**
   * 获取单元格值（自动识别类型）
   *
   * @param cell 单元格
   * @return 单元格值
   */
  private Object getCellValue(final Cell cell) {
    if (cell == null) {
      return null;
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue().trim();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getDateCellValue();
        } else {
          double numericValue = cell.getNumericCellValue();
          // 判断是否为整数
          if (numericValue == (long) numericValue) {
            return (long) numericValue;
          } else {
            return numericValue;
          }
        }
      case BOOLEAN:
        return cell.getBooleanCellValue();
      case FORMULA:
        return cell.getCellFormula();
      default:
        return null;
    }
  }

  /**
   * 获取单元格值为字符串
   *
   * @param cell 单元格
   * @return 字符串值
   */
  private String getCellValueAsString(final Cell cell) {
    if (cell == null) {
      return "";
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue().trim();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getDateCellValue().toString();
        } else {
          double numericValue = cell.getNumericCellValue();
          if (numericValue == (long) numericValue) {
            return String.valueOf((long) numericValue);
          } else {
            return String.valueOf(numericValue);
          }
        }
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        return cell.getCellFormula();
      default:
        return "";
    }
  }

  /**
   * 创建Excel文件
   *
   * @param headers 表头列表
   * @param data 数据列表
   * @param sheetName 工作表名称
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream createExcel(
      final List<String> headers, final List<List<Object>> data, final String sheetName)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet(sheetName != null ? sheetName : "Sheet1");

    // 创建表头样式
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);

    // 创建表头
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.size(); i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers.get(i));
      cell.setCellStyle(headerStyle);
    }

    // 创建数据行
    for (int i = 0; i < data.size(); i++) {
      Row row = sheet.createRow(i + 1);
      List<Object> rowData = data.get(i);
      for (int j = 0; j < headers.size() && j < rowData.size(); j++) {
        Cell cell = row.createCell(j);
        Object value = rowData.get(j);
        setCellValue(cell, value);
      }
    }

    // 自动调整列宽
    for (int i = 0; i < headers.size(); i++) {
      sheet.autoSizeColumn(i);
      // 设置最小列宽
      final int columnWidth = sheet.getColumnWidth(i);
      final int minWidth = MIN_COLUMN_WIDTH * COLUMN_WIDTH_UNIT;
      final int maxWidth = MAX_COLUMN_WIDTH * COLUMN_WIDTH_UNIT;
      if (columnWidth < minWidth) {
        sheet.setColumnWidth(i, minWidth);
      } else if (columnWidth > maxWidth) {
        sheet.setColumnWidth(i, maxWidth);
      }
    }

    // 写入ByteArrayOutputStream
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 设置单元格值
   *
   * @param cell 单元格
   * @param value 值
   */
  private void setCellValue(final Cell cell, final Object value) {
    if (value == null) {
      cell.setCellValue("");
      return;
    }

    if (value instanceof String) {
      cell.setCellValue((String) value);
    } else if (value instanceof Number) {
      if (value instanceof Integer || value instanceof Long) {
        cell.setCellValue(((Number) value).doubleValue());
      } else {
        cell.setCellValue(((Number) value).doubleValue());
      }
    } else if (value instanceof Boolean) {
      cell.setCellValue((Boolean) value);
    } else if (value instanceof LocalDate) {
      cell.setCellValue(((LocalDate) value).format(DATE_FORMATTER));
    } else if (value instanceof java.util.Date) {
      cell.setCellValue((java.util.Date) value);
    } else {
      cell.setCellValue(value.toString());
    }
  }
}
