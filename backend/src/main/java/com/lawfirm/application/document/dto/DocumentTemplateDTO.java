package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文档模板DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentTemplateDTO extends BaseDTO {

    private Long id;
    private String templateNo;
    private String name;
    
    private Long categoryId;
    private String categoryName;
    
    private String templateType;
    private String templateTypeName;
    
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileSizeDisplay;
    
    private List<String> variables;
    private String description;
    
    private String status;
    private String statusName;
    
    private Integer useCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
