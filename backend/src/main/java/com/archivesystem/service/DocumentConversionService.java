package com.archivesystem.service;

import java.io.InputStream;

/**
 * 文档转换服务接口.
 * 用于将Office文档（Word、Excel、PPT）转换为PDF长期保存格式
 * 
 * <p>档案管理标准要求：
 * <ul>
 *   <li>电子档案应采用版式文档（PDF/A、OFD）作为长期保存格式</li>
 *   <li>Office等可编辑格式需转换为不可编辑的版式文档</li>
 *   <li>转换后的文档保持原始内容和格式</li>
 * </ul>
 * </p>
 * @author junyuzhan
 */
public interface DocumentConversionService {

    /**
     * 判断文件是否需要转换.
     * Office文档（doc, docx, xls, xlsx, ppt, pptx）需要转换为PDF
     *
     * @param extension 文件扩展名
     * @return 是否需要转换
     */
    boolean needsConversion(String extension);

    /**
     * 判断文件是否为长期保存格式.
     * PDF、OFD、TIF、TIFF 为长期保存格式
     *
     * @param extension 文件扩展名
     * @return 是否为长期保存格式
     */
    boolean isLongTermFormat(String extension);

    /**
     * 将Office文档转换为PDF.
     *
     * @param inputStream 源文件输入流
     * @param sourceExtension 源文件扩展名
     * @return 转换后的PDF字节数组，转换失败返回null
     */
    byte[] convertToPdf(InputStream inputStream, String sourceExtension);

    /**
     * 将Office文档转换为PDF.
     *
     * @param sourceData 源文件字节数组
     * @param sourceExtension 源文件扩展名
     * @return 转换后的PDF字节数组，转换失败返回null
     */
    byte[] convertToPdf(byte[] sourceData, String sourceExtension);

    /**
     * 检查转换服务是否可用.
     * 
     * @return 服务是否可用
     */
    boolean isServiceAvailable();
}
