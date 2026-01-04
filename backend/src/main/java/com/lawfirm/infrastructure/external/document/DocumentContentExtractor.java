package com.lawfirm.infrastructure.external.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 文档内容提取服务
 * 支持提取Word、Excel等文档的文本内容
 */
@Slf4j
@Service
public class DocumentContentExtractor {

    /**
     * 提取文档文本内容
     * 
     * @param fileBytes 文件字节数组
     * @param fileName 文件名（用于判断文件类型）
     * @return 提取的文本内容
     */
    public String extractText(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            return "";
        }

        String lowerName = fileName.toLowerCase();
        
        try {
            if (lowerName.endsWith(".docx")) {
                return extractFromDocx(fileBytes);
            } else if (lowerName.endsWith(".doc")) {
                return extractFromDoc(fileBytes);
            } else if (lowerName.endsWith(".xlsx")) {
                return extractFromXlsx(fileBytes);
            } else if (lowerName.endsWith(".xls")) {
                return extractFromXls(fileBytes);
            } else {
                log.warn("不支持的文件类型: {}", fileName);
                return "";
            }
        } catch (Exception e) {
            log.error("提取文档内容失败: {}", fileName, e);
            return "文档内容提取失败: " + e.getMessage();
        }
    }

    /**
     * 提取DOCX文档内容
     */
    private String extractFromDocx(byte[] fileBytes) throws Exception {
        try (InputStream is = new ByteArrayInputStream(fileBytes);
             XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 提取DOC文档内容
     * 注意：需要poi-scratchpad依赖支持
     */
    private String extractFromDoc(byte[] fileBytes) throws Exception {
        // DOC格式需要poi-scratchpad依赖，暂时返回提示
        // 如果需要支持，可以添加依赖并取消注释以下代码
        /*
        try (InputStream is = new ByteArrayInputStream(fileBytes);
             HWPFDocument document = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
        */
        return "DOC格式文档内容提取需要poi-scratchpad依赖支持，当前版本暂不支持。";
    }

    /**
     * 提取XLSX文档内容
     */
    private String extractFromXlsx(byte[] fileBytes) throws Exception {
        StringBuilder text = new StringBuilder();
        try (InputStream is = new ByteArrayInputStream(fileBytes);
             Workbook workbook = new XSSFWorkbook(is)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (i > 0) {
                    text.append("\n\n--- 工作表: ").append(sheet.getSheetName()).append(" ---\n\n");
                }
                
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        String cellValue = getCellValue(cell);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            text.append(cellValue).append("\t");
                        }
                    }
                    text.append("\n");
                }
            }
        }
        return text.toString();
    }

    /**
     * 提取XLS文档内容
     */
    private String extractFromXls(byte[] fileBytes) throws Exception {
        StringBuilder text = new StringBuilder();
        try (InputStream is = new ByteArrayInputStream(fileBytes);
             Workbook workbook = new HSSFWorkbook(is)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (i > 0) {
                    text.append("\n\n--- 工作表: ").append(sheet.getSheetName()).append(" ---\n\n");
                }
                
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        String cellValue = getCellValue(cell);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            text.append(cellValue).append("\t");
                        }
                    }
                    text.append("\n");
                }
            }
        }
        return text.toString();
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 处理日期格式
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法
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
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}

