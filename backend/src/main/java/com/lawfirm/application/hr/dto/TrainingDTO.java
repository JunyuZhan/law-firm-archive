package com.lawfirm.application.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 培训计划DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDTO {

    private Long id;
    private String title;
    private String trainingType;
    private String category;
    private String description;
    private String trainer;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Integer credits;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private LocalDate enrollDeadline;
    private String status;
    private String materialsUrl;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 是否可报名
     */
    private Boolean canEnroll;

    /**
     * 当前用户是否已报名
     */
    private Boolean enrolled;
}
