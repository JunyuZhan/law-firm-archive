package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建培训计划命令
 */
@Data
public class CreateTrainingCommand {

    @NotBlank(message = "培训标题不能为空")
    private String title;

    @NotBlank(message = "培训类型不能为空")
    private String trainingType;

    private String category;

    private String description;

    private String trainer;

    private String location;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private Integer duration;

    private Integer credits = 0;

    private Integer maxParticipants;

    private LocalDate enrollDeadline;

    private String materialsUrl;

    private String remarks;
}
