package com.archivesystem.service;

import com.archivesystem.entity.DigitalFile;

/**
 * 文件预览服务接口.
 */
public interface PreviewService {

    /**
     * 生成缩略图.
     */
    String generateThumbnail(Long fileId);

    /**
     * 获取预览URL.
     */
    String getPreviewUrl(Long fileId, String accessIp);

    /**
     * 判断文件是否支持预览.
     */
    boolean isPreviewable(DigitalFile file);

    /**
     * 判断文件是否支持直接预览（无需转换）.
     */
    boolean isDirectPreviewable(String fileExtension);
}
