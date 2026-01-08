package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentDTO extends BaseDTO {

    private Long id;
    private String docNo;
    private String title;
    
    private Long categoryId;
    private String categoryName;
    
    private Long matterId;
    private String matterName;
    
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileSizeDisplay;
    private String fileType;
    private String mimeType;
    
    private Integer version;
    private Boolean isLatest;
    private Long parentDocId;
    
    private String securityLevel;
    private String securityLevelName;
    
    private String stage;
    private List<String> tags;
    private String description;
    
    private String status;
    private String statusName;
    
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /** 卷宗目录项ID */
    private Long dossierItemId;
    /** 文件夹路径 */
    private String folderPath;
    /** 显示排序顺序 */
    private Integer displayOrder;
}
