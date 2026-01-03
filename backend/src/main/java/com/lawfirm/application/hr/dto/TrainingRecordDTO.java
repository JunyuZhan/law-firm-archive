package com.lawfirm.application.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingRecordDTO {

    private Long id;
    private Long trainingId;
    private String trainingTitle;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime enrollTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private BigDecimal actualDuration;
    private String status;
    private BigDecimal score;
    private Boolean passed;
    private Integer earnedCredits;
    private String feedback;
    private Integer rating;
    private String certificateUrl;
    private String remarks;
    private LocalDateTime createdAt;
}
