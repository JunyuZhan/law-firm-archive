package com.lawfirm.application.admin.service;

import com.lawfirm.application.admin.dto.AdminContractViewDTO;
import com.lawfirm.domain.system.entity.ExportLog;
import com.lawfirm.domain.system.repository.ExportLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * 司法局报备导出服务
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudicialFilingExportService {

    private final AdminContractQueryService contractQueryService;
    private final ExportLogRepository exportLogRepository;

    /**
     * 按月导出收案清单
     * 
     * Requirements: 6.1, 6.2, 6.3
     */
    public ByteArrayInputStream exportMonthlyFilingReport(int year, int month, Long operatorId) throws IOException {
        List<AdminContractViewDTO> contracts = contractQueryService.listContractsByMonth(year, month);

        ByteArrayInputStream result = generateExcel(contracts, null);

        // 记录导出日志 (Requirement 6.7)
        logExport(year, month, contracts.size(), operatorId);

        return result;
    }

    /**
     * 按月导出收案清单（自定义字段）
     * 
     * Requirements: 6.6
     */
    public ByteArrayInputStream exportMonthlyFilingReport(int year, int month, Set<String> customFields,
            Long operatorId) throws IOException {
        List<AdminContractViewDTO> contracts = contractQueryService.listContractsByMonth(year, month);

        ByteArrayInputStream result = generateExcel(contracts, customFields);

        // 记录导出日志 (Requirement 6.7)
        logExport(year, month, contracts.size(), operatorId);

        return result;
    }

    /**
     * 生成Excel文件
     */
    private ByteArrayInputStream generateExcel(List<AdminContractViewDTO> contracts, Set<String> customFields)
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
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));
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
                sheet.setColumnWidth(i, Math.min(width + 1000, 15000));
            } catch (Exception e) {
                log.warn("无法自动调整第 {} 列的宽度: {}", i, e.getMessage());
                sheet.setColumnWidth(i, 5000); // 发生异常时使用默认宽度
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
     * Requirements: 6.3, 6.4, 6.5, 6.6
     */
    private String[] getHeaders(Set<String> customFields) {
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
     */
    private void fillDataRow(Row row, AdminContractViewDTO contract, int seq,
            Set<String> customFields, CellStyle dataStyle,
            CellStyle numberStyle, CellStyle dateStyle) {
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

            // 案由
            Cell causeCell = row.createCell(colNum++);
            causeCell.setCellValue(nullSafe(contract.getCauseOfAction()));
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
                signDateCell.setCellValue(contract.getSignDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
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
     */
    private String getFieldValue(AdminContractViewDTO contract, String field, int seq) {
        return switch (field) {
            case "序号" -> String.valueOf(seq);
            case "合同编号" -> nullSafe(contract.getContractNo());
            case "委托人" -> nullSafe(contract.getClientName());
            case "对方当事人" -> nullSafe(contract.getOpposingParty());
            case "案件类型" -> nullSafe(contract.getCaseTypeName());
            case "案由" -> nullSafe(contract.getCauseOfAction());
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
     * Requirement: 6.7
     */
    private void logExport(int year, int month, int recordCount, Long operatorId) {
        ExportLog exportLog = ExportLog.builder()
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

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

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
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
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

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
