package com.lawfirm.application.hr.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 职级通道 DTO
 */
@Data
public class CareerLevelDTO {
    private Long id;
    private String levelCode;
    private String levelName;
    private Integer levelOrder;
    private String category;
    private String categoryName;
    private String description;
    
    private Integer minWorkYears;
    private Integer minMatterCount;
    private BigDecimal minRevenue;
    private List<String> requiredCertificates;
    private String otherRequirements;
    
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryRange;
    
    private String status;
    private String statusName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
