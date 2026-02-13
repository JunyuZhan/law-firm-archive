package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 创建培训计划命令. */
@Data
public class CreateTrainingCommand {

  /** 培训标题. */
  @NotBlank(message = "培训标题不能为空")
  private String title;

  /** 培训类型. */
  @NotBlank(message = "培训类型不能为空")
  private String trainingType;

  /** 培训类别. */
  private String category;

  /** 培训描述. */
  private String description;

  /** 培训讲师. */
  private String trainer;

  /** 培训地点. */
  private String location;

  /** 开始时间. */
  @NotNull(message = "开始时间不能为空")
  private LocalDateTime startTime;

  /** 结束时间. */
  @NotNull(message = "结束时间不能为空")
  private LocalDateTime endTime;

  /** 时长(分钟). */
  private Integer duration;

  /** 学分. */
  private Integer credits = 0;

  /** 最大参与人数. */
  private Integer maxParticipants;

  /** 报名截止日期. */
  private LocalDate enrollDeadline;

  /** 资料链接. */
  private String materialsUrl;

  /** 备注. */
  private String remarks;
}
