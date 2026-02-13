package com.archivesystem.service;

/**
 * 水印服务接口.
 */
public interface WatermarkService {

    /**
     * 添加文本水印到图片.
     */
    byte[] addWatermarkToImage(byte[] imageData, String watermarkText);

    /**
     * 添加文本水印到PDF.
     */
    byte[] addWatermarkToPdf(byte[] pdfData, String watermarkText);

    /**
     * 获取当前用户的水印文本.
     */
    String getUserWatermarkText();
}
