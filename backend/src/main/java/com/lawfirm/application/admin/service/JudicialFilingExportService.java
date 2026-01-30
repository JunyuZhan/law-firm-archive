package com.lawfirm.application.admin.service;

import com.lawfirm.application.admin.dto.AdminContractViewDTO;
import com.lawfirm.domain.system.entity.ExportLog;
import com.lawfirm.domain.system.repository.ExportLogRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
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
import org.springframework.stereotype.Service;

/**
 * 司法局报备导出服务
 *
 * <p>Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudicialFilingExportService {

  /** ContractQuery Service. */
  private final AdminContractQueryService contractQueryService;

  /** ExportLog Repository. */
  private final ExportLogRepository exportLogRepository;

  /** Excel列最大宽度 */
  private static final int MAX_COLUMN_WIDTH = 15000;

  /** Excel列宽度增量 */
  private static final int COLUMN_WIDTH_INCREMENT = 1000;

  /** Excel列默认宽度 */
  private static final int DEFAULT_COLUMN_WIDTH = 5000;

  /**
   * 按月导出收案清单
   *
   * <p>Requirements: 6.1, 6.2, 6.3
   *
   * @param year 年份
   * @param month 月份
   * @param operatorId 操作人ID
   * @return Excel文件流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream exportMonthlyFilingReport(
      final int year, final int month, final Long operatorId) throws IOException {
    List<AdminContractViewDTO> contracts = contractQueryService.listContractsByMonth(year, month);

    ByteArrayInputStream result = generateExcel(contracts, null);

    // 记录导出日志 (Requirement 6.7)
    logExport(year, month, contracts.size(), operatorId);

    return result;
  }

  /**
   * 按月导出收案清单（自定义字段）
   *
   * <p>Requirements: 6.6
   *
   * @param year 年份
   * @param month 月份
   * @param customFields 自定义字段集合
   * @param operatorId 操作人ID
   * @return Excel文件流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream exportMonthlyFilingReport(
      final int year, final int month, final Set<String> customFields, final Long operatorId)
      throws IOException {
    List<AdminContractViewDTO> contracts = contractQueryService.listContractsByMonth(year, month);

    ByteArrayInputStream result = generateExcel(contracts, customFields);

    // 记录导出日志 (Requirement 6.7)
    logExport(year, month, contracts.size(), operatorId);

    return result;
  }

  /**
   * 导出合同列表（根据查询条件） 用于导出当前查询结果到Excel
   *
   * @param contracts 合同列表
   * @param operatorId 操作人ID
   * @return Excel文件流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream exportContractList(
      final List<AdminContractViewDTO> contracts, final Long operatorId) throws IOException {
    ByteArrayInputStream result = generateContractListExcel(contracts);

    // 记录导出日志
    ExportLog exportLog = new ExportLog();
    exportLog.setExportType("CONTRACT_LIST");
    exportLog.setFileName("合同列表导出");
    exportLog.setRecordCount(contracts.size());
    exportLog.setExportedBy(operatorId);
    exportLog.setExportedAt(LocalDateTime.now());
    exportLogRepository.save(exportLog);

    log.info("合同列表导出完成: 共 {} 条记录, 操作人: {}", contracts.size(), operatorId);
    return result;
  }

  /**
   * 生成合同列表Excel文件
   *
   * @param contracts 合同列表
   * @return Excel文件流
   * @throws IOException IO异常
   */
  private ByteArrayInputStream generateContractListExcel(final List<AdminContractViewDTO> contracts)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("合同列表");

    // 创建样式
    CellStyle headerStyle = createHeaderStyle(workbook);
    CellStyle dataStyle = createDataStyle(workbook);
    CellStyle numberStyle = createNumberStyle(workbook);
    CellStyle dateStyle = createDateStyle(workbook);

    int rowNum = 0;

    // 表头
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers =
        new String[] {
          "序号", "合同编号", "合同名称", "委托人", "对方当事人", "案件类型", "案由", "承办律师", "律师费金额", "签约日期", "管辖法院",
          "审理阶段", "状态"
        };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    // 数据行
    int seq = 1;
    for (AdminContractViewDTO contract : contracts) {
      Row dataRow = sheet.createRow(rowNum++);
      int colNum = 0;

      // 序号
      Cell seqCell = dataRow.createCell(colNum++);
      seqCell.setCellValue(seq++);
      seqCell.setCellStyle(dataStyle);

      // 合同编号
      Cell contractNoCell = dataRow.createCell(colNum++);
      contractNoCell.setCellValue(nullSafe(contract.getContractNo()));
      contractNoCell.setCellStyle(dataStyle);

      // 合同名称
      Cell nameCell = dataRow.createCell(colNum++);
      nameCell.setCellValue(nullSafe(contract.getName()));
      nameCell.setCellStyle(dataStyle);

      // 委托人
      Cell clientCell = dataRow.createCell(colNum++);
      clientCell.setCellValue(nullSafe(contract.getClientName()));
      clientCell.setCellStyle(dataStyle);

      // 对方当事人
      Cell opposingCell = dataRow.createCell(colNum++);
      opposingCell.setCellValue(nullSafe(contract.getOpposingParty()));
      opposingCell.setCellStyle(dataStyle);

      // 案件类型
      Cell caseTypeCell = dataRow.createCell(colNum++);
      caseTypeCell.setCellValue(nullSafe(contract.getCaseTypeName()));
      caseTypeCell.setCellStyle(dataStyle);

      // 案由（使用案由名称，如果没有则用代码）
      Cell causeCell = dataRow.createCell(colNum++);
      causeCell.setCellValue(
          nullSafe(
              contract.getCauseOfActionName() != null
                  ? contract.getCauseOfActionName()
                  : contract.getCauseOfAction()));
      causeCell.setCellStyle(dataStyle);

      // 承办律师
      Cell lawyerCell = dataRow.createCell(colNum++);
      lawyerCell.setCellValue(nullSafe(contract.getLeadLawyerName()));
      lawyerCell.setCellStyle(dataStyle);

      // 律师费金额
      Cell amountCell = dataRow.createCell(colNum++);
      if (contract.getTotalAmount() != null) {
        amountCell.setCellValue(contract.getTotalAmount().doubleValue());
        amountCell.setCellStyle(numberStyle);
      } else {
        amountCell.setCellStyle(dataStyle);
      }

      // 签约日期
      Cell signDateCell = dataRow.createCell(colNum++);
      if (contract.getSignDate() != null) {
        signDateCell.setCellValue(
            contract.getSignDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      }
      signDateCell.setCellStyle(dateStyle);

      // 管辖法院
      Cell courtCell = dataRow.createCell(colNum++);
      courtCell.setCellValue(nullSafe(contract.getJurisdictionCourt()));
      courtCell.setCellStyle(dataStyle);

      // 审理阶段
      Cell stageCell = dataRow.createCell(colNum++);
      stageCell.setCellValue(nullSafe(contract.getTrialStageName()));
      stageCell.setCellStyle(dataStyle);

      // 状态
      Cell statusCell = dataRow.createCell(colNum++);
      statusCell.setCellValue(getContractStatusName(contract.getStatus()));
      statusCell.setCellStyle(dataStyle);
    }

    // 自动调整列宽
    for (int i = 0; i < headers.length; i++) {
      try {
        sheet.autoSizeColumn(i);
        int width = sheet.getColumnWidth(i);
        sheet.setColumnWidth(i, Math.min(width + COLUMN_WIDTH_INCREMENT, MAX_COLUMN_WIDTH));
      } catch (Exception e) {
        log.warn("无法自动调整第 {} 列的宽度: {}", i, e.getMessage());
        sheet.setColumnWidth(i, DEFAULT_COLUMN_WIDTH);
      }
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 生成Excel文件
   *
   * @param contracts 合同列表
   * @param customFields 自定义字段集合
   * @return Excel文件流
   * @throws IOException IO异常
   */
  private ByteArrayInputStream generateExcel(
      final List<AdminContractViewDTO> contracts, final Set<String> customFields)
      throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("收案清单");

    // 创建样式
    CellStyle titleStyle = createTitleStyle(workbook);
    CellStyle headerStyle = createHeaderStyle(workbook);
    CellStyle dataStyle = createDataStyle(workbook);
    CellStyle numberStyle = createNumberStyle(workbook);
    CellStyle dateStyle = createDateStyle(workbook);

    int rowNum = 0;

    // 标题行
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("收案清单（司法局报备）");
    titleCell.setCellStyle(titleStyle);

    // 空行
    rowNum++;

    // 表头 (Requirement 6.3)
    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = getHeaders(customFields);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    // 合并标题单元格
    if (headers.length > 1) {
      sheet.addMergedRegion(
          new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    // 数据行
    int seq = 1;
    for (AdminContractViewDTO contract : contracts) {
      Row dataRow = sheet.createRow(rowNum++);
      fillDataRow(dataRow, contract, seq++, customFields, dataStyle, numberStyle, dateStyle);
    }

    // 自动调整列宽
    for (int i = 0; i < headers.length; i++) {
      try {
        sheet.autoSizeColumn(i);
        int width = sheet.getColumnWidth(i);
        sheet.setColumnWidth(i, Math.min(width + COLUMN_WIDTH_INCREMENT, MAX_COLUMN_WIDTH));
      } catch (Exception e) {
        log.warn("无法自动调整第 {} 列的宽度: {}", i, e.getMessage());
        sheet.setColumnWidth(i, DEFAULT_COLUMN_WIDTH); // 发生异常时使用默认宽度
      }
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  /**
   * 获取表头列
   *
   * <p>Requirements: 6.3, 6.4, 6.5, 6.6
   *
   * @param customFields 自定义字段集合
   * @return 表头数组
   */
  private String[] getHeaders(final Set<String> customFields) {
    if (customFields != null && !customFields.isEmpty()) {
      return customFields.toArray(new String[0]);
    }
    // 默认表头：基本字段 + 诉讼类型字段
    return new String[] {
      "序号", "合同编号", "委托人", "对方当事人", "案件类型", "案由",
      "承办律师", "律师费金额", "签约日期", "案号", "管辖法院", "代理权限"
    };
  }

  /**
   * 填充数据行
   *
   * @param row Excel行对象
   * @param contract 合同DTO
   * @param seq 序号
   * @param customFields 自定义字段集合
   * @param dataStyle 数据样式
   * @param numberStyle 数字样式
   * @param dateStyle 日期样式
   */
  private void fillDataRow(
      final Row row,
      final AdminContractViewDTO contract,
      final int seq,
      final Set<String> customFields,
      final CellStyle dataStyle,
      final CellStyle numberStyle,
      final CellStyle dateStyle) {
    int colNum = 0;

    if (customFields == null || customFields.isEmpty()) {
      // 默认字段
      // 序号
      Cell seqCell = row.createCell(colNum++);
      seqCell.setCellValue(seq);
      seqCell.setCellStyle(dataStyle);

      // 合同编号
      Cell contractNoCell = row.createCell(colNum++);
      contractNoCell.setCellValue(nullSafe(contract.getContractNo()));
      contractNoCell.setCellStyle(dataStyle);

      // 委托人
      Cell clientCell = row.createCell(colNum++);
      clientCell.setCellValue(nullSafe(contract.getClientName()));
      clientCell.setCellStyle(dataStyle);

      // 对方当事人
      Cell opposingCell = row.createCell(colNum++);
      opposingCell.setCellValue(nullSafe(contract.getOpposingParty()));
      opposingCell.setCellStyle(dataStyle);

      // 案件类型
      Cell caseTypeCell = row.createCell(colNum++);
      caseTypeCell.setCellValue(nullSafe(contract.getCaseTypeName()));
      caseTypeCell.setCellStyle(dataStyle);

      // 案由（使用案由名称，如果没有则用代码）
      Cell causeCell = row.createCell(colNum++);
      causeCell.setCellValue(
          nullSafe(
              contract.getCauseOfActionName() != null
                  ? contract.getCauseOfActionName()
                  : contract.getCauseOfAction()));
      causeCell.setCellStyle(dataStyle);

      // 承办律师
      Cell lawyerCell = row.createCell(colNum++);
      lawyerCell.setCellValue(nullSafe(contract.getLeadLawyerName()));
      lawyerCell.setCellStyle(dataStyle);

      // 律师费金额
      Cell amountCell = row.createCell(colNum++);
      if (contract.getTotalAmount() != null) {
        amountCell.setCellValue(contract.getTotalAmount().doubleValue());
        amountCell.setCellStyle(numberStyle);
      } else {
        amountCell.setCellStyle(dataStyle);
      }

      // 签约日期
      Cell signDateCell = row.createCell(colNum++);
      if (contract.getSignDate() != null) {
        signDateCell.setCellValue(
            contract.getSignDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      }
      signDateCell.setCellStyle(dateStyle);

      // 诉讼类型额外字段 (Requirement 6.4)
      // 案号
      Cell caseNoCell = row.createCell(colNum++);
      // 案号字段暂未在DTO中定义，留空
      caseNoCell.setCellStyle(dataStyle);

      // 管辖法院
      Cell courtCell = row.createCell(colNum++);
      courtCell.setCellValue(nullSafe(contract.getJurisdictionCourt()));
      courtCell.setCellStyle(dataStyle);

      // 代理权限
      Cell authorityCell = row.createCell(colNum++);
      // 代理权限字段暂未在DTO中定义，留空
      authorityCell.setCellStyle(dataStyle);
    } else {
      // 自定义字段 (Requirement 6.6)
      for (String field : customFields) {
        Cell cell = row.createCell(colNum++);
        cell.setCellValue(getFieldValue(contract, field, seq));
        cell.setCellStyle(dataStyle);
      }
    }
  }

  /**
   * 根据字段名获取值
   *
   * @param contract 合同DTO
   * @param field 字段名
   * @param seq 序号
   * @return 字段值
   */
  private String getFieldValue(
      final AdminContractViewDTO contract, final String field, final int seq) {
    return switch (field) {
      case "序号" -> String.valueOf(seq);
      case "合同编号" -> nullSafe(contract.getContractNo());
      case "委托人" -> nullSafe(contract.getClientName());
      case "对方当事人" -> nullSafe(contract.getOpposingParty());
      case "案件类型" -> nullSafe(contract.getCaseTypeName());
      case "案由" -> nullSafe(
          contract.getCauseOfActionName() != null
              ? contract.getCauseOfActionName()
              : contract.getCauseOfAction());
      case "承办律师" -> nullSafe(contract.getLeadLawyerName());
      case "律师费金额" -> contract.getTotalAmount() != null ? contract.getTotalAmount().toString() : "";
      case "签约日期" -> contract.getSignDate() != null
          ? contract.getSignDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          : "";
      case "管辖法院" -> nullSafe(contract.getJurisdictionCourt());
      case "审理阶段" -> nullSafe(contract.getTrialStage());
      default -> "";
    };
  }

  /**
   * 记录导出日志
   *
   * <p>Requirement: 6.7
   *
   * @param year 年份
   * @param month 月份
   * @param recordCount 记录数
   * @param operatorId 操作人ID
   */
  private void logExport(
      final int year, final int month, final int recordCount, final Long operatorId) {
    ExportLog exportLog =
        ExportLog.builder()
            .exportType("JUDICIAL_FILING")
            .exportParams(String.format("{\"year\":%d,\"month\":%d}", year, month))
            .recordCount(recordCount)
            .exportedBy(operatorId)
            .exportedAt(LocalDateTime.now())
            .fileName(String.format("收案清单_%d年%d月.xlsx", year, month))
            .build();
    exportLogRepository.save(exportLog);
    log.info("司法局报备导出完成: year={}, month={}, recordCount={}", year, month, recordCount);
  }

  private String nullSafe(final String value) {
    return value != null ? value : "";
  }

  /**
   * 获取合同状态名称
   *
   * @param status 合同状态
   * @return 状态名称
   */
  private String getContractStatusName(final String status) {
    if (status == null) {
      return "";
    }
    return switch (status) {
      case "DRAFT" -> "草稿";
      case "PENDING" -> "待审批";
      case "ACTIVE" -> "生效中";
      case "EXPIRED" -> "已过期";
      case "TERMINATED" -> "已终止";
      case "COMPLETED" -> "已完成";
      default -> status;
    };
  }

  private CellStyle createTitleStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 16);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    return style;
  }

  private CellStyle createHeaderStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private CellStyle createDataStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.LEFT);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private CellStyle createNumberStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    style.setDataFormat(format.getFormat("#,##0.00"));
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private CellStyle createDateStyle(final Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }
}
