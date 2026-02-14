package com.archivesystem.service.impl;

import com.archivesystem.service.DocumentConversionService;
import lombok.extern.slf4j.Slf4j;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * 文档转换服务实现.
 * 使用 JODConverter + LibreOffice 将 Office 文档转换为 PDF
 * 
 * <p>支持转换的格式：
 * <ul>
 *   <li>Word: doc, docx</li>
 *   <li>Excel: xls, xlsx</li>
 *   <li>PowerPoint: ppt, pptx</li>
 *   <li>文本: txt, rtf</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class DocumentConversionServiceImpl implements DocumentConversionService {

    /** 需要转换的Office格式 */
    private static final Set<String> CONVERTIBLE_FORMATS = Set.of(
            "doc", "docx",      // Word
            "xls", "xlsx",      // Excel
            "ppt", "pptx",      // PowerPoint
            "txt", "rtf",       // 文本
            "odt", "ods", "odp" // OpenDocument
    );

    /** 长期保存格式（无需转换） */
    private static final Set<String> LONG_TERM_FORMATS = Set.of(
            "pdf",              // PDF
            "ofd",              // OFD (国产版式文档)
            "tif", "tiff"       // TIFF
    );

    private final OfficeManager officeManager;
    private final DocumentConverter documentConverter;

    @Autowired(required = false)
    public DocumentConversionServiceImpl(OfficeManager officeManager, 
                                         DocumentConverter documentConverter) {
        this.officeManager = officeManager;
        this.documentConverter = documentConverter;
        
        if (officeManager != null && documentConverter != null) {
            log.info("文档转换服务初始化成功，LibreOffice 连接正常");
        } else {
            log.warn("文档转换服务初始化失败，LibreOffice 未配置或未启动");
        }
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
        if (!isServiceAvailable()) {
            log.warn("文档转换服务不可用，跳过转换: extension={}", sourceExtension);
            return null;
        }

        if (!needsConversion(sourceExtension)) {
            log.debug("文件无需转换: extension={}", sourceExtension);
            return null;
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            documentConverter
                    .convert(inputStream)
                    .as(getDocumentFormat(sourceExtension))
                    .to(outputStream)
                    .as(DefaultDocumentFormatRegistry.PDF)
                    .execute();

            byte[] pdfData = outputStream.toByteArray();
            log.info("文档转换成功: {} -> PDF, 大小: {} bytes", sourceExtension, pdfData.length);
            return pdfData;

        } catch (OfficeException e) {
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
        return officeManager != null && 
               documentConverter != null && 
               officeManager.isRunning();
    }

    /**
     * 根据扩展名获取文档格式.
     */
    private org.jodconverter.core.document.DocumentFormat getDocumentFormat(String extension) {
        return switch (extension.toLowerCase()) {
            case "doc" -> DefaultDocumentFormatRegistry.DOC;
            case "docx" -> DefaultDocumentFormatRegistry.DOCX;
            case "xls" -> DefaultDocumentFormatRegistry.XLS;
            case "xlsx" -> DefaultDocumentFormatRegistry.XLSX;
            case "ppt" -> DefaultDocumentFormatRegistry.PPT;
            case "pptx" -> DefaultDocumentFormatRegistry.PPTX;
            case "txt" -> DefaultDocumentFormatRegistry.TXT;
            case "rtf" -> DefaultDocumentFormatRegistry.RTF;
            case "odt" -> DefaultDocumentFormatRegistry.ODT;
            case "ods" -> DefaultDocumentFormatRegistry.ODS;
            case "odp" -> DefaultDocumentFormatRegistry.ODP;
            default -> null;
        };
    }
}
