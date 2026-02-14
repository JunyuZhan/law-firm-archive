package com.archivesystem.service.impl;

import com.archivesystem.service.DocumentConversionService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Set;

/**
 * 文档转换服务实现.
 * 使用 Apache POI + OpenPDF 纯 Java 方案将 Office 文档转换为 PDF
 * 
 * <p>支持转换的格式：
 * <ul>
 *   <li>Word: doc, docx</li>
 *   <li>Excel: xls, xlsx</li>
 *   <li>文本: txt</li>
 * </ul>
 * </p>
 * 
 * <p>注意：此方案为轻量级实现，复杂格式（图片、表格样式等）可能无法完美保留</p>
 */
@Slf4j
@Service
public class DocumentConversionServiceImpl implements DocumentConversionService {

    /** 需要转换的Office格式 */
    private static final Set<String> CONVERTIBLE_FORMATS = Set.of(
            "doc", "docx",      // Word
            "xls", "xlsx",      // Excel
            "txt"               // 文本
    );

    /** 长期保存格式（无需转换） */
    private static final Set<String> LONG_TERM_FORMATS = Set.of(
            "pdf",              // PDF
            "ofd",              // OFD (国产版式文档)
            "tif", "tiff"       // TIFF
    );

    public DocumentConversionServiceImpl() {
        log.info("文档转换服务初始化成功 (Apache POI + OpenPDF 方案)");
    }

    @Override
    public boolean needsConversion(String extension) {
        if (extension == null) {
            return false;
        }
        return CONVERTIBLE_FORMATS.contains(extension.toLowerCase());
    }

    @Override
    public boolean isLongTermFormat(String extension) {
        if (extension == null) {
            return false;
        }
        return LONG_TERM_FORMATS.contains(extension.toLowerCase());
    }

    @Override
    public byte[] convertToPdf(InputStream inputStream, String sourceExtension) {
        if (!needsConversion(sourceExtension)) {
            log.debug("文件无需转换: extension={}", sourceExtension);
            return null;
        }

        try {
            // 先读取到字节数组，因为某些操作需要多次读取
            byte[] inputData = inputStream.readAllBytes();
            
            byte[] pdfData = switch (sourceExtension.toLowerCase()) {
                case "docx" -> convertDocxToPdf(new ByteArrayInputStream(inputData));
                case "doc" -> convertDocToPdf(new ByteArrayInputStream(inputData));
                case "xlsx" -> convertXlsxToPdf(new ByteArrayInputStream(inputData));
                case "xls" -> convertXlsToPdf(new ByteArrayInputStream(inputData));
                case "txt" -> convertTxtToPdf(new ByteArrayInputStream(inputData));
                default -> null;
            };

            if (pdfData != null) {
                log.info("文档转换成功: {} -> PDF, 大小: {} bytes", sourceExtension, pdfData.length);
            }
            return pdfData;

        } catch (Exception e) {
            log.error("文档转换失败: extension={}, error={}", sourceExtension, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public byte[] convertToPdf(byte[] sourceData, String sourceExtension) {
        return convertToPdf(new ByteArrayInputStream(sourceData), sourceExtension);
    }

    @Override
    public boolean isServiceAvailable() {
        // 纯 Java 方案始终可用
        return true;
    }

    /**
     * 转换 DOCX (Word 2007+) 到 PDF
     */
    private byte[] convertDocxToPdf(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Document pdfDoc = new Document(PageSize.A4);
            PdfWriter.getInstance(pdfDoc, outputStream);
            pdfDoc.open();

            // 设置中文字体
            BaseFont baseFont = getChineseBaseFont();
            Font font = new Font(baseFont, 12);
            Font headingFont = new Font(baseFont, 14, Font.BOLD);

            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        // 简单处理标题样式
                        Font useFont = paragraph.getStyle() != null && 
                                      paragraph.getStyle().toLowerCase().contains("heading") 
                                      ? headingFont : font;
                        Paragraph pdfPara = new Paragraph(text, useFont);
                        pdfDoc.add(pdfPara);
                    }
                } else if (element instanceof XWPFTable table) {
                    // 转换表格
                    pdfDoc.add(convertXWPFTable(table, font));
                }
            }

            pdfDoc.close();
            return outputStream.toByteArray();
        }
    }

    /**
     * 转换 DOC (Word 97-2003) 到 PDF
     */
    private byte[] convertDocToPdf(InputStream inputStream) throws Exception {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            WordExtractor extractor = new WordExtractor(document);
            String text = extractor.getText();
            extractor.close();

            Document pdfDoc = new Document(PageSize.A4);
            PdfWriter.getInstance(pdfDoc, outputStream);
            pdfDoc.open();

            BaseFont baseFont = getChineseBaseFont();
            Font font = new Font(baseFont, 12);

            // 按段落分割
            String[] paragraphs = text.split("\n");
            for (String para : paragraphs) {
                if (para != null && !para.trim().isEmpty()) {
                    pdfDoc.add(new Paragraph(para.trim(), font));
                }
            }

            pdfDoc.close();
            return outputStream.toByteArray();
        }
    }

    /**
     * 转换 XLSX (Excel 2007+) 到 PDF
     */
    private byte[] convertXlsxToPdf(InputStream inputStream) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            return convertWorkbookToPdf(workbook);
        }
    }

    /**
     * 转换 XLS (Excel 97-2003) 到 PDF
     */
    private byte[] convertXlsToPdf(InputStream inputStream) throws Exception {
        try (Workbook workbook = new HSSFWorkbook(inputStream)) {
            return convertWorkbookToPdf(workbook);
        }
    }

    /**
     * 通用 Workbook 转 PDF
     */
    private byte[] convertWorkbookToPdf(Workbook workbook) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Excel 横向打印更合适
            Document pdfDoc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(pdfDoc, outputStream);
            pdfDoc.open();

            BaseFont baseFont = getChineseBaseFont();
            Font font = new Font(baseFont, 10);
            Font headerFont = new Font(baseFont, 10, Font.BOLD);

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                
                // 添加工作表名称
                if (sheetIndex > 0) {
                    pdfDoc.newPage();
                }
                pdfDoc.add(new Paragraph("工作表: " + sheet.getSheetName(), headerFont));
                pdfDoc.add(Chunk.NEWLINE);

                // 确定列数
                int maxCols = 0;
                for (Row row : sheet) {
                    if (row.getLastCellNum() > maxCols) {
                        maxCols = row.getLastCellNum();
                    }
                }

                if (maxCols > 0) {
                    // 创建 PDF 表格
                    com.lowagie.text.Table pdfTable = new com.lowagie.text.Table(maxCols);
                    pdfTable.setWidth(100);
                    pdfTable.setPadding(3);
                    pdfTable.setSpacing(0);

                    boolean isFirstRow = true;
                    for (Row row : sheet) {
                        for (int colIndex = 0; colIndex < maxCols; colIndex++) {
                            Cell cell = row.getCell(colIndex);
                            String cellValue = getCellValueAsString(cell);
                            
                            com.lowagie.text.Cell pdfCell = new com.lowagie.text.Cell(
                                new Phrase(cellValue, isFirstRow ? headerFont : font)
                            );
                            if (isFirstRow) {
                                pdfCell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                            }
                            pdfTable.addCell(pdfCell);
                        }
                        isFirstRow = false;
                    }
                    pdfDoc.add(pdfTable);
                }
            }

            pdfDoc.close();
            return outputStream.toByteArray();
        }
    }

    /**
     * 转换 TXT 到 PDF
     */
    private byte[] convertTxtToPdf(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Document pdfDoc = new Document(PageSize.A4);
            PdfWriter.getInstance(pdfDoc, outputStream);
            pdfDoc.open();

            BaseFont baseFont = getChineseBaseFont();
            Font font = new Font(baseFont, 12);

            String line;
            while ((line = reader.readLine()) != null) {
                pdfDoc.add(new Paragraph(line, font));
            }

            pdfDoc.close();
            return outputStream.toByteArray();
        }
    }

    /**
     * 获取中文字体
     */
    private BaseFont getChineseBaseFont() {
        try {
            // 尝试使用系统中文字体
            String[] fontPaths = {
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",  // Linux WenQuanYi
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                "/usr/share/fonts/chinese/TrueType/SimSun.ttf",
                "C:/Windows/Fonts/simsun.ttc",                      // Windows 宋体
                "C:/Windows/Fonts/msyh.ttc",                        // Windows 微软雅黑
                "/System/Library/Fonts/PingFang.ttc",               // macOS
                "/System/Library/Fonts/STHeiti Light.ttc"
            };

            for (String fontPath : fontPaths) {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    return BaseFont.createFont(fontPath + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            }

            // 回退到内置字体
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        } catch (Exception e) {
            log.warn("加载中文字体失败，使用默认字体: {}", e.getMessage());
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            } catch (Exception ex) {
                throw new RuntimeException("创建字体失败", ex);
            }
        }
    }

    /**
     * 转换 XWPF 表格为 PDF 表格
     */
    private com.lowagie.text.Table convertXWPFTable(XWPFTable table, Font font) throws Exception {
        int numRows = table.getNumberOfRows();
        if (numRows == 0) {
            return null;
        }
        
        int numCols = table.getRow(0).getTableCells().size();
        com.lowagie.text.Table pdfTable = new com.lowagie.text.Table(numCols);
        pdfTable.setWidth(100);
        pdfTable.setPadding(3);

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = cell.getText();
                com.lowagie.text.Cell pdfCell = new com.lowagie.text.Cell(new Phrase(cellText, font));
                pdfTable.addCell(pdfCell);
            }
        }

        return pdfTable;
    }

    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        yield String.valueOf((long) value);
                    }
                    yield String.valueOf(value);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> "";
        };
    }
}
