package com.lawfirm.application.admin.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 出函模板DTO
 */
@Data
public class LetterTemplateDTO {
    private Long id;
    private String templateNo;
    private String name;
    private String letterType;
    private String letterTypeName;
    private String content;
    private String description;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
