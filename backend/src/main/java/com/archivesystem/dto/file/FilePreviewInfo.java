package com.archivesystem.dto.file;

import lombok.Builder;
import lombok.Data;

/**
 * 文件预览信息DTO.
 * @author junyuzhan
 */
@Data
@Builder
public class FilePreviewInfo {
    /** 预览URL */
    private String url;
    
    /** 预览类型: pdf, image, video, audio, unsupported */
    private String previewType;
    
    /** 是否为转换后的文件 */
    private Boolean isConverted;
    
    /** 原始文件扩展名 */
    private String originalExtension;
}
