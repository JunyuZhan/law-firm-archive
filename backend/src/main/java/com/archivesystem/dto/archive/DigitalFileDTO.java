package com.archivesystem.dto.archive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 电子文件DTO.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalFileDTO {

    private Long id;
    private Long archiveId;
    private String fileNo;
    
    // 文件信息
    private String fileName;
    private String originalName;
    private String fileExtension;
    private String mimeType;
    private Long fileSize;
    private String fileSizeFormatted;
    
    // 格式信息
    private String formatName;
    private Boolean isLongTermFormat;
    
    // 完整性校验
    private String hashAlgorithm;
    private String hashValue;
    
    // 预览信息
    private Boolean hasPreview;
    private String previewUrl;
    private String thumbnailUrl;
    private String downloadUrl;
    
    // 分类信息
    private String fileCategory;
    private String fileCategoryName;
    private Integer sortOrder;
    private String description;
    private Integer volumeNo;
    private String sectionType;
    private String documentNo;
    private Integer pageStart;
    private Integer pageEnd;
    private String versionLabel;
    private String fileSourceType;
    private String scanBatchNo;
    private String scanOperator;
    private LocalDateTime scanTime;
    private String scanCheckStatus;
    private String scanCheckBy;
    private LocalDateTime scanCheckTime;
    
    // OCR信息
    private String ocrStatus;
    
    // 系统字段
    private LocalDateTime uploadAt;
    private String uploadByName;
    private LocalDateTime createdAt;
}
