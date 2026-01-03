package com.lawfirm.application.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户标签DTO
 */
@Data
public class ClientTagDTO {

    private Long id;
    private String tagName;
    private String tagColor;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

